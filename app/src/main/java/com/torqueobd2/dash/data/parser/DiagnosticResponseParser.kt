package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.tr

/** Reensambla mensajes por ECU; nunca concatena respuestas de módulos distintos. */
object DiagnosticResponseParser {
    private data class Pending(val length: Int, val bytes: MutableList<Int>, var sequence: Int = 1)
    fun parse(command: String, raw: String, canProtocol: Boolean? = null): DiagnosticReply {
        val cmd = command.replace(" ", "").uppercase()
        val mode = cmd.take(2).toIntOrNull(16) ?: return DiagnosticReply(DiagnosticStatus.INVALID)
        val positive = mode + 0x40
        val frames = mutableListOf<DiagnosticFrame>()
        val issues = mutableListOf<String>()
        val pending = mutableMapOf<String, Pending>()
        var negative: DiagnosticStatus? = null
        var noData = false
        var indexed: MutableList<Int>? = null
        var nextIndex = 0
        var indexedLength: Int? = null
        fun add(ecu: String, bytes: List<Int>, can: Boolean?) {
            when {
                bytes.firstOrNull() == positive -> frames.add(DiagnosticFrame(ecu, bytes, can))
                bytes.size == 3 && bytes[0] == 0x7F && bytes[1] == mode -> {
                    val status = if (bytes[2] in listOf(0x11, 0x12, 0x31)) DiagnosticStatus.UNSUPPORTED else DiagnosticStatus.NO_DATA
                    negative = status
                    val hexBytes = bytes.joinToString(" ") { "%02X".format(it) }
                    issues.add(tr("ECU $ecu: respuesta negativa $hexBytes", "ECU $ecu: negative response $hexBytes"))
                }
                else -> issues.add(tr("ECU $ecu: respuesta ajena al comando o incompleta", "ECU $ecu: unrelated or incomplete command response"))
            }
        }
        fun finishIndexed() {
            indexed?.let { bytes ->
                if (indexedLength != null && bytes.size != indexedLength) issues.add(tr("Respuesta indexada truncada", "Truncated indexed response"))
                else add(tr("Sin identificar", "Unidentified"), bytes, canProtocol)
            }
            indexed = null; indexedLength = null; nextIndex = 0
        }
        fun canFrame(ecu: String, bytes: List<Int>) {
            if (bytes.isEmpty()) { issues.add(tr("ECU $ecu: trama vacía", "ECU $ecu: empty frame")); return }
            when (bytes[0] shr 4) {
                0 -> {
                    if (pending.remove(ecu) != null) issues.add(tr("ECU $ecu: mensaje previo truncado", "ECU $ecu: previous message truncated"))
                    val length = bytes[0] and 0xF
                    if (length !in 1..7 || bytes.size < length + 1) issues.add(tr("ECU $ecu: longitud CAN inválida", "ECU $ecu: invalid CAN length"))
                    else add(ecu, bytes.drop(1).take(length), true)
                }
                1 -> {
                    if (pending.remove(ecu) != null) issues.add(tr("ECU $ecu: primer segmento repetido", "ECU $ecu: repeated first frame"))
                    if (bytes.size < 3) { issues.add(tr("ECU $ecu: primer segmento truncado", "ECU $ecu: truncated first frame")); return }
                    val length = ((bytes[0] and 15) shl 8) or bytes[1]
                    if (length !in 8..4095 || bytes.size != 8) { issues.add(tr("ECU $ecu: longitud multitrama inválida", "ECU $ecu: invalid multi-frame length")); return }
                    pending[ecu] = Pending(length, bytes.drop(2).toMutableList())
                }
                2 -> {
                    val message = pending[ecu]
                    if (message == null || bytes[0] and 15 != message.sequence || bytes.size < 2) {
                        pending.remove(ecu); issues.add(tr("ECU $ecu: secuencia CAN incompleta o desordenada", "ECU $ecu: incomplete or out-of-order CAN sequence")); return
                    }
                    message.sequence = (message.sequence + 1) and 15
                    val remaining = message.length - message.bytes.size
                    if (bytes.size - 1 < minOf(remaining, 7)) {
                        pending.remove(ecu); issues.add(tr("ECU $ecu: segmento CAN truncado", "ECU $ecu: truncated CAN segment")); return
                    }
                    message.bytes.addAll(bytes.drop(1).take(remaining))
                    if (message.bytes.size >= message.length) { pending.remove(ecu); add(ecu, message.bytes, true) }
                }
                else -> {
                    // Algunos adaptadores eliminan PCI incluso con ATH1 y CAF1.
                    if (bytes[0] == positive || bytes[0] == 0x7F) add(ecu, bytes, true)
                    else issues.add(tr("ECU $ecu: tipo de trama CAN no reconocido", "ECU $ecu: unrecognized CAN frame type"))
                }
            }
        }
        fun bytes(hex: String): List<Int>? = if (hex.isNotEmpty() && hex.length % 2 == 0 && hex.matches(Regex("[0-9A-F]+")))
            hex.chunked(2).map { it.toInt(16) } else null

        if (raw.length > 100_000) return DiagnosticReply(DiagnosticStatus.INVALID, issues = listOf(tr("Respuesta demasiado larga", "Response too long")))
        val lines = raw.replace(">", "").replace("SEARCHING...", "", true).replace("SEARCHING", "", true)
            .split(Regex("[\\r\\n]+"))
        for (line in lines) {
            val text = line.trim().uppercase()
            val compact = text.replace(Regex("\\s+"), "")
            if (compact.isEmpty() || compact == cmd) continue
            if (text == "NO DATA") { noData = true; continue }
            if (text == "?" || text.contains("ERROR") || text.contains("STOPPED") || text.contains("UNABLE") || text.contains("BUS BUSY")) {
                issues.add(text); continue
            }
            val index = Regex("^([0-9A-F]+):\\s*(.*)$").matchEntire(text)
            if (index != null) {
                val number = index.groupValues[1].toIntOrNull(16)
                if (number == 0) { if (indexed != null) finishIndexed(); indexed = mutableListOf(); nextIndex = 0 }
                val data = bytes(index.groupValues[2].replace(Regex("\\s"), ""))
                if (indexed == null || number != nextIndex++ || data == null) {
                    indexed = null; indexedLength = null; issues.add(tr("Secuencia indexada inválida", "Invalid indexed sequence"))
                } else indexed!!.addAll(data)
                continue
            }
            if (compact.matches(Regex("[0-9A-F]{3}")) && compact.toInt(16) > 7) {
                // Longitud previa a las líneas 0:, 1: de ELM/CAF1.
                finishIndexed(); indexedLength = compact.toInt(16); continue
            }
            finishIndexed()
            // ATH0 se acepta sin asignar falsamente una dirección de ECU.
            if (compact.startsWith("%02X".format(positive)) || compact.startsWith("7F")) {
                val payload = bytes(compact)
                if (payload == null) issues.add(tr("Respuesta sin cabecera inválida", "Invalid headerless response")) else add(tr("Sin identificar", "Unidentified"), payload, canProtocol)
                continue
            }
            // Protocolos J1850/ISO9141/KWP: tres bytes de cabecera y checksum final.
            val tokens = text.split(Regex("\\s+"))
            val explicitCanHeader = tokens.first().length in listOf(3, 8) && tokens.size > 1
            val legacy = bytes(compact)
            if (!explicitCanHeader && legacy != null && legacy.size >= 5 && (legacy[3] == positive || legacy[3] == 0x7F ||
                (legacy[0] and 0x3F == 0 && legacy.size >= 6 && legacy[4] == positive))) {
                val explicitLength = legacy[0] and 0x3F == 0 && legacy[3] != positive && legacy[3] != 0x7F
                val start = if (explicitLength) 4 else 3
                val payload = legacy.subList(start, legacy.lastIndex)
                val ecuHex = "%02X".format(legacy[2])
                if (explicitLength && legacy[3] != payload.size) issues.add(tr("ECU $ecuHex: longitud KWP inválida", "ECU $ecuHex: invalid KWP length"))
                else add(ecuHex, payload, false)
                continue
            }
            val headerLength = when {
                tokens.first().length == 3 -> 3
                tokens.first().length == 8 && tokens.first().toLongOrNull(16)?.let { it <= 0x1FFFFFFF } == true -> 8
                compact.length % 2 == 1 && compact.take(3).toIntOrNull(16)?.let { it <= 0x7FF } == true -> 3
                compact.length >= 10 && compact.take(8).toLongOrNull(16)?.let { it <= 0x1FFFFFFF } == true -> 8
                else -> 0
            }
            if (headerLength == 0) { issues.add(tr("Cabecera o formato no reconocido: $text", "Unrecognized header or format: $text")); continue }
            val ecu = compact.take(headerLength)
            var data = compact.drop(headerLength)
            if (data.length % 2 == 1) {
                val dlc = data.take(1).toIntOrNull(16)
                data = data.drop(1)
                if (dlc == null || dlc !in 1..8 || dlc * 2 != data.length) { issues.add(tr("ECU $ecu: DLC inválido", "ECU $ecu: invalid DLC")); continue }
            }
            val payload = bytes(data)
            if (payload == null || payload.size > 8 && payload.firstOrNull()?.let { it != positive } == true) issues.add(tr("ECU $ecu: bytes CAN inválidos", "ECU $ecu: invalid CAN bytes"))
            else canFrame(ecu, payload ?: emptyList())
        }
        finishIndexed()
        pending.keys.forEach { issues.add(tr("ECU $it: respuesta multitrama truncada", "ECU $it: truncated multi-frame response")) }
        val status = when {
            frames.isNotEmpty() -> if (issues.isEmpty() && !noData) DiagnosticStatus.OK else DiagnosticStatus.PARTIAL
            negative != null -> negative!!
            issues.isNotEmpty() -> DiagnosticStatus.INVALID
            else -> DiagnosticStatus.NO_DATA
        }
        return DiagnosticReply(status, frames, issues.distinct())
    }

    fun decodeCode(high: Int, low: Int): String? {
        require(high in 0..255 && low in 0..255)
        if (high == 0 && low == 0) return null
        return "${"PCBU"[high shr 6]}${(high shr 4) and 3}${"%01X%02X".format(high and 15, low)}"
    }

    fun dtcs(kind: DtcKind, reply: DiagnosticReply): DtcBatch {
        val codes = mutableListOf<TroubleCode>()
        val ecus = mutableListOf<String>()
        val issues = reply.issues.toMutableList()
        reply.frames.forEach { frame ->
            val data = frame.bytes.drop(1)
            val counted = frame.can ?: (data.size % 2 == 1)
            val pairs = if (counted) {
                val count = data.firstOrNull()
                if (count == null || data.size < 1 + count * 2 || data.drop(1 + count * 2).any { it != 0 }) {
                    issues.add(tr("ECU ${frame.ecu}: número de DTC o longitud inválidos", "ECU ${frame.ecu}: invalid DTC count or length")); return@forEach
                }
                data.drop(1).take(count * 2)
            } else {
                if (data.isEmpty() || data.size % 2 != 0) { issues.add(tr("ECU ${frame.ecu}: pares DTC incompletos", "ECU ${frame.ecu}: incomplete DTC pairs")); return@forEach }
                data
            }
            val decoded = pairs.chunked(2).mapNotNull { decodeCode(it[0], it[1]) }
            if (counted && decoded.size != data[0]) { issues.add(tr("ECU ${frame.ecu}: contador DTC inconsistente", "ECU ${frame.ecu}: inconsistent DTC count")); return@forEach }
            ecus.add(frame.ecu)
            decoded.forEach { codes.add(TroubleCode(it, frame.ecu, kind)) }
        }
        val status = if (reply.frames.isEmpty()) reply.status else if (ecus.isEmpty()) DiagnosticStatus.INVALID
            else if (issues.isNotEmpty() || reply.status != DiagnosticStatus.OK) DiagnosticStatus.PARTIAL else DiagnosticStatus.OK
        return DtcBatch(kind, status, codes.distinct(), ecus.distinct(), issues.distinct())
    }

    /** Verifica SID, PID y número de cuadro antes de usar los bytes del dato. */
    fun pidData(frame: DiagnosticFrame, mode: Int, pid: Int, freezeNumber: Int = 0): List<Int>? {
        val prefix = if (mode == 2) listOf(0x42, pid, freezeNumber) else listOf(0x41, pid)
        return frame.bytes.takeIf { it.take(prefix.size) == prefix }?.drop(prefix.size)
    }
}
