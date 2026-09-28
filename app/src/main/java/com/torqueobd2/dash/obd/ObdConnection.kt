package com.torqueobd2.dash.obd

import com.torqueobd2.dash.data.model.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

interface ObdConnection {
    val status: StateFlow<ConnectionStatus>
    val statusMessage: StateFlow<String>

    suspend fun connect(): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(command: String): String
    fun isConnected(): Boolean
}
