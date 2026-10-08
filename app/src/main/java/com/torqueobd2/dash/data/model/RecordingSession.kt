package com.torqueobd2.dash.data.model

data class RecordingContext(
    val vehicleId: String,
    val vehicleName: String,
    val connectionType: ConnectionType,
    val adapterName: String,
    val adapterAddress: String
)

enum class RecordingEnd(val label: String) {
    COMPLETED("Finalizada"), INTERRUPTED("Interrumpida"), ERROR("Error")
}

data class RecordingSession(
    val id: String,
    val context: RecordingContext,
    val startedAtMillis: Long,
    val endedAtMillis: Long = 0,
    val sampleCount: Long = 0,
    val bytes: Long = 0,
    val end: RecordingEnd? = null,
    val reason: String = ""
) {
    val isSimulated get() = context.connectionType == ConnectionType.SIMULATION
}

data class RecordingState(
    val active: RecordingSession? = null,
    val sessions: List<RecordingSession> = emptyList(),
    val error: String? = null
)
