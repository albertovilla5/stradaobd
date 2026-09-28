package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    private val initCommands: List<String> = listOf("AT Z", "AT E0", "AT L0", "AT S0", "AT SP0")
) : ObdConnection {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("Desconectado")
    override val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            _status.value = ConnectionStatus.CONNECTING
            _statusMessage.value = "Conectando a $host:$port..."

            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), 4000)
            socket = sock
            inputStream = sock.getInputStream()
            outputStream = sock.getOutputStream()

            _status.value = ConnectionStatus.INITIALIZING_ELM
            _statusMessage.value = "Configurando ELM327..."

            for (cmd in initCommands) {
                sendCommandInternal(cmd)
            }

            _status.value = ConnectionStatus.CONNECTED
            _statusMessage.value = "Conectado vía WiFi a $host:$port"
            true
        } catch (e: Exception) {
            disconnect()
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = "Error al conectar WiFi: ${e.localizedMessage}"
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Desconectado"
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false && _status.value == ConnectionStatus.CONNECTED
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext "ERROR: NO CONNECTED"
        return@withContext sendCommandInternal(command)
    }

    private fun sendCommandInternal(command: String): String {
        val out = outputStream ?: return "ERROR"
        val inStream = inputStream ?: return "ERROR"

        val formattedCmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
        out.write(formattedCmd)
        out.flush()

        val sb = StringBuilder()
        val buffer = ByteArray(256)
        val startTime = System.currentTimeMillis()
        val timeoutMs = 2000L

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (inStream.available() > 0) {
                val len = inStream.read(buffer)
                if (len > 0) {
                    val chunk = String(buffer, 0, len, Charsets.US_ASCII)
                    sb.append(chunk)
                    if (chunk.contains(">")) {
                        break
                    }
                }
            } else {
                Thread.sleep(10)
            }
        }

        return sb.toString()
    }
}
