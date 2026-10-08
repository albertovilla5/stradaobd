package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.parser.*
import com.torqueobd2.dash.data.repository.DefaultPids
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Una captura de diagnóstico usa exclusivamente respuestas de su propia transacción OBD. */
class DtcDiagnosticReader(private val exchange: suspend (String) -> String, private val canProtocol: Boolean?,
    private val progress: (String) -> Unit = {}) {
    private val raw = linkedMapOf<String, String>()
    private suspend fun query(command: String): DiagnosticReply {
        currentCoroutineContext().ensureActive()
        progress("Consultando $command")
        val response = exchange(command)
        currentCoroutineContext().ensureActive()
        raw[command] = response
        return DiagnosticResponseParser.parse(command, response, canProtocol)
    }
    suspend fun read(vehicle: VehicleProfile, adapter: String, simulated: Boolean, session: Long): DiagnosticReport {
        raw.clear()
        val monitorsReply = query("0101")
        val monitors = monitorsReply.frames.mapNotNull { frame ->
            DiagnosticResponseParser.pidData(frame, 1, 1)?.takeIf { it.size >= 4 }?.let { data ->
                val items = mutableListOf<ReadinessMonitor>()
                listOf("Fallos de combustión", "Sistema de combustible", "Componentes integrales").forEachIndexed { bit, name ->
                    if (data[1] and (1 shl bit) != 0) items.add(ReadinessMonitor(name, data[1] and (1 shl (bit + 4)) == 0))
                }
                val names = if (data[1] and 8 == 0) listOf("Catalizador", "Catalizador calentado", "EVAP", "Aire secundario",
                    "Refrigerante del aire acondicionado", "Sonda de oxígeno", "Calefactor de sonda", "EGR / VVT")
                    else listOf("Catalizador NMHC", "NOx / SCR", "", "Sobrealimentación", "", "Sensor de escape", "Filtro de partículas", "EGR / VVT")
                names.forEachIndexed { bit, name ->
                    if (name.isNotEmpty() && data[2] and (1 shl bit) != 0) items.add(ReadinessMonitor(name, data[3] and (1 shl bit) == 0))
                }
                EcuMonitorStatus(frame.ecu, data[0] and 128 != 0, data[0] and 127, items)
            }
        }
        val monitorStatus = if (monitorsReply.frames.isEmpty()) monitorsReply.status
            else if (monitors.isEmpty()) DiagnosticStatus.INVALID
            else if (monitors.size < monitorsReply.frames.size || monitorsReply.status != DiagnosticStatus.OK) DiagnosticStatus.PARTIAL else DiagnosticStatus.OK
        val batches = DtcKind.entries.map { kind -> DiagnosticResponseParser.dtcs(kind, query(kind.mode)) }
        val freeze = readFreezeFrame()
        return DiagnosticReport(vehicle.id, vehicle.name, adapter, simulated, session, batches = batches,
            monitorStatus = monitorStatus, monitors = monitors, freezeFrame = freeze, rawResponses = raw.toMap())
    }
    private suspend fun readFreezeFrame(): FreezeFrameSnapshot {
        val trigger = query("020200")
        val codes = mutableMapOf<String, String>()
        val issues = trigger.issues.toMutableList()
        if (trigger.status == DiagnosticStatus.PARTIAL && issues.isEmpty()) issues.add("Identificación del cuadro congelado: lectura parcial")
        var declaredEmpty = false
        trigger.frames.forEach { frame ->
            val data = DiagnosticResponseParser.pidData(frame, 2, 2)
            if (data == null || data.size < 2) issues.add("ECU ${frame.ecu}: código del cuadro congelado inválido")
            else DiagnosticResponseParser.decodeCode(data[0], data[1])?.let { codes[frame.ecu] = it } ?: run { declaredEmpty = true }
        }
        if (codes.isEmpty() && declaredEmpty) return FreezeFrameSnapshot(status = if (issues.isEmpty()) DiagnosticStatus.OK else DiagnosticStatus.PARTIAL, issues = issues)
        // Sin respuesta positiva no se confunden valores actuales con datos congelados.
        if (codes.isEmpty()) return FreezeFrameSnapshot(status = if (trigger.frames.isNotEmpty()) DiagnosticStatus.INVALID else trigger.status,
            issues = issues.ifEmpty { listOf("La ECU no ha comunicado un cuadro congelado válido.") })
        val supported = mutableMapOf<String, MutableSet<Int>>()
        val checked = mutableMapOf<String, MutableSet<Int>>()
        for (base in listOf(0, 0x20, 0x40)) {
            if (base != 0 && supported.values.none { base in it }) break
            val reply = query("02%02X00".format(base))
            reply.frames.forEach { frame ->
                val bytes = DiagnosticResponseParser.pidData(frame, 2, base)
                if (bytes != null && bytes.size >= 4) {
                    val range = checked.getOrPut(frame.ecu) { mutableSetOf() }
                    range.addAll(base + 1..base + 0x20)
                    val set = supported.getOrPut(frame.ecu) { mutableSetOf() }
                    bytes.take(4).forEachIndexed { index, value -> for (bit in 7 downTo 0) {
                        if (value and (1 shl bit) != 0) set.add(base + index * 8 + (8 - bit))
                    } }
                    if (base + 0x20 !in set) range.addAll(base + 0x21..0xFF)
                }
            }
        }
        val wanted = setOf("0104", "0105", "010B", "010C", "010D", "010F", "0110", "0111", "0123", "012F", "0142", "015C", "015E")
        val definitions = DefaultPids.getStandardPids().filter { it.cleanObdCommand() in wanted }
        val values = mutableListOf<FreezeFrameValue>()
        definitions.forEach { pid ->
            val number = pid.cleanObdCommand().takeLast(2).toInt(16)
            if (codes.keys.all { number in checked[it].orEmpty() && number !in supported[it].orEmpty() }) return@forEach
            val reply = query("02%02X00".format(number))
            var valid = false
            val responding = mutableSetOf<String>()
            reply.frames.filter { it.ecu in codes }.forEach { frame ->
                responding.add(frame.ecu)
                val bytes = DiagnosticResponseParser.pidData(frame, 2, number)
                try {
                    require(bytes != null && bytes.isNotEmpty())
                    val decoded = PidDecoder.decode(pid, EquationEvaluator.mapBytesToVariables(bytes!!.toIntArray()))
                    require(decoded.isFinite())
                    values.add(FreezeFrameValue(frame.ecu, pid.name, decoded, pid.units)); valid = true
                } catch (_: IllegalArgumentException) { issues.add("ECU ${frame.ecu}: dato congelado inválido para ${pid.name}") }
            }
            codes.keys.filter { it !in responding && !(number in checked[it].orEmpty() && number !in supported[it].orEmpty()) }
                .forEach { issues.add("ECU $it: sin dato congelado para ${pid.name}") }
            issues.addAll(reply.issues)
            if (!valid) issues.add("${pid.name}: ${reply.status.label}")
        }
        return FreezeFrameSnapshot(status = if (values.isEmpty()) DiagnosticStatus.NO_DATA
            else if (issues.isNotEmpty()) DiagnosticStatus.PARTIAL else DiagnosticStatus.OK,
            triggeringCodes = codes, values = values, issues = issues.distinct())
    }
    suspend fun clear(): ClearDtcResult {
        val reply = query("04")
        val accepted = reply.frames.filter { it.bytes == listOf(0x44) }.map { it.ecu }.distinct()
        val status = if (accepted.isEmpty()) {
            if (reply.frames.isEmpty()) reply.status else DiagnosticStatus.INVALID
        } else if (reply.status != DiagnosticStatus.OK || accepted.size < reply.frames.size) DiagnosticStatus.PARTIAL else DiagnosticStatus.OK
        val message = if (accepted.isEmpty()) "No se ha confirmado el borrado. ${status.label}. Vuelve a leer las averías antes de reintentarlo."
            else "Borrado confirmado por ${accepted.joinToString { "ECU $it" }}. Los códigos permanentes no se borran con esta operación." +
                if (status == DiagnosticStatus.PARTIAL) " Hay respuestas sin confirmación; revisa la nueva lectura." else ""
        return ClearDtcResult(status, accepted, message)
    }
}
