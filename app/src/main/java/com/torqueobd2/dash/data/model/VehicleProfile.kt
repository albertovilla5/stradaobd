package com.torqueobd2.dash.data.model

import org.json.JSONObject
import java.util.UUID

enum class FuelType(val displayName: String, val stoichiometricRatio: Float, val densityGramsPerLiter: Float) {
    DIESEL("Diésel", 14.5f, 832f),
    GASOLINE("Gasolina (E5/E10)", 14.7f, 745f),
    LPG("GLP / Autogas", 15.5f, 540f),
    HYBRID("Híbrido (Gasolina/Eléctrico)", 14.7f, 745f)
}

/**
 * Configuración y ficha del vehículo. Estos valores no generan lecturas de sensores.
 */
data class VehicleProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Honda Civic 2.2 i-DTEC",
    val modelYear: String = "2012",
    val engineDisplacementLiters: Float = 2.2f, // Cilindrada en litros
    val fuelTankCapacityLiters: Float = 50.0f,  // Capacidad del depósito
    val vehicleWeightKg: Float = 1450f,        // Peso en vacío del coche
    val fuelType: FuelType = FuelType.DIESEL,
    val maxRpm: Float = 5000f,                 // Régimen máximo del motor (redline)
    val maxPowerHp: Float = 150f,              // Potencia de ficha (CV)
    val maxTorqueNm: Float = 350f,             // Par de ficha (Nm)
    val volumetricEfficiencyPercent: Float = 85f, // Eficiencia volumétrica del motor
    val customInitCommands: String = "AT SP7\nAT SH DA10F1",
    val hideUnsupportedSensors: Boolean = true, // Ocultar sensores no disponibles en la ECU
    val boostPressureAdjustmentBar: Float = 0.0f // Calibración de presión MAP/Boost
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("modelYear", modelYear)
        json.put("engineDisplacementLiters", engineDisplacementLiters.toDouble())
        json.put("fuelTankCapacityLiters", fuelTankCapacityLiters.toDouble())
        json.put("vehicleWeightKg", vehicleWeightKg.toDouble())
        json.put("fuelType", fuelType.name)
        json.put("maxRpm", maxRpm.toDouble())
        json.put("maxPowerHp", maxPowerHp.toDouble())
        json.put("maxTorqueNm", maxTorqueNm.toDouble())
        json.put("volumetricEfficiencyPercent", volumetricEfficiencyPercent.toDouble())
        json.put("customInitCommands", customInitCommands)
        json.put("hideUnsupportedSensors", hideUnsupportedSensors)
        json.put("boostPressureAdjustmentBar", boostPressureAdjustmentBar.toDouble())
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): VehicleProfile {
            val fuelType = try {
                FuelType.valueOf(json.optString("fuelType", FuelType.DIESEL.name))
            } catch (_: Exception) {
                FuelType.DIESEL
            }
            return VehicleProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Vehículo"),
                modelYear = json.optString("modelYear", "2015"),
                engineDisplacementLiters = json.optDouble("engineDisplacementLiters", 2.0).toFloat(),
                fuelTankCapacityLiters = json.optDouble("fuelTankCapacityLiters", 50.0).toFloat(),
                vehicleWeightKg = json.optDouble("vehicleWeightKg", 1400.0).toFloat(),
                fuelType = fuelType,
                maxRpm = json.optDouble("maxRpm", 5000.0).toFloat(),
                maxPowerHp = json.optDouble("maxPowerHp", 150.0).toFloat(),
                maxTorqueNm = json.optDouble("maxTorqueNm", 350.0).toFloat(),
                volumetricEfficiencyPercent = json.optDouble("volumetricEfficiencyPercent", 85.0).toFloat(),
                customInitCommands = json.optString("customInitCommands", ""),
                hideUnsupportedSensors = json.optBoolean("hideUnsupportedSensors", true),
                boostPressureAdjustmentBar = json.optDouble("boostPressureAdjustmentBar", 0.0).toFloat()
            )
        }
    }
}
