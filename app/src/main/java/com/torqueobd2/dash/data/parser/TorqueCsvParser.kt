package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory
import com.torqueobd2.dash.i18n.tr
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
                    val name = tokens.getOrElse(0) { tr("Sensor Desconocido", "Unknown Sensor") }
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
                                id = java.util.UUID.nameUUIDFromBytes(listOf(modeAndPid.removePrefix("0x").uppercase(),
                                    header.replace(" ", "").uppercase(), name, shortName, equation, units)
                                    .joinToString("\u0000").toByteArray(Charsets.UTF_8)).toString(),
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
                errors.add(tr("Error en línea '$trimmed': ${e.message}", "Error on line '$trimmed': ${e.message}"))
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
            s.contains("dpf") || s.contains("fap") || s.contains("egt") || s.contains("exhaust") ||
                s.contains("escape") || s.contains("regen") || s.contains("pm") || s.contains("hollín") ||
                s.contains("hollin") || s.contains("ceniza") || s.contains("soot") || s.contains("ash") ||
                s.contains("adblue") || s.contains("def") || s.contains("scr") || s.contains("nox") ||
                s.contains("catalizador") || s.contains("catalyst") || s.contains("aditivo") ||
                s.contains("eolys") || s.contains("egr") ->
                PidCategory.EXHAUST_DPF
            s.contains("o2") || s.contains("oxígeno") || s.contains("oxigeno") || s.contains("oxygen") ||
                s.contains("lambda") || s.contains("afr") || s.contains("sonda") ->
                PidCategory.OXYGEN
            s.contains("trans") || s.contains("gear") || s.contains("marcha") || s.contains("cambio") ||
                s.contains("clutch") || s.contains("embrague") || s.contains("atf") || s.contains("dsg") ||
                s.contains("cvt") || s.contains("tft") ->
                PidCategory.TRANSMISSION
            s.contains("volt") || s.contains("tensión") || s.contains("tension") || s.contains("battery") ||
                s.contains("batería") || s.contains("bateria") || s.contains("alt") || s.contains("soc") ||
                s.contains("soh") || s.contains("inversor") || s.contains("inverter") || s.contains("bms") ||
                s.contains("corriente") || s.contains("current") || s.contains("hv") ->
                PidCategory.ELECTRICAL
            s.contains("dtc") || s.contains("mil") || s.contains("avería") || s.contains("averia") ||
                s.contains("odómetro") || s.contains("odometro") || s.contains("odometer") ||
                s.contains("monitor") || s.contains("tpms") || s.contains("neumático") || s.contains("neumatico") ->
                PidCategory.DIAGNOSTICS
            s.contains("oil") || s.contains("fuel") || s.contains("combustible") || s.contains("consum") ||
                s.contains("gas") || s.contains("rail") || s.contains("inyec") || s.contains("inj") ||
                s.contains("etanol") || s.contains("ethanol") || s.contains("stft") || s.contains("ltft") ||
                s.contains("trim") || s.contains("evap") ->
                PidCategory.FUEL
            s.contains("rpm") || s.contains("speed") || s.contains("velocidad") || s.contains("coolant") ||
                s.contains("refrigerante") || s.contains("throttle") || s.contains("mariposa") ||
                s.contains("acelerador") || s.contains("pedal") || s.contains("temp") || s.contains("boost") ||
                s.contains("turbo") || s.contains("vgt") || s.contains("wastegate") || s.contains("map") ||
                s.contains("maf") || s.contains("admisión") || s.contains("admision") || s.contains("intake") ||
                s.contains("intercooler") || s.contains("carga") || s.contains("load") || s.contains("avance") ||
                s.contains("timing") || s.contains("barom") || s.contains("aceite") || s.contains("cht") ||
                s.contains("compresor") || s.contains("turbina") ->
                PidCategory.ENGINE
            else -> PidCategory.CUSTOM
        }
    }
}
