package com.torqueobd2.dash.data.model

enum class FuelType(val displayName: String, val stoichiometricRatio: Float, val densityGramsPerLiter: Float) {
    DIESEL("Diésel", 14.5f, 832f),
    GASOLINE("Gasolina (E5/E10)", 14.7f, 745f),
    LPG("GLP / Autogas", 15.5f, 540f),
    HYBRID("Híbrido (Gasolina/Eléctrico)", 14.7f, 745f)
}

/**
 * Perfil completo del vehículo con los datos requeridos para los cálculos de consumo,
 * potencia, autonomía y telemetría exacta igual que en Torque Pro.
 */
data class VehicleProfile(
    val name: String = "Honda Civic 2.2 i-DTEC",
    val modelYear: String = "2012",
    val engineDisplacementLiters: Float = 2.2f, // Cilindrada en litros
    val fuelTankCapacityLiters: Float = 50.0f,  // Capacidad del depósito
    val vehicleWeightKg: Float = 1450f,        // Peso en vacío del coche
    val fuelType: FuelType = FuelType.DIESEL,
    val maxRpm: Float = 5000f,                 // Régimen máximo del motor (redline)
    val volumetricEfficiencyPercent: Float = 85f, // Eficiencia volumétrica del motor
    val customInitCommands: String = "AT SP7\nAT SH DA10F1",
    val hideUnsupportedSensors: Boolean = true, // Ocultar sensores no disponibles en la ECU
    val boostPressureAdjustmentBar: Float = 0.0f // Calibración de presión MAP/Boost
)
