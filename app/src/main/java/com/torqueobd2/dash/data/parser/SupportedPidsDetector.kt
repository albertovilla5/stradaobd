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

        val baseOffset = when (basePidHex.uppercase().takeLast(2)) {
            "00" -> 0x01
            "20" -> 0x21
            "40" -> 0x41
            "60" -> 0x61
            "80" -> 0x81
            else -> 0x01
        }

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
    fun isResponseUnsupported(rawResponse: String): Boolean {
        val clean = Elm327Protocol.cleanResponse(rawResponse).uppercase()
        return clean.contains("NO DATA") ||
                clean.contains("UNABLE TO CONNECT") ||
                clean.contains("ERROR") ||
                clean.contains("7F 22") ||
                clean.contains("STOPPED") ||
                clean.isEmpty()
    }

    /**
     * Actualiza la lista de PIDs marcando cuáles están soportados por el vehículo
     * basándose en el mapa de PIDs estándar soportados y las respuestas recibidas.
     */
    fun updatePidsSupport(
        allPids: List<ObdPid>,
        supportedStandardPids: Set<String>,
        extendedResponses: Map<String, Boolean>
    ): List<ObdPid> {
        return allPids.map { pid ->
            val cmd = pid.cleanObdCommand()
            val supported = when {
                cmd.startsWith("01") && cmd.length >= 4 -> {
                    val pidHex = cmd.substring(2, 4)
                    supportedStandardPids.contains(pidHex)
                }
                cmd.startsWith("22") -> {
                    extendedResponses[cmd] ?: true // Si aún no se ha probado, mantener provisionalmente
                }
                else -> true
            }
            pid.copy(isSupported = supported)
        }
    }
}
