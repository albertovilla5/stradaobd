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
        val clean = cleanResponse(rawResponse)

        if (clean.contains("NO DATA", ignoreCase = true) ||
            clean.contains("ERROR", ignoreCase = true) ||
            clean.contains("UNABLE", ignoreCase = true) ||
            clean.contains("BUS INIT", ignoreCase = true)
        ) {
            return IntArray(0)
        }

        // Eliminar prefijos de multilínea CAN tipo "0: 62 25 12 ... 1: ... 2: ..."
        val sanitized = clean.replace(Regex("""\b[0-9A-Fa-f]:"""), " ")
            .replace(" ", "")

        val cmdClean = command.replace(" ", "").uppercase()

        // Determinar prefijo esperado de respuesta positiva (ej: 010C -> 410C, 222512 -> 622512)
        val expectedPrefix = when {
            cmdClean.startsWith("01") -> "41" + cmdClean.substring(2)
            cmdClean.startsWith("02") -> "42" + cmdClean.substring(2)
            cmdClean.startsWith("22") -> "62" + cmdClean.substring(2)
            else -> ""
        }

        var dataHex = sanitized
        if (expectedPrefix.isNotEmpty()) {
            val idx = sanitized.indexOf(expectedPrefix)
            if (idx >= 0) {
                dataHex = sanitized.substring(idx + expectedPrefix.length)
            }
        }

        // Convertir cada par de caracteres hexadecimales en un entero 0..255
        val bytesList = mutableListOf<Int>()
        var i = 0
        while (i + 1 < dataHex.length) {
            val byteStr = dataHex.substring(i, i + 2)
            val byteVal = byteStr.toIntOrNull(16)
            if (byteVal != null) {
                bytesList.add(byteVal)
            } else {
                break
            }
            i += 2
        }

        return bytesList.toIntArray()
    }
}
