package com.torqueobd2.dash.data.model

/** Lecturas, históricos y motivos de ausencia se identifican siempre por PID ID. */
data class VehicleState(
    val readings: Map<String, ObdReading> = emptyMap(),
    val sparklineHistory: Map<String, List<Float>> = emptyMap(),
    val sensorStatuses: Map<String, SensorTelemetryStatus> = emptyMap()
) {
    private fun value(id: String): Float = readings[id]?.value ?: Float.NaN
    val rpm get() = value(SensorIds.RPM)
    val speedKmh get() = value(SensorIds.SPEED)
    val coolantTempC get() = value(SensorIds.COOLANT)
    val batteryVoltage get() = value(SensorIds.VOLTAGE)
    val intakeAirTempC get() = value("SAE_010F_Temp Adm")
    val engineLoadPercent get() = value("SAE_0104_Carga")
    val throttlePercent get() = value("SAE_0111_Acelerador")
    val manifoldPressureKpa get() = value("SAE_010B_MAP")
    val fuelLevelPercent get() = value("SAE_012F_Nivel Dep")
    val mafFlowGramsSec get() = value("SAE_0110_MAF")
    val fuelRateLh get() = value("SAE_015E_Consumo")

    // Potencia/par se seleccionan explícitamente por ID en el cuadro personalizado.
    val horsepowerCv: Float get() = Float.NaN
    val torqueNm: Float get() = Float.NaN
    val dpfDistanceLastRegenKm get() = value("HONDA_DPF dist a/reg")
    val dpfFuelLastRegenL get() = value("HONDA_DPF fuel a/reg")
    val dpfTimeLastRegenSec get() = value("HONDA_DPF time a/reg")
    val dpfDifferentialPressureKpa get() = value("HONDA_DPF diff.pres")
    val exhaustGasTemp1C get() = value("HONDA_EGT")
    val dpfCalculatedPmGrams get() = value("HONDA_DPF calc PM")
    val engineOilLevelMm get() = value("HONDA_Eng.oil lvl f")
    val dpfExhaustFlowM3h get() = value("HONDA_DPF calc.flow")
}
