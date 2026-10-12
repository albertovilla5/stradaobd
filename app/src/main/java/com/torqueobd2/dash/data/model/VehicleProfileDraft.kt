package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.tr

/** Un borrador no es un vehículo guardado y no asigna datos de un coche de ejemplo. */
enum class VehicleField(private val baseUnit: String = "", val step: Float = 1f) {
    NAME,
    YEAR(step = 1f),
    FUEL,
    DISPLACEMENT("L", .1f),
    TANK("L", 5f),
    WEIGHT("kg", 50f),
    POWER("CV", 5f),
    TORQUE("Nm", 10f),
    RPM("RPM", 100f),
    EFFICIENCY("%", 1f),
    BOOST("bar", .1f),
    INIT,
    OPTIONS,
    REVIEW;

    val title: String
        get() = when (this) {
            NAME -> tr("Nombre del vehículo", "Vehicle name")
            YEAR -> tr("Año", "Year")
            FUEL -> tr("Combustible", "Fuel")
            DISPLACEMENT -> tr("Cilindrada", "Displacement")
            TANK -> tr("Capacidad del depósito", "Tank capacity")
            WEIGHT -> tr("Peso del vehículo", "Vehicle weight")
            POWER -> tr("Potencia máxima", "Max power")
            TORQUE -> tr("Par máximo", "Max torque")
            RPM -> tr("Régimen máximo", "Max RPM")
            EFFICIENCY -> tr("Eficiencia volumétrica", "Volumetric efficiency")
            BOOST -> tr("Ajuste de presión", "Boost adjustment")
            INIT -> tr("Comandos de inicio", "Init commands")
            OPTIONS -> tr("Sensores", "Sensors")
            REVIEW -> tr("Revisar y guardar", "Review and save")
        }

    val unit: String
        get() = when (this) {
            POWER -> tr("CV", "HP")
            else -> baseUnit
        }

    val numeric get() = this in listOf(YEAR, DISPLACEMENT, TANK, WEIGHT, POWER, TORQUE, RPM, EFFICIENCY, BOOST)
}

data class VehicleProfileDraft(val fields: Map<VehicleField, String> = mapOf(
    VehicleField.EFFICIENCY to "85", VehicleField.BOOST to "0"),
    val fuel: FuelType? = null, val hideUnsupported: Boolean = true) {
    fun value(field: VehicleField) = fields[field].orEmpty()
    fun set(field: VehicleField, value: String) = copy(fields = fields + (field to value))
    fun number(field: VehicleField) = value(field).trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
    fun error(field: VehicleField): String? = when (field) {
        VehicleField.NAME -> if (value(field).isBlank()) tr("Escribe el nombre de tu vehículo.", "Enter your vehicle's name.") else null
        VehicleField.FUEL -> if (fuel == null) tr("Elige el combustible.", "Choose the fuel type.") else null
        VehicleField.YEAR -> if (value(field).toIntOrNull() !in 1900..2100) tr("Introduce un año entre 1900 y 2100.", "Enter a year between 1900 and 2100.") else null
        VehicleField.EFFICIENCY -> if (number(field)?.let { it > 0f && it <= 100f } != true) tr("Introduce una eficiencia mayor que 0 y hasta 100 %.", "Enter an efficiency greater than 0 and up to 100%.") else null
        VehicleField.BOOST -> if (number(field) == null) tr("Introduce un ajuste válido en bar; puede ser 0.", "Enter a valid adjustment in bar; can be 0.") else null
        else -> if (field.numeric && number(field)?.let { it > 0f } != true) tr("Introduce un valor mayor que 0 en ${field.unit}.", "Enter a value greater than 0 in ${field.unit}.") else null
    }
    fun firstInvalidField() = VehicleField.entries.firstOrNull { error(it) != null }
    fun toProfile(id: String): VehicleProfile {
        require(firstInvalidField() == null) { tr("Completa la ficha antes de guardar.", "Complete the profile before saving.") }
        return VehicleProfile(id = id, name = value(VehicleField.NAME).trim(), modelYear = value(VehicleField.YEAR).trim(),
            engineDisplacementLiters = number(VehicleField.DISPLACEMENT)!!, fuelTankCapacityLiters = number(VehicleField.TANK)!!,
            vehicleWeightKg = number(VehicleField.WEIGHT)!!, maxPowerHp = number(VehicleField.POWER)!!,
            maxTorqueNm = number(VehicleField.TORQUE)!!, maxRpm = number(VehicleField.RPM)!!,
            fuelType = fuel!!, volumetricEfficiencyPercent = number(VehicleField.EFFICIENCY)!!,
            boostPressureAdjustmentBar = number(VehicleField.BOOST)!!, customInitCommands = value(VehicleField.INIT).trim(),
            hideUnsupportedSensors = hideUnsupported)
    }
    companion object {
        fun from(profile: VehicleProfile) = VehicleProfileDraft(mapOf(
            VehicleField.NAME to profile.name, VehicleField.YEAR to profile.modelYear,
            VehicleField.DISPLACEMENT to profile.engineDisplacementLiters.toString(),
            VehicleField.TANK to profile.fuelTankCapacityLiters.toString(), VehicleField.WEIGHT to profile.vehicleWeightKg.toString(),
            VehicleField.POWER to profile.maxPowerHp.toString(), VehicleField.TORQUE to profile.maxTorqueNm.toString(),
            VehicleField.RPM to profile.maxRpm.toString(), VehicleField.EFFICIENCY to profile.volumetricEfficiencyPercent.toString(),
            VehicleField.BOOST to profile.boostPressureAdjustmentBar.toString(), VehicleField.INIT to profile.customInitCommands
        ), profile.fuelType, profile.hideUnsupportedSensors)
    }
}
