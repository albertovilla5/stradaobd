package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.*
import com.torqueobd2.dash.i18n.tr
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/** Cada lectura aceptada se escribe una vez, sin depender de la frecuencia de la interfaz. */
class TelemetryRecorder(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val nowNanos: () -> Long = System::nanoTime
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(RecordingState())
    val state = _state.asStateFlow()
    private var initialized = false
    private var session: RecordingSession? = null
    private var output: FileOutputStream? = null
    private var writer: BufferedWriter? = null
    private var startNanos = 0L
    private val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private suspend fun <T> serial(block: () -> T): T = withContext(ioDispatcher) { mutex.withLock { block() } }
    private fun csvFile(id: String) = File(directory, "$id.csv")
    private fun infoFile(id: String) = File(directory, "$id.info")

    suspend fun refresh() = serial {
        try { initialize(); publish() } catch (e: IOException) { fail(e) }
    }

    suspend fun start(context: RecordingContext): Boolean = serial {
        try {
            initialize()
            if (session != null) return@serial false
            val created = RecordingSession(UUID.randomUUID().toString(), context, nowMillis())
            session = created
            startNanos = nowNanos()
            persist(created)
            output = FileOutputStream(csvFile(created.id))
            writer = output!!.bufferedWriter(Charsets.UTF_8)
            writer!!.write(csvRow(listOf("record_type", "timestamp_utc", "timestamp_ms", "elapsed_ms", "session_id",
                "vehicle_id", "vehicle_name", "source", "adapter_name", "adapter_address", "pid_id", "sensor_name",
                "command", "ecu_header", "equation", "value", "unit", "status", "detail")))
            writeEvent("START", tr("Inicio de grabación", "Recording started"))
            _state.value = _state.value.copy(error = null)
            flushInternal(true)
            publish()
            true
        } catch (e: IOException) { fail(e); false }
    }

    suspend fun record(reading: ObdReading, pid: ObdPid, context: RecordingContext) = serial {
        val active = session ?: return@serial
        // No escribir valores ausentes, no finitos ni datos de otra definición.
        if (reading.pidId != pid.id || reading.units != pid.units || !reading.decodedValue.isFinite() || !reading.value.isFinite()) return@serial
        try {
            if (active.context != context || reading.isSimulated != active.isSimulated) {
                finish(RecordingEnd.INTERRUPTED, tr("Cambió el vehículo o el origen de los datos", "Vehicle or data source changed"))
                return@serial
            }
            writer!!.write(csvRow(prefix("SENSOR", reading.timestamp, reading.receivedAtNanos) + listOf(
                pid.id, pid.name, pid.cleanObdCommand(), pid.header, pid.equation, reading.decodedValue.toString(),
                reading.units, if (reading.isSimulated) "SIMULATED" else "AVAILABLE", "")))
            session = active.copy(sampleCount = active.sampleCount + 1)
        } catch (e: IOException) { fail(e) }
    }

    suspend fun event(status: String, detail: String) = serial {
        if (session == null) return@serial
        try { writeEvent(status, detail); flushInternal(true) } catch (e: IOException) { fail(e) }
    }

    suspend fun flush() = serial {
        if (session == null) return@serial
        try { flushInternal(true) } catch (e: IOException) { fail(e) }
    }

    suspend fun stop(reason: String = tr("Detenida por el usuario", "Stopped by user"), end: RecordingEnd = RecordingEnd.COMPLETED) = serial {
        if (session == null) return@serial
        try { finish(end, reason) } catch (e: IOException) { fail(e) }
    }

    suspend fun delete(id: String) = serial {
        initialize()
        require(_state.value.sessions.any { it.id == id } && session?.id != id) { tr("La sesión no está finalizada", "Session is not finished") }
        if (csvFile(id).exists() && !csvFile(id).delete()) throw IOException(tr("No se pudo eliminar el CSV", "Could not delete CSV file"))
        if (!infoFile(id).delete()) throw IOException(tr("No se pudo eliminar la información de la sesión", "Could not delete session info"))
        publish()
    }

    suspend fun export(id: String, destination: OutputStream) = serial {
        initialize()
        require(_state.value.sessions.any { it.id == id } && session?.id != id) { tr("Detén la grabación antes de exportarla", "Stop recording before exporting") }
        csvFile(id).inputStream().use { it.copyTo(destination) }
    }

    private fun prefix(kind: String, millis: Long, nanos: Long): List<String> {
        val active = session!!
        return listOf(kind, utc.format(Date(millis)), millis.toString(), ((nanos - startNanos).coerceAtLeast(0) / 1_000_000).toString(),
            active.id, active.context.vehicleId, active.context.vehicleName, active.context.connectionType.name,
            active.context.adapterName, active.context.adapterAddress)
    }

    private fun writeEvent(status: String, detail: String) {
        writer!!.write(csvRow(prefix("EVENT", nowMillis(), nowNanos()) + List(7) { "" } + listOf(status, detail)))
    }

    private fun flushInternal(sync: Boolean) {
        writer?.flush()
        if (sync) output?.fd?.sync()
        session?.let {
            session = it.copy(bytes = csvFile(it.id).length())
            persist(session!!)
            _state.value = _state.value.copy(active = session)
        }
    }

    private fun finish(end: RecordingEnd, reason: String) {
        val active = session ?: return
        writeEvent(end.name, reason)
        flushInternal(true)
        writer?.close()
        writer = null; output = null
        val completed = session!!.copy(endedAtMillis = nowMillis(), end = end, reason = reason, bytes = csvFile(active.id).length())
        persist(completed)
        session = null
        publish()
    }

    private fun fail(error: IOException) {
        val active = session
        runCatching { writer?.close() }
        writer = null; output = null; session = null
        val storageFallback = tr("Error de almacenamiento", "Storage error")
        if (active != null) runCatching {
            val samples = recoverCsv(csvFile(active.id))
            persist(active.copy(endedAtMillis = nowMillis(), sampleCount = samples, end = RecordingEnd.ERROR,
                reason = error.message ?: storageFallback, bytes = csvFile(active.id).length()))
        }
        runCatching { publish() }
        val detail = error.message ?: tr("error de almacenamiento", "storage error")
        _state.value = _state.value.copy(active = null, error = tr("No se pudo guardar la grabación: $detail", "Could not save recording: $detail"))
    }

    private fun initialize() {
        if (initialized) return
        if (!directory.exists() && !directory.mkdirs()) throw IOException(tr("No se pudo crear el directorio de grabaciones", "Could not create recordings directory"))
        val saved = loadSessions()
        saved.filter { it.end == null }.forEach { interrupted ->
            val file = csvFile(interrupted.id)
            val lastUpdate = file.lastModified().coerceAtLeast(interrupted.startedAtMillis)
            val samples = recoverCsv(file)
            persist(interrupted.copy(endedAtMillis = lastUpdate, sampleCount = samples, bytes = file.length(),
                end = RecordingEnd.INTERRUPTED, reason = tr("El proceso terminó antes de cerrar la sesión", "Process ended before closing the session")))
        }
        initialized = true
        publish()
    }

    private fun persist(record: RecordingSession) {
        val properties = Properties().apply {
            setProperty("id", record.id); setProperty("vehicle", record.context.vehicleId)
            setProperty("name", record.context.vehicleName); setProperty("source", record.context.connectionType.name)
            setProperty("adapter_name", record.context.adapterName); setProperty("adapter_address", record.context.adapterAddress)
            setProperty("start", record.startedAtMillis.toString()); setProperty("end", record.endedAtMillis.toString())
            setProperty("samples", record.sampleCount.toString()); setProperty("status", record.end?.name ?: "")
            setProperty("reason", record.reason)
        }
        val temporary = File(directory, "${record.id}.info.tmp")
        FileOutputStream(temporary).use { properties.store(it, null); it.fd.sync() }
        if (!temporary.renameTo(infoFile(record.id))) throw IOException(tr("No se pudo guardar la información de la sesión", "Could not save session info"))
    }

    private fun loadSessions(): List<RecordingSession> = directory.listFiles().orEmpty().filter { it.extension == "info" }.mapNotNull { file ->
        runCatching {
            val p = Properties().apply { file.inputStream().use { load(it) } }
            val id = p.getProperty("id")
            require(UUID.fromString(id).toString() == id && file.nameWithoutExtension == id)
            RecordingSession(id, RecordingContext(p.getProperty("vehicle"), p.getProperty("name"),
                ConnectionType.valueOf(p.getProperty("source")), p.getProperty("adapter_name"), p.getProperty("adapter_address")),
                p.getProperty("start").toLong(), p.getProperty("end").toLong(), p.getProperty("samples").toLong(),
                csvFile(id).length(), p.getProperty("status").takeIf { it.isNotEmpty() }?.let(RecordingEnd::valueOf), p.getProperty("reason", ""))
        }.getOrNull()
    }.sortedByDescending { it.startedAtMillis }

    private fun publish() {
        _state.value = _state.value.copy(active = session, sessions = loadSessions().filter { it.id != session?.id })
    }

    companion object {
        fun csvRow(values: List<String>): String = values.joinToString(",") { value ->
            if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
        } + "\n"

        /** Quita exclusivamente una cola incompleta, respetando campos CSV con saltos de línea. */
        internal fun recoverCsv(file: File): Long {
            if (!file.exists()) return 0
            var lastComplete = 0L
            var quotes = false
            var samples = 0L
            val prefix = StringBuilder()
            var offset = 0L
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    for (index in 0 until count) {
                        val byte = buffer[index].toInt() and 255
                        offset++
                        if (prefix.length < 7) prefix.append(byte.toChar())
                        if (byte == '"'.code) quotes = !quotes
                        if (byte == '\n'.code && !quotes) {
                            lastComplete = offset
                            if (prefix.toString() == "SENSOR,") samples++
                            prefix.setLength(0)
                        }
                    }
                }
            }
            if (lastComplete < file.length()) RandomAccessFile(file, "rw").use { it.setLength(lastComplete) }
            return samples
        }
    }
}
