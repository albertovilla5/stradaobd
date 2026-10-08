package com.torqueobd2.dash.data.model

/** Un borrador no es un vehículo guardado y no asigna datos de un coche de ejemplo. */
enum class VehicleField(val title: String, val unit: String = "", val step: Float = 1f) {
    NAME("Nombre del vehículo"), YEAR("Año", step = 1f), FUEL("Combustible"),
    DISPLACEMENT("Cilindrada", "L", .1f), TANK("Capacidad del depósito", "L", 5f),
    WEIGHT("Peso del vehículo", "kg", 50f), POWER("Potencia máxima", "CV", 5f),
    TORQUE("Par máximo", "Nm", 10f), RPM("Régimen máximo", "RPM", 100f),
    EFFICIENCY("Eficiencia volumétrica", "%", 1f), BOOST("Ajuste de presión", "bar", .1f),
    INIT("Comandos de inicio"), OPTIONS("Sensores"), REVIEW("Revisar y guardar");
    val numeric get() = this in listOf(YEAR, DISPLACEMENT, TANK, WEIGHT, POWER, TORQUE, RPM, EFFICIENCY, BOOST)
}

data class VehicleProfileDraft(val fields: Map<VehicleField, String> = mapOf(
    VehicleField.EFFICIENCY to "85", VehicleField.BOOST to "0"),
    val fuel: FuelType? = null, val hideUnsupported: Boolean = true) {
    fun value(field: VehicleField) = fields[field].orEmpty()
    fun set(field: VehicleField, value: String) = copy(fields = fields + (field to value))
    fun number(field: VehicleField) = value(field).trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
    fun error(field: VehicleField): String? = when (field) {
        VehicleField.NAME -> if (value(field).isBlank()) "Escribe el nombre de tu vehículo." else null
        VehicleField.FUEL -> if (fuel == null) "Elige el combustible." else null
        VehicleField.YEAR -> if (value(field).toIntOrNull() !in 1900..2100) "Introduce un año entre 1900 y 2100." else null
        VehicleField.EFFICIENCY -> if (number(field)?.let { it > 0f && it <= 100f } != true) "Introduce una eficiencia mayor que 0 y hasta 100 %." else null
        VehicleField.BOOST -> if (number(field) == null) "Introduce un ajuste válido en bar; puede ser 0." else null
        else -> if (field.numeric && number(field)?.let { it > 0f } != true) "Introduce un valor mayor que 0 en ${field.unit}." else null
    }
    fun firstInvalidField() = VehicleField.entries.firstOrNull { error(it) != null }
    fun toProfile(id: String): VehicleProfile {
        require(firstInvalidField() == null) { "Completa la ficha antes de guardar." }
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
