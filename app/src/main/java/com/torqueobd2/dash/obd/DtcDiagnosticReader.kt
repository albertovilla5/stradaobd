package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.data.parser.*
import com.torqueobd2.dash.data.repository.DefaultPids
import com.torqueobd2.dash.i18n.tr
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Una captura de diagnóstico usa exclusivamente respuestas de su propia transacción OBD. */
class DtcDiagnosticReader(private val exchange: suspend (String) -> String, private val canProtocol: Boolean?,
    private val progress: (String) -> Unit = {}) {
    private val raw = linkedMapOf<String, String>()
    private suspend fun query(command: String): DiagnosticReply {
        currentCoroutineContext().ensureActive()
        progress(tr("Consultando $command", "Querying $command"))
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
                listOf(
                    tr("Fallos de combustión", "Misfire"),
                    tr("Sistema de combustible", "Fuel system"),
                    tr("Componentes integrales", "Comprehensive components")
                ).forEachIndexed { bit, name ->
                    if (data[1] and (1 shl bit) != 0) items.add(ReadinessMonitor(name, data[1] and (1 shl (bit + 4)) == 0))
                }
                val names = if (data[1] and 8 == 0) listOf(
                    tr("Catalizador", "Catalyst"),
                    tr("Catalizador calentado", "Heated catalyst"),
                    "EVAP",
                    tr("Aire secundario", "Secondary air"),
                    tr("Refrigerante del aire acondicionado", "A/C refrigerant"),
                    tr("Sonda de oxígeno", "Oxygen sensor"),
                    tr("Calefactor de sonda", "Oxygen sensor heater"),
                    "EGR / VVT"
                ) else listOf(
                    tr("Catalizador NMHC", "NMHC catalyst"),
                    "NOx / SCR",
                    "",
                    tr("Sobrealimentación", "Boost pressure"),
                    "",
                    tr("Sensor de escape", "Exhaust gas sensor"),
                    tr("Filtro de partículas", "PM filter (DPF)"),
                    "EGR / VVT"
                )
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
        if (trigger.status == DiagnosticStatus.PARTIAL && issues.isEmpty()) {
            issues.add(tr("Identificación del cuadro congelado: lectura parcial", "Freeze frame identification: partial read"))
        }
        var declaredEmpty = false
        trigger.frames.forEach { frame ->
            val data = DiagnosticResponseParser.pidData(frame, 2, 2)
            if (data == null || data.size < 2) {
                issues.add(tr("ECU ${frame.ecu}: código del cuadro congelado inválido", "ECU ${frame.ecu}: invalid freeze frame code"))
            } else DiagnosticResponseParser.decodeCode(data[0], data[1])?.let { codes[frame.ecu] = it } ?: run { declaredEmpty = true }
        }
        if (codes.isEmpty() && declaredEmpty) return FreezeFrameSnapshot(status = if (issues.isEmpty()) DiagnosticStatus.OK else DiagnosticStatus.PARTIAL, issues = issues)
        // Sin respuesta positiva no se confunden valores actuales con datos congelados.
        if (codes.isEmpty()) return FreezeFrameSnapshot(
            status = if (trigger.frames.isNotEmpty()) DiagnosticStatus.INVALID else trigger.status,
            issues = issues.ifEmpty {
                listOf(tr("La ECU no ha comunicado un cuadro congelado válido.", "The ECU did not report a valid freeze frame."))
            }
        )
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
                } catch (_: IllegalArgumentException) {
                    issues.add(tr("ECU ${frame.ecu}: dato congelado inválido para ${pid.name}", "ECU ${frame.ecu}: invalid freeze frame data for ${pid.name}"))
                }
            }
            codes.keys.filter { it !in responding && !(number in checked[it].orEmpty() && number !in supported[it].orEmpty()) }
                .forEach { issues.add(tr("ECU $it: sin dato congelado para ${pid.name}", "ECU $it: no freeze frame data for ${pid.name}")) }
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
        val message = if (accepted.isEmpty()) {
            tr(
                "No se ha confirmado el borrado. ${status.label}. Vuelve a leer las averías antes de reintentarlo.",
                "Clear not confirmed. ${status.label}. Read trouble codes again before retrying."
            )
        } else {
            val ecuList = accepted.joinToString { "ECU $it" }
            tr(
                "Borrado confirmado por $ecuList. Los códigos permanentes no se borran con esta operación.",
                "Clear confirmed by $ecuList. Permanent trouble codes cannot be cleared by this operation."
            ) + if (status == DiagnosticStatus.PARTIAL) {
                tr(" Hay respuestas sin confirmación; revisa la nueva lectura.", " Some responses were unconfirmed; check the new reading.")
            } else ""
        }
        return ClearDtcResult(status, accepted, message)
    }
}

