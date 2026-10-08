package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Conexión TCP / WiFi para adaptadores ELM327 WiFi (típicamente 192.168.0.10:35000).
 */
class WifiObdConnection(
    private val host: String = "192.168.0.10",
    private val port: Int = 35000,
    private val initCommands: List<String> = emptyList(),
    private val onLog: ((String) -> Unit)? = null
) : ObdConnection {

    private val commandMutex = Mutex()
    private val defaultInitCommands = listOf("AT Z", "AT E0", "AT L0", "AT S0", "AT AT1", "AT SP0")

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("Desconectado")
    override val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            require(host.isNotBlank()) { "Indica la dirección IP del adaptador" }
            require(port in 1..65535) { "El puerto debe estar entre 1 y 65535" }
            _status.value = ConnectionStatus.CONNECTING
            _statusMessage.value = "Conectando a $host:$port..."

            val sock = Socket()
            socket = sock
            sock.tcpNoDelay = true
            sock.connect(InetSocketAddress(host, port), 4000)
            inputStream = sock.getInputStream()
            outputStream = sock.getOutputStream()

            _status.value = ConnectionStatus.INITIALIZING_ELM
            _statusMessage.value = "Configurando ELM327..."

            val commands = (defaultInitCommands + initCommands.map { it.trim() }.filter { it.isNotEmpty() }).distinct()
            for (cmd in commands) {
                onLog?.invoke("WiFi > $cmd")
                val response = sendCommandInternal(cmd, 5000L)
                onLog?.invoke("WiFi < ${response.trim()}")
                if (response.contains('?') || response.contains("ERROR", ignoreCase = true)) {
                    throw IOException("El adaptador rechazó el comando $cmd: ${response.trim()}")
                }
            }

            _status.value = ConnectionStatus.CONNECTED
            _statusMessage.value = "Conectado vía WiFi a $host:$port"
            true
        } catch (e: CancellationException) {
            closeSocket()
            _status.value = ConnectionStatus.DISCONNECTED
            throw e
        } catch (e: Exception) {
            disconnect()
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = "Error al conectar WiFi: ${e.localizedMessage}"
            onLog?.invoke("❌ ${_statusMessage.value}")
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        closeSocket()
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Desconectado"
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false && _status.value == ConnectionStatus.CONNECTED
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        commandMutex.withLock {
            if (!isConnected()) return@withLock "ERROR: NOT CONNECTED"
            try {
                sendCommandInternal(command)
            } catch (e: IOException) {
                closeSocket()
                _status.value = ConnectionStatus.DISCONNECTED
                _statusMessage.value = e.message ?: "Enlace WiFi perdido"
                throw e
            }
        }
    }

    private suspend fun sendCommandInternal(command: String, timeoutMs: Long = 2500L): String {
        val out = outputStream ?: throw IOException("No hay salida WiFi")
        val inStream = inputStream ?: throw IOException("No hay entrada WiFi")

        while (inStream.available() > 0) {
            inStream.read()
        }

        val formattedCmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
        out.write(formattedCmd)
        out.flush()

        val sb = StringBuilder()
        val buffer = ByteArray(256)
        val startTime = System.nanoTime()

        while ((System.nanoTime() - startTime) / 1_000_000 < timeoutMs) {
            if (inStream.available() > 0) {
                val len = inStream.read(buffer)
                if (len == -1) {
                    _status.value = ConnectionStatus.DISCONNECTED
                    throw java.io.IOException("Socket WiFi cerrado por el dispositivo (EOF)")
                }
                if (len > 0) {
                    val chunk = String(buffer, 0, len, Charsets.US_ASCII)
                    sb.append(chunk)
                    if (chunk.contains(">")) {
                        return sb.toString()
                    }
                }
            } else {
                if (socket?.isConnected != true || socket?.isClosed == true) {
                    _status.value = ConnectionStatus.DISCONNECTED
                    throw java.io.IOException("Socket WiFi desconectado")
                }
                delay(2)
            }
        }

        throw IOException("Sin respuesta completa del adaptador a $command en ${timeoutMs}ms")
    }
}
