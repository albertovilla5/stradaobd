package com.torqueobd2.dash.obd

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
                "La centralita del vehículo no responde (UNABLE TO CONNECT). Comprueba que el contacto del coche esté en posición ON (cuadro de mandos encendido o motor en marcha) y que el adaptador esté firmemente encajado en la toma OBD2."

            upper.contains("BUS INIT: ... ERROR") || upper.contains("BUS INIT: ERROR") ->
                "Error en la inicialización del bus OBD del coche. Asegúrate de que el contacto esté dado y que el protocolo seleccionado sea compatible con tu vehículo."

            upper.contains("CAN ERROR") ->
                "Error en el bus CAN del vehículo. Puede deberse a una interferencia en la línea o un pin del conector OBD flojo."

            upper.contains("BUS BUSY") ->
                "El bus de datos del coche está saturado o ocupado por otra centralita. Reintentando..."

            upper.contains("FB ERROR") ->
                "Error de feedback (retroalimentación) en el circuito del adaptador ELM327. Es común en clones defectuosos o cuando el pin del bus tiene tensión anormal."

            upper.contains("DATA ERROR") || upper.contains("ERR") ->
                "Error de suma de comprobación (checksum) o datos corruptos recibidos desde la centralita."

            upper.contains("STOPPED") ->
                "Comando interrumpido por el usuario o por timeout del adaptador."

            upper.trim() == "?" ->
                "Comando no reconocido por el chip ELM327. Comprueba la sintaxis del comando enviado."

            upper.contains("BUFFER FULL") ->
                "El búfer interno del adaptador ELM327 se ha llenado. Se recomienda reducir la frecuencia de comandos."

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
