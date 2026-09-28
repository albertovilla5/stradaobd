package com.torqueobd2.dash.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.torqueobd2.dash.data.model.ConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.util.UUID

/**
 * Conexión Bluetooth Clásica SPP (Serial Port Profile) de grado profesional para adaptadores ELM327 físicos.
 * Incluye reintento automático por reflexión (canal RFCOMM 1) para compatibilidad con el 100% de clones chinos v1.5/v2.1,
 * lectura con timeout parametrizable y diagnóstico detallado de errores.
 */
class BluetoothObdConnection(
    private val deviceAddress: String,
    private val customInitCommands: List<String> = emptyList(),
    private val onLog: ((String) -> Unit)? = null
) : ObdConnection {

    companion object {
        // UUID estándar SPP (Serial Port Profile) RFCOMM
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Comandos AT esenciales recomendados para inicializar el chip ELM327
        private val DEFAULT_AT_INIT_COMMANDS = listOf(
            "AT Z",   // Reset general
            "AT E0",  // Desactivar eco de comandos
            "AT L0",  // Desactivar saltos de línea adicionales
            "AT S0",  // Desactivar espacios para mayor velocidad de bus
            "AT AT1", // Habilitar Adaptive Timing (ajuste automático a la velocidad de la ECU)
            "AT SP0"  // Búsqueda automática de protocolo de comunicación
        )
    }

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _statusMessage = MutableStateFlow("Desconectado")
    override val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private fun log(message: String) {
        onLog?.invoke(message)
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            val msg = "Hardware Bluetooth no disponible en este dispositivo."
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = msg
            log("❌ $msg")
            return@withContext false
        }

        if (!adapter.isEnabled) {
            val msg = "Bluetooth desactivado. Por favor, activa el Bluetooth en los ajustes del teléfono."
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = msg
            log("❌ $msg")
            return@withContext false
        }

        try {
            _status.value = ConnectionStatus.CONNECTING
            _statusMessage.value = "Conectando a $deviceAddress..."
            log("🔌 Buscando dispositivo emparejado $deviceAddress...")

            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(deviceAddress)
            } catch (e: IllegalArgumentException) {
                val err = "Dirección MAC Bluetooth inválida ($deviceAddress)"
                _status.value = ConnectionStatus.ERROR
                _statusMessage.value = err
                log("❌ $err")
                return@withContext false
            }

            val deviceName = try { device.name ?: deviceAddress } catch (_: Exception) { deviceAddress }
            log("Dispositivo seleccionado: $deviceName ($deviceAddress)")

            // Cancelar descubrimiento Bluetooth para no ralentizar el handshake de conexión
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                    log("Búsqueda de dispositivos cancelada para liberar ancho de banda.")
                }
            } catch (_: Exception) {}

            // Intento 1: Conexión mediante UUID SPP estándar
            log("Intentando enlace RFCOMM con UUID SPP estándar...")
            var connectedSocket: BluetoothSocket? = null
            var lastException: Exception? = null

            try {
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                connectedSocket = s
                log("✅ Conexión establecida mediante UUID SPP estándar.")
            } catch (e: IOException) {
                lastException = e
                log("⚠️ Falló conexión por UUID estándar: ${e.message}. Probando método fallback (Canal RFCOMM 1)...")
            }

            // Intento 2: Fallback por reflexión al canal RFCOMM 1 (esencial para adaptadores ELM327 clon)
            if (connectedSocket == null) {
                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    val fallbackSocket = method.invoke(device, 1) as BluetoothSocket
                    fallbackSocket.connect()
                    connectedSocket = fallbackSocket
                    log("✅ Conexión establecida con éxito mediante canal alternativo RFCOMM 1.")
                } catch (e: Exception) {
                    lastException = e
                    log("❌ Falló también el método fallback: ${e.message}")
                }
            }

            if (connectedSocket == null || !connectedSocket.isConnected) {
                disconnect()
                val detailedErr = "No se pudo conectar a $deviceName. Verifica que el adaptador ELM327 esté conectado al coche, con el LED de alimentación encendido y la llave en contacto."
                _status.value = ConnectionStatus.ERROR
                _statusMessage.value = detailedErr
                log("❌ $detailedErr")
                return@withContext false
            }

            socket = connectedSocket
            inputStream = connectedSocket.inputStream
            outputStream = connectedSocket.outputStream

            _status.value = ConnectionStatus.INITIALIZING_ELM
            _statusMessage.value = "Inicializando chip ELM327..."
            log("⚙️ Inicializando chip ELM327...")

            // Pequeña pausa de 300ms para estabilizar el microcontrolador ELM327 tras la apertura del socket
            Thread.sleep(300)

            // Secuencia de comandos AT
            val commandsToRun = mutableListOf<String>()
            commandsToRun.addAll(DEFAULT_AT_INIT_COMMANDS)
            // Añadir comandos de inicialización personalizados (ej: los de Honda i-DTEC si se cargó un CSV)
            for (customCmd in customInitCommands) {
                if (customCmd.isNotBlank() && !commandsToRun.contains(customCmd.trim())) {
                    commandsToRun.add(customCmd.trim())
                }
            }

            for (cmd in commandsToRun) {
                log("  > $cmd")
                // AT Z y AT SP0 requieren mayor tiempo de espera
                val timeout = if (cmd.startsWith("AT Z", ignoreCase = true) || cmd.startsWith("AT SP", ignoreCase = true)) 5000L else 2500L
                val response = sendCommandInternal(cmd, timeout)
                val cleanResp = response.trim()
                log("  < $cleanResp")

                // Si hay diagnóstico de error del adaptador o ECU, notificarlo
                val diag = Elm327Protocol.diagnoseResponseError(response)
                if (diag != null) {
                    log("  ℹ️ Diagnóstico ELM: $diag")
                }
            }

            // Comprobación inicial de la centralita con comando PID 0100 (PIDs soportados 01-20)
            log("  > 0100 (Prueba de enlace con la ECU del coche)")
            val ecuTest = sendCommandInternal("0100", 5000L)
            log("  < ${ecuTest.trim()}")

            val ecuDiag = Elm327Protocol.diagnoseResponseError(ecuTest)
            if (ecuDiag != null) {
                log("  ⚠️ $ecuDiag")
            } else if (ecuTest.contains("41 00") || ecuTest.contains("4100")) {
                log("  ✅ ECU respondió satisfactoriamente (Protocolo establecido).")
            }

            _status.value = ConnectionStatus.CONNECTED
            _statusMessage.value = "Conectado a $deviceName"
            log("🚀 Conexión completa y activa con $deviceName.")
            true
        } catch (e: Exception) {
            disconnect()
            val errMsg = "Error inesperado al conectar: ${e.localizedMessage ?: e.javaClass.simpleName}"
            _status.value = ConnectionStatus.ERROR
            _statusMessage.value = errMsg
            log("❌ $errMsg")
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        log("Cerrando conexión Bluetooth...")
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}

        inputStream = null
        outputStream = null
        socket = null
        _status.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Desconectado"
        log("Conexión finalizada.")
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && _status.value == ConnectionStatus.CONNECTED
    }

    override suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext "ERROR: NOT CONNECTED"
        return@withContext sendCommandInternal(command, 2500L)
    }

    private fun sendCommandInternal(command: String, timeoutMs: Long = 2500L): String {
        val out = outputStream ?: return "ERROR: NO OUTPUT STREAM"
        val inStream = inputStream ?: return "ERROR: NO INPUT STREAM"

        try {
            // Limpiar cualquier residuo previo en el stream de entrada
            while (inStream.available() > 0) {
                inStream.read()
            }

            val formattedCmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(formattedCmd)
            out.flush()

            val sb = StringBuilder()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val avail = inStream.available()
                if (avail > 0) {
                    val buffer = ByteArray(avail)
                    val len = inStream.read(buffer)
                    if (len > 0) {
                        for (i in 0 until len) {
                            val c = buffer[i].toInt().toChar()
                            // Ignorar caracteres nulos
                            if (c != '\u0000') {
                                sb.append(c)
                            }
                        }
                        // El carácter '>' es el prompt estándar de fin de comando en todos los adaptadores ELM327
                        if (sb.contains(">")) {
                            break
                        }
                    }
                } else {
                    Thread.sleep(15)
                }
            }

            val result = sb.toString()
            if (result.isEmpty()) {
                return "TIMEOUT: Sin respuesta del adaptador"
            }
            return result
        } catch (e: Exception) {
            return "ERROR I/O: ${e.message}"
        }
    }
}
