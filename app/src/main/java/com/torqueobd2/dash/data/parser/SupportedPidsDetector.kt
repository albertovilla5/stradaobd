package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.obd.Elm327Protocol

/**
 * Detector de PIDs soportados por la ECU del vehículo según el estándar SAE J1979
 * (PIDs 0100, 0120, 0140, 0160) y comprobación de respuestas para PIDs extendidos Modo 22.
 */
object SupportedPidsDetector {

    /**
     * Procesa la respuesta de un PID de soporte (como 0100, 0120, 0140, 0160)
     * y devuelve el conjunto de PIDs soportados en formato hexadecimal de 2 caracteres ("0C", "0D", etc.).
     */
    fun parseSupportedPidsRange(basePidHex: String, rawResponse: String): Set<String> {
        val supported = mutableSetOf<String>()
        val dataBytes = Elm327Protocol.extractDataBytes(basePidHex, rawResponse)
        if (dataBytes.size < 4) return supported

        val baseOffset = (basePidHex.takeLast(2).toIntOrNull(16) ?: 0) + 1

        var pidCounter = baseOffset
        for (byteVal in dataBytes.take(4)) {
            for (bit in 7 downTo 0) {
                val isSet = ((byteVal shr bit) and 1) == 1
                if (isSet) {
                    val hexStr = String.format("%02X", pidCounter)
                    supported.add(hexStr)
                }
                pidCounter++
            }
        }

        return supported
    }

    /**
     * Determina si una respuesta OBD indica que el sensor no está soportado en la ECU.
     */
    /** NO DATA/timeouts are not proof of incompatibility. Only explicit ECU rejection is. */
    fun responseFailure(command: String, rawResponse: String): com.torqueobd2.dash.data.model.SensorTelemetryStatus? {
        val clean = Elm327Protocol.cleanResponse(rawResponse).uppercase()
        val responseLines = rawResponse.split(Regex("[\\r\\n]+"))
            .map { Elm327Protocol.cleanResponse(it).uppercase().replace(" ", "") }
        val mode = command.replace(" ", "").take(2).uppercase()
        val rejection = responseLines.any { it.matches(Regex("7F" + mode + "(11|12|31)")) }
        return when {
            rejection -> com.torqueobd2.dash.data.model.SensorTelemetryStatus.INCOMPATIBLE
            clean.isEmpty() || clean.contains("NO DATA") || clean.contains("UNABLE TO CONNECT") ||
                clean.contains("STOPPED") || clean.contains("BUS BUSY") || responseLines.any { it.matches(Regex("7F" + mode + "[0-9A-F]{2}")) } ->
                com.torqueobd2.dash.data.model.SensorTelemetryStatus.NO_RESPONSE
            Elm327Protocol.diagnoseResponseError(rawResponse) != null ->
                com.torqueobd2.dash.data.model.SensorTelemetryStatus.INVALID_RESPONSE
            else -> null
        }
    }

    /**
     * Actualiza la lista de PIDs marcando cuáles están soportados por el vehículo
     * basándose en el mapa de PIDs estándar soportados y las respuestas recibidas.
     */
    fun updatePidsSupport(
        allPids: List<ObdPid>,
        supportedStandardPids: Set<String>,
        extendedResponses: Map<String, Boolean>,
        checkedStandardPids: Set<String> = (1..255).map { "%02X".format(it) }.toSet()
    ): List<ObdPid> {
        return allPids.map { pid ->
            val cmd = pid.cleanObdCommand()
            val supported = when {
                cmd.startsWith("01") && cmd.length >= 4 -> {
                    val pidHex = cmd.substring(2, 4)
                    if (pidHex in checkedStandardPids) supportedStandardPids.contains(pidHex) else pid.isSupported
                }
                cmd.startsWith("22") -> {
                    extendedResponses[cmd] ?: pid.isSupported // Si aún no se ha probado, mantener provisionalmente
                }
                else -> true
            }
            val checked = when {
                cmd.startsWith("01") -> cmd.takeLast(2) in checkedStandardPids || pid.supportChecked
                cmd.startsWith("22") -> cmd in extendedResponses || pid.supportChecked
                else -> pid.supportChecked
            }
            pid.copy(isSupported = supported, supportChecked = checked)
        }
    }
}
