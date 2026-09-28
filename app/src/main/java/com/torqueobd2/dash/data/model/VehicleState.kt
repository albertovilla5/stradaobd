package com.torqueobd2.dash.data.model

/**
 * Estado general del vehículo en un instante dado, conteniendo los parámetros principales
 * del cuadro de instrumentos, variables derivadas del perfil del coche y telemetría.
 */
data class VehicleState(
    // Parámetros principales del motor
    val rpm: Float = 0f,
    val speedKmh: Float = 0f,
    val coolantTempC: Float = 20f,
    val intakeAirTempC: Float = 22f,
    val engineLoadPercent: Float = 0f,
    val throttlePercent: Float = 0f,
    val boostPressureBar: Float = 0f, // Presión turbo relativa (ej. 0.0 - 2.5 bar)
    val fuelLevelPercent: Float = 65f,
    val mafFlowGramsSec: Float = 0f,
    val fuelConsumptionL100km: Float = 0f,
    val fuelRateLh: Float = 0f,
    val batteryVoltage: Float = 12.6f,
    val gearEstimated: Int = 1,

    // Parámetros calculados basados en el perfil del vehículo (Torque Pro physics)
    val estimatedHorsepowerCv: Float = 0f,   // Potencia estimada en CV / HP
    val estimatedTorqueNm: Float = 0f,        // Par motor estimado en Nm
    val fuelRemainingLiters: Float = 32.5f,   // Litros restantes en el depósito
    val estimatedRangeKm: Float = 580f,       // Autonomía restante estimada

    // Parámetros específicos DPF / Diésel (Honda i-DTEC y comunes)
    val dpfDistanceLastRegenKm: Float = 342f,
    val dpfFuelLastRegenL: Float = 22.4f,
    val dpfTimeLastRegenSec: Float = 18600f,
    val dpfDifferentialPressureKpa: Float = 1.2f,
    val exhaustGasTemp1C: Float = 310f,
    val dpfCalculatedPmGrams: Float = 14.5f,
    val dpfRegenStatus: Long = 0L,
    val dpfActiveRegen: Boolean = false,
    val engineOilLevelMm: Float = 98f,
    val dpfExhaustFlowM3h: Float = 140f,

    // Luces de advertencia automotriz
    val checkEngineMil: Boolean = false,
    val dpfWarningLight: Boolean = false,
    val oilWarningLight: Boolean = false,

    // Mapa de todas las lecturas de PIDs activos (tanto estándar como personalizados del CSV)
    val readings: Map<String, ObdReading> = emptyMap(),

    // Histórico de puntos para gráficos en tiempo real (clave = PID shortName, valor = lista de puntos)
    val sparklineHistory: Map<String, List<Float>> = emptyMap()
)
