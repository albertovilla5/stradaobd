package com.torqueobd2.dash.data.repository

import com.torqueobd2.dash.data.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

class TelemetryRecorderTest {
    @get:Rule val folder = TemporaryFolder()
    private val context = RecordingContext("vehicle-a", "Coche, \"azul\"\nDiésel", ConnectionType.WIFI, "ELM327", "192.168.0.10:35000")
    private val pid = ObdPid(id = "temperature", name = "Temperatura, motor", shortName = "T", modeAndPid = "0x0105", equation = "A-40", units = "°C")
    private fun reading(value: Double = 87.123456789, simulated: Boolean = false) = ObdReading(pid.id, pid.name, pid.shortName,
        value.toFloat(), pid.units, 0f, 120f, timestamp = 1_000, decodedValue = value, isSimulated = simulated, receivedAtNanos = 2_000_000_000)
    private fun recorder(directory: File) = TelemetryRecorder(directory, Dispatchers.Unconfined, { 1_000L }, { 1_000_000_000L })
    private fun csv(directory: File) = directory.listFiles()!!.single { it.extension == "csv" }

    @Test fun writesEachReceivedSampleOnceWithExactDoubleAndCsvEscaping() = runBlocking {
        val dir = folder.newFolder(); val recorder = recorder(dir)
        assertTrue(recorder.start(context))
        assertFalse(recorder.start(context))
        repeat(3) { recorder.record(reading(), pid, context) }
        recorder.stop()
        val text = csv(dir).readText()
        assertEquals(3L, recorder.state.value.sessions.single().sampleCount)
        assertEquals(3, Regex("(?m)^SENSOR,").findAll(text).count())
        assertTrue(text.contains("\"Coche, \"\"azul\"\"\nDiésel\""))
        assertTrue(text.contains("0105,,A-40,87.123456789,°C,AVAILABLE,"))
        assertTrue(text.contains("1970-01-01T00:00:01.000Z,1000,1000,"))
        assertTrue(text.contains(",COMPLETED,Detenida por el usuario"))
        assertNull(recorder.state.value.active)
    }

    @Test fun skipsInvalidValuesAndWrongDefinitions() = runBlocking {
        val dir = folder.newFolder(); val recorder = recorder(dir); recorder.start(context)
        recorder.record(reading(Double.NaN), pid, context)
        recorder.record(reading().copy(units = "F"), pid, context)
        recorder.record(reading().copy(pidId = "other"), pid, context)
        recorder.record(reading(Double.MAX_VALUE), pid, context)
        recorder.stop()
        assertEquals(0L, recorder.state.value.sessions.single().sampleCount)
    }

    @Test fun marksSimulationAndRefusesMixedSources() = runBlocking {
        val dir = folder.newFolder(); val recorder = recorder(dir)
        val simulated = context.copy(connectionType = ConnectionType.SIMULATION)
        recorder.start(simulated)
        recorder.record(reading(simulated = true), pid, simulated)
        recorder.record(reading(), pid, context)
        assertNull(recorder.state.value.active)
        assertEquals(RecordingEnd.INTERRUPTED, recorder.state.value.sessions.single().end)
        assertEquals(1L, recorder.state.value.sessions.single().sampleCount)
        assertTrue(csv(dir).readText().contains(",SIMULATED,"))
    }

    @Test fun refusesVehicleAndAdapterChangesWithoutMixingSamples() = runBlocking {
        for (changed in listOf(context.copy(vehicleId = "other"), context.copy(adapterAddress = "other"))) {
            val dir = folder.newFolder(); val recorder = recorder(dir); recorder.start(context)
            recorder.record(reading(), pid, context)
            recorder.record(reading(), pid, changed)
            assertEquals(RecordingEnd.INTERRUPTED, recorder.state.value.sessions.single().end)
            assertEquals(1L, recorder.state.value.sessions.single().sampleCount)
        }
    }

    @Test fun survivesRestartAndCanExportAndDeleteCompletedSession() = runBlocking {
        val dir = folder.newFolder(); val first = recorder(dir); first.start(context)
        first.record(reading(), pid, context); first.stop()
        val second = recorder(dir); second.refresh()
        val saved = second.state.value.sessions.single()
        assertEquals(context, saved.context)
        assertEquals(RecordingEnd.COMPLETED, saved.end)
        assertEquals(csv(dir).length(), saved.bytes)
        val exported = ByteArrayOutputStream(); second.export(saved.id, exported)
        assertArrayEquals(csv(dir).readBytes(), exported.toByteArray())
        second.delete(saved.id)
        assertTrue(second.state.value.sessions.isEmpty())
        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test fun recoversCompleteRowsAndTruncatesOnlyIncompleteTailAfterProcessDeath() = runBlocking {
        val dir = folder.newFolder(); val first = recorder(dir); first.start(context)
        first.record(reading(), pid, context); first.flush()
        val file = csv(dir); val valid = file.readText()
        file.appendText("SENSOR,1000,\"unfinished\n")
        val restarted = recorder(dir); restarted.refresh()
        val recovered = restarted.state.value.sessions.single()
        assertNull(restarted.state.value.active)
        assertEquals(RecordingEnd.INTERRUPTED, recovered.end)
        assertEquals(1L, recovered.sampleCount)
        assertEquals(valid, file.readText())
        first.stop() // Libera el descriptor del objeto que simula el proceso anterior.
    }

    @Test fun storageFailureIsReportedWithoutThrowingIntoPolling() = runBlocking {
        val file = folder.newFile(); val recorder = recorder(file)
        assertFalse(recorder.start(context))
        assertNotNull(recorder.state.value.error)
        assertNull(recorder.state.value.active)
        recorder.record(reading(), pid, context)
    }

    @Test fun serializesConcurrentSamplesAndStop() = runBlocking {
        val dir = folder.newFolder(); val recorder = recorder(dir); recorder.start(context)
        coroutineScope { repeat(100) { launch(Dispatchers.Default) { recorder.record(reading(), pid, context) } } }
        recorder.event("RECONNECTING", "Enlace perdido")
        recorder.event("CONNECTED", "Enlace recuperado")
        recorder.stop()
        recorder.record(reading(), pid, context)
        assertEquals(100L, recorder.state.value.sessions.single().sampleCount)
        assertEquals(100L, TelemetryRecorder.recoverCsv(csv(dir)))
        assertTrue(csv(dir).readText().contains(",RECONNECTING,Enlace perdido"))
        assertTrue(csv(dir).readText().contains(",CONNECTED,Enlace recuperado"))
    }

    @Test fun activeSessionsCannotBeExportedOrDeleted() = runBlocking {
        val recorder = recorder(folder.newFolder()); recorder.start(context)
        val id = recorder.state.value.active!!.id
        try { recorder.export(id, ByteArrayOutputStream()); fail("Exportó una sesión activa") } catch (_: IllegalArgumentException) { }
        try { recorder.delete(id); fail("Eliminó una sesión activa") } catch (_: IllegalArgumentException) { }
        recorder.stop()
    }

    @Test fun distinguishesHomonymousSensorsByIdAndEquation() = runBlocking {
        val dir = folder.newFolder(); val recorder = recorder(dir); recorder.start(context)
        val other = pid.copy(id = "other-temperature", equation = "B-40", header = "7E1")
        recorder.record(reading(), pid, context)
        recorder.record(reading().copy(pidId = other.id), other, context)
        recorder.stop()
        assertTrue(csv(dir).readText().contains(",temperature,\"Temperatura, motor\",0105,,A-40,"))
        assertTrue(csv(dir).readText().contains(",other-temperature,\"Temperatura, motor\",0105,7E1,B-40,"))
    }
}
