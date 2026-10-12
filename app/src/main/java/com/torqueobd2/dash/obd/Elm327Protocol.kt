package com.torqueobd2.dash.obd

import com.torqueobd2.dash.i18n.tr

/**
 * Utilidades para el protocolo ELM327, tramas OBD-II y diagnóstico de respuestas.
 */
object Elm327Protocol {

    /**
     * Limpia la respuesta de un ELM327, eliminando ecos, retornos de carro,
     * espacios, mensajes transitorios como 'SEARCHING...' y el prompt final '>'.
     */
    fun cleanResponse(raw: String): String {
        return raw
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("SEARCHING...", "")
            .replace("SEARCHING", "")
            .replace(">", "")
            .trim()
    }

    /**
     * Diagnostica una respuesta del ELM327 y, si contiene un fallo o aviso,
     * devuelve una descripción amigable en español con recomendaciones de solución.
     * Retorna null si la respuesta es una lectura o comando exitoso.
     */
    fun diagnoseResponseError(raw: String): String? {
        val upper = raw.uppercase()
        return when {
            upper.contains("UNABLE TO CONNECT") ->
                tr(
                    "La centralita del vehículo no responde (UNABLE TO CONNECT). Comprueba que el contacto del coche esté en posición ON (cuadro de mandos encendido o motor en marcha) y que el adaptador esté firmemente encajado en la toma OBD2.",
                    "The vehicle ECU is not responding (UNABLE TO CONNECT). Check that the ignition is in the ON position (dashboard lit or engine running) and the adapter is firmly plugged into the OBD2 port."
                )

            upper.contains("BUS INIT: ... ERROR") || upper.contains("BUS INIT: ERROR") ->
                tr(
                    "Error en la inicialización del bus OBD del coche. Asegúrate de que el contacto esté dado y que el protocolo seleccionado sea compatible con tu vehículo.",
                    "Vehicle OBD bus initialization error. Make sure the ignition is on and the selected protocol is compatible with your vehicle."
                )

            upper.contains("CAN ERROR") ->
                tr(
                    "Error en el bus CAN del vehículo. Puede deberse a una interferencia en la línea o un pin del conector OBD flojo.",
                    "Vehicle CAN bus error. This may be caused by line interference or a loose OBD connector pin."
                )

            upper.contains("BUS BUSY") ->
                tr(
                    "El bus de datos del coche está saturado o ocupado por otra centralita. Reintentando...",
                    "The vehicle data bus is busy or saturated by another module. Retrying..."
                )

            upper.contains("FB ERROR") ->
                tr(
                    "Error de feedback (retroalimentación) en el circuito del adaptador ELM327. Es común en clones defectuosos o cuando el pin del bus tiene tensión anormal.",
                    "Feedback error in the ELM327 adapter circuit. Common on faulty clones or when bus pin voltage is abnormal."
                )

            upper.contains("DATA ERROR") || upper.contains("ERR") ->
                tr(
                    "Error de suma de comprobación (checksum) o datos corruptos recibidos desde la centralita.",
                    "Checksum error or corrupted data received from the ECU."
                )

            upper.contains("STOPPED") ->
                tr(
                    "Comando interrumpido por el usuario o por timeout del adaptador.",
                    "Command interrupted by user or adapter timeout."
                )

            upper.trim() == "?" ->
                tr(
                    "Comando no reconocido por el chip ELM327. Comprueba la sintaxis del comando enviado.",
                    "Command not recognized by the ELM327 chip. Check the syntax of the sent command."
                )

            upper.contains("BUFFER FULL") ->
                tr(
                    "El búfer interno del adaptador ELM327 se ha llenado. Se recomienda reducir la frecuencia de comandos.",
                    "The ELM327 adapter internal buffer is full. Consider reducing command polling frequency."
                )

            else -> null
        }
    }

    /**
     * Extrae los bytes de datos hex tras el encabezado de respuesta positiva.
     * En OBD2:
     * Para modo 01 (ej. "010C" -> respuesta empieza por "41 0C")
     * Para modo 22 (ej. "222512" -> respuesta empieza por "62 25 12")
     * Para multilíneas CAN (ISO 15765-4) se limpian prefijos como "0:", "1:", etc.
     */
    fun extractDataBytes(command: String, rawResponse: String): IntArray {
        if (diagnoseResponseError(rawResponse) != null ||
            rawResponse.contains("NO DATA", ignoreCase = true)) return IntArray(0)
        val cmd = command.replace(" ", "").uppercase()
        val prefix = when {
            cmd.startsWith("01") -> "41" + cmd.substring(2)
            cmd.startsWith("02") -> "42" + cmd.substring(2)
            cmd.startsWith("22") -> "62" + cmd.substring(2)
            else -> return IntArray(0)
        }
        val lines = rawResponse.replace(">", "").replace("SEARCHING...", "", ignoreCase = true)
            .replace("SEARCHING", "", ignoreCase = true).split(Regex("[\\r\\n]+"))
            .map { it.trim().uppercase() }.filter { it.isNotEmpty() && it.replace(" ", "") != cmd }
        // ATH0/CAF1: respuestas sin cabecera CAN; una respuesta por ECU.
        // Rechazar cabeceras no reconocidas evita mezclar ECU o interpretar la cabecera como datos.
        val frames = mutableListOf<String>()
        var indexed: StringBuilder? = null
        var nextIndex = 0
        for (line in lines) {
            val part = Regex("^([0-9A-F]+):\\s*(.*)$").matchEntire(line)
            if (part != null) {
                val index = part.groupValues[1].toIntOrNull(16) ?: return IntArray(0)
                if (index == 0) {
                    indexed?.let { frames.add(it.toString()) }
                    indexed = StringBuilder()
                    nextIndex = 0
                }
                if (indexed == null || index != nextIndex++) return IntArray(0)
                indexed.append(part.groupValues[2].replace(Regex("\\s"), ""))
            } else {
                indexed?.let { frames.add(it.toString()); indexed = null }
                val compact = line.replace(Regex("\\s"), "")
                // ELM añade a veces la longitud total antes de una respuesta multilínea.
                if (compact.matches(Regex("[0-9A-F]{3}"))) continue
                frames.add(compact)
            }
        }
        indexed?.let { frames.add(it.toString()) }
        val frame = frames.firstOrNull { it.startsWith(prefix) } ?: return IntArray(0)
        val data = frame.substring(prefix.length)
        if (data.isEmpty() || data.length % 2 != 0 || !data.matches(Regex("[0-9A-F]+"))) return IntArray(0)
        return data.chunked(2).map { it.toInt(16) }.toIntArray()
    }
}
