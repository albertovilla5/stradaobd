package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class TorqueCsvImportResult(
    val pids: List<ObdPid>,
    val initCommands: List<String>,
    val skippedLinesCount: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Parser para archivos CSV de Torque Pro.
 * Compatible con encabezados: "Name", "ShortName", "ModeAndPID", "Equation", "Min Value", "Max Value", "Units", "Header"
 * y con directivas especiales en comentarios como: "; init string: AT SP7\nAT SH DA10F1"
 */
object TorqueCsvParser {

    fun parse(inputStream: InputStream): TorqueCsvImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val pids = mutableListOf<ObdPid>()
        val initCommands = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var skipped = 0

        var line: String?
        var isFirstDataRow = true

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            // Detección de comentarios y comandos de inicialización
            if (trimmed.startsWith(";") || trimmed.startsWith("#")) {
                val commentContent = trimmed.substring(1).trim()
                if (commentContent.startsWith("init string:", ignoreCase = true)) {
                    val rawInit = commentContent.substring("init string:".length).trim()
                    // Descomponer comandos separados por \n o espacios dobles o comas
                    val cmds = rawInit.split(Regex("""\\n|\n|;"""))
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    initCommands.addAll(cmds)
                }
                continue
            }

            // Ignorar cabecera si contiene "Name" y "ModeAndPID"
            if (isFirstDataRow && (trimmed.contains("ModeAndPID", ignoreCase = true) || trimmed.contains("ShortName", ignoreCase = true))) {
                isFirstDataRow = false
                continue
            }
            isFirstDataRow = false

            // Parsear fila de valores CSV
            try {
                val tokens = parseCsvLine(trimmed)
                if (tokens.size >= 4) {
                    val name = tokens.getOrElse(0) { "Sensor Desconocido" }
                    val shortName = tokens.getOrElse(1) { name.take(10) }
                    val modeAndPid = tokens.getOrElse(2) { "" }
                    val equation = tokens.getOrElse(3) { "" }
                    val minVal = tokens.getOrElse(4) { "0" }.toFloatOrNull() ?: 0f
                    val maxVal = tokens.getOrElse(5) { "100" }.toFloatOrNull() ?: 100f
                    val units = tokens.getOrElse(6) { "" }
                    val header = tokens.getOrElse(7) { "" }

                    if (modeAndPid.isNotBlank()) {
                        val category = categorizePid(name, shortName, modeAndPid)
                        pids.add(
                            ObdPid(
                                name = name,
                                shortName = shortName,
                                modeAndPid = modeAndPid,
                                equation = equation,
                                minValue = minVal,
                                maxValue = maxVal,
                                units = units,
                                header = header,
                                isCustom = true,
                                isEnabled = true,
                                category = category
                            )
                        )
                    } else {
                        skipped++
                    }
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                errors.add("Error en línea '$trimmed': ${e.message}")
                skipped++
            }
        }

        return TorqueCsvImportResult(
            pids = pids,
            initCommands = initCommands,
            skippedLinesCount = skipped,
            errors = errors
        )
    }

    fun parseString(csvContent: String): TorqueCsvImportResult {
        return parse(csvContent.byteInputStream())
    }

    /**
     * Desglosa una línea CSV respetando texto entre comillas dobles.
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Comillas escapadas ("")
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())

        // Limpiar comillas exteriores de los tokens
        return tokens.map { token ->
            var t = token.trim()
            if (t.startsWith("\"") && t.endsWith("\"") && t.length >= 2) {
                t = t.substring(1, t.length - 1)
            }
            t.trim()
        }
    }

    private fun categorizePid(name: String, shortName: String, modeAndPid: String): PidCategory {
        val s = (name + " " + shortName).lowercase()
        return when {
            s.contains("dpf") || s.contains("egt") || s.contains("exhaust") || s.contains("regen") || s.contains("pm") ->
                PidCategory.EXHAUST_DPF
            s.contains("oil") || s.contains("fuel") || s.contains("consum") || s.contains("gas") ->
                PidCategory.FUEL
            s.contains("trans") || s.contains("gear") || s.contains("clutch") ->
                PidCategory.TRANSMISSION
            s.contains("volt") || s.contains("battery") || s.contains("alt") ->
                PidCategory.ELECTRICAL
            s.contains("rpm") || s.contains("speed") || s.contains("coolant") || s.contains("throttle") || s.contains("temp") ->
                PidCategory.ENGINE
            else -> PidCategory.CUSTOM
        }
    }
}
