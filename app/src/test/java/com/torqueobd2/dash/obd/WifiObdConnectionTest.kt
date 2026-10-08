package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

class WifiObdConnectionTest {
    private fun withAdapter(
        response: (String) -> String?,
        fragmentSize: Int = Int.MAX_VALUE,
        test: suspend (Int) -> Unit
    ) = runBlocking {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val worker = thread(isDaemon = true) {
                try {
                    server.accept().use { client ->
                        val reader = client.getInputStream().bufferedReader(Charsets.US_ASCII)
                        val output = client.getOutputStream()
                        while (true) {
                            val command = reader.readLine() ?: break
                            val reply = response(command) ?: continue
                            val bytes = reply.toByteArray(Charsets.US_ASCII)
                            var offset = 0
                            while (offset < bytes.size) {
                                val count = minOf(fragmentSize, bytes.size - offset)
                                output.write(bytes, offset, count)
                                output.flush()
                                offset += count
                                if (offset < bytes.size) Thread.sleep(3)
                            }
                        }
                    }
                } catch (_: IOException) {
                    // The connection is intentionally closed by several failure tests.
                }
            }
            try {
                test(server.localPort)
            } finally {
                server.close()
                worker.join(1000)
            }
        }
    }

    @Test
    fun initializesAdapterBeforeCustomCommandsAndReadsTelemetry() {
        val commands = ConcurrentLinkedQueue<String>()
        withAdapter({ command ->
            commands.add(command)
            if (command == "010C") "41 0C 1A F8\r>" else "OK\r>"
        }) { port ->
            val connection = WifiObdConnection("127.0.0.1", port, listOf("AT SH 7E0"))
            try {
                assertTrue(connection.connect())
                assertEquals("41 0C 1A F8\r>", connection.sendCommand("010C"))
                assertEquals(
                    listOf("AT Z", "AT E0", "AT L0", "AT S0", "AT AT1", "AT SP0", "AT SH 7E0", "010C"),
                    commands.toList()
                )
            } finally {
                connection.disconnect()
            }
            assertFalse(connection.isConnected())
        }
    }

    @Test
    fun silentAdapterIsAnErrorInsteadOfFalseSuccess() {
        withAdapter({ null }) { port ->
            val connection = WifiObdConnection("127.0.0.1", port)
            assertFalse(connection.connect())
            assertEquals(ConnectionStatus.ERROR, connection.status.value)
            assertTrue(connection.statusMessage.value.contains("AT Z"))
            assertFalse(connection.isConnected())
        }
    }

    @Test
    fun rejectedInitializationCommandIsReported() {
        withAdapter({ "?\r>" }) { port ->
            val connection = WifiObdConnection("127.0.0.1", port)
            assertFalse(connection.connect())
            assertTrue(connection.statusMessage.value.contains("rechazó"))
            assertFalse(connection.isConnected())
        }
    }

    @Test
    fun concurrentTerminalAndPollingCommandsKeepTheirOwnResponses() {
        withAdapter({ command ->
            if (command.startsWith("AT")) "OK\r>" else "$command\r>"
        }) { port ->
            val connection = WifiObdConnection("127.0.0.1", port)
            try {
                assertTrue(connection.connect())
                kotlinx.coroutines.coroutineScope {
                    val rpm = async { connection.sendCommand("010C") }
                    val speed = async { connection.sendCommand("010D") }
                    assertEquals("010C\r>", rpm.await())
                    assertEquals("010D\r>", speed.await())
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    @Test
    fun waitsForPromptAndKeepsEveryFragmentOfMultilineDpfResponse() {
        val raw = "0: 62 25 12 01 02\r1: 03 04 05 06 07\r>"
        withAdapter({ command -> if (command == "222512") raw else "OK\r>" }, fragmentSize = 3) { port ->
            val connection = WifiObdConnection("127.0.0.1", port)
            try {
                assertTrue(connection.connect())
                val response = connection.sendCommand("222512")
                assertEquals(raw, response)
                assertArrayEquals(intArrayOf(1, 2, 3, 4, 5, 6, 7), Elm327Protocol.extractDataBytes("222512", response))
                assertEquals("OK\r>", connection.sendCommand("ATI"))
            } finally { connection.disconnect() }
        }
    }

    @Test
    fun invalidPortDoesNotCrash() = runBlocking {
        val connection = WifiObdConnection("127.0.0.1", 70000)
        assertFalse(connection.connect())
        assertEquals(ConnectionStatus.ERROR, connection.status.value)
        assertTrue(connection.statusMessage.value.contains("65535"))
    }
}
