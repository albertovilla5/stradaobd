package com.torqueobd2.dash.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticStatus(val label: String) {
    OK("Lectura completa"), PARTIAL("Lectura parcial"), NO_DATA("Sin respuesta / sin datos"),
    UNSUPPORTED("Modo no admitido"), INVALID("Respuesta inválida")
}
enum class DtcKind(val mode: String, val label: String, val explanation: String) {
    CONFIRMED("03", "Confirmados", "Averías de emisiones almacenadas por la ECU."),
    PENDING("07", "Pendientes", "Detectadas durante el ciclo actual o el último completado; pueden no encender el testigo."),
    PERMANENT("0A", "Permanentes", "La ECU los elimina cuando verifica la reparación. El borrado estándar no los elimina.")
}
data class DiagnosticFrame(val ecu: String, val bytes: List<Int>, val can: Boolean?)
data class DiagnosticReply(val status: DiagnosticStatus, val frames: List<DiagnosticFrame> = emptyList(),
    val issues: List<String> = emptyList())
data class TroubleCode(val code: String, val ecu: String, val kind: DtcKind)
data class DtcBatch(val kind: DtcKind, val status: DiagnosticStatus, val codes: List<TroubleCode> = emptyList(),
    val respondingEcus: List<String> = emptyList(), val issues: List<String> = emptyList())
data class ReadinessMonitor(val name: String, val complete: Boolean)
data class EcuMonitorStatus(val ecu: String, val milOn: Boolean, val confirmedCount: Int,
    val monitors: List<ReadinessMonitor>)
data class FreezeFrameValue(val ecu: String, val name: String, val value: Double, val unit: String)
data class FreezeFrameSnapshot(val frameNumber: Int = 0, val status: DiagnosticStatus,
    val triggeringCodes: Map<String, String> = emptyMap(), val values: List<FreezeFrameValue> = emptyList(),
    val issues: List<String> = emptyList())
data class DiagnosticReport(val vehicleId: String, val vehicleName: String, val adapterName: String,
    val simulated: Boolean, val session: Long, val capturedAt: Long = System.currentTimeMillis(),
    val batches: List<DtcBatch>, val monitorStatus: DiagnosticStatus, val monitors: List<EcuMonitorStatus>,
    val freezeFrame: FreezeFrameSnapshot, val rawResponses: Map<String, String> = emptyMap()) {
    val codes get() = batches.flatMap { it.codes }
    val complete get() = batches.all { it.status == DiagnosticStatus.OK } && monitorStatus == DiagnosticStatus.OK && freezeFrame.status == DiagnosticStatus.OK
    fun asText(description: (String) -> String): String = buildString {
        appendLine("Torque OBD2 Dash · Informe de averías")
        appendLine(if (simulated) "SIMULACIÓN · DATOS FICTICIOS" else "Lecturas OBD de emisiones")
        appendLine("Vehículo: $vehicleName")
        appendLine("Adaptador: $adapterName")
        appendLine("Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(capturedAt))}")
        appendLine("Este informe es una captura; no sustituye una nueva consulta a la ECU.")
        appendLine()
        appendLine("Testigo MIL y monitores desde el último borrado: ${monitorStatus.label}")
        monitors.forEach { ecu ->
            appendLine("ECU ${ecu.ecu}: MIL ${if (ecu.milOn) "encendido" else "apagado"}; ${ecu.confirmedCount} DTC confirmados declarados")
            ecu.monitors.forEach { appendLine("  ${it.name}: ${if (it.complete) "Completado" else "Pendiente"}") }
        }
        batches.forEach { batch ->
            appendLine(); appendLine("${batch.kind.label}: ${batch.status.label}")
            if (batch.codes.isEmpty() && batch.status == DiagnosticStatus.OK) appendLine("Sin códigos en las ECU que respondieron: ${batch.respondingEcus.joinToString()}")
            batch.codes.forEach { appendLine("${it.code} · ECU ${it.ecu} · ${description(it.code)}") }
            batch.issues.forEach { appendLine("Aviso: $it") }
        }
        appendLine(); appendLine("Datos congelados · cuadro ${freezeFrame.frameNumber}: ${freezeFrame.status.label}")
        freezeFrame.triggeringCodes.forEach { (ecu, code) -> appendLine("ECU $ecu: originado por $code") }
        freezeFrame.values.forEach { appendLine("ECU ${it.ecu}: ${it.name} = ${String.format(Locale.US, "%.2f", it.value)} ${it.unit}") }
        freezeFrame.issues.forEach { appendLine("Aviso: $it") }
        appendLine("Los códigos permanentes solo los elimina la ECU tras verificar la reparación.")
        appendLine("Un código identifica una condición detectada; no confirma qué pieza necesita sustitución.")
        appendLine(); appendLine("Respuestas originales:")
        rawResponses.forEach { (command, raw) -> appendLine("> $command\n${raw.trim()}") }
    }
}
data class ClearDtcResult(val status: DiagnosticStatus, val acceptedEcus: List<String> = emptyList(), val message: String)
data class DtcDiagnosticState(val busy: Boolean = false, val progress: String = "", val report: DiagnosticReport? = null,
    val error: String? = null, val clearResult: ClearDtcResult? = null)
