package com.torqueobd2.dash.data.model

import org.junit.Assert.*
import org.junit.Test

class VehicleProfileDraftTest {
    @Test fun freshDraftHasNoInventedVehicleAndCannotBeSaved() {
        val draft = VehicleProfileDraft()
        assertEquals("", draft.value(VehicleField.NAME))
        assertEquals("", draft.value(VehicleField.DISPLACEMENT))
        assertNull(draft.fuel)
        assertEquals(VehicleField.NAME, draft.firstInvalidField())
        assertThrows(IllegalArgumentException::class.java) { draft.toProfile("new") }
    }
    @Test fun editingRoundTripKeepsAllFieldsIncludingAdvancedSettingsAndIdentity() {
        val profile = VehicleProfile(id = "real-car", name = "Mi coche", modelYear = "2019", fuelType = FuelType.GASOLINE,
            engineDisplacementLiters = 1.5f, fuelTankCapacityLiters = 45f, vehicleWeightKg = 1230f,
            maxPowerHp = 130f, maxTorqueNm = 250f, maxRpm = 6500f, volumetricEfficiencyPercent = 90f,
            boostPressureAdjustmentBar = -.2f, customInitCommands = "AT SP0\nAT SH 7E0", hideUnsupportedSensors = false)
        assertEquals(profile, VehicleProfileDraft.from(profile).toProfile(profile.id))
    }
    @Test fun decimalCommaIsAcceptedButNonFiniteAndNonPositiveSpecificationValuesAreRejected() {
        val draft = VehicleProfileDraft.from(VehicleProfile()).set(VehicleField.DISPLACEMENT, "1,6")
        assertEquals(1.6f, draft.toProfile("one").engineDisplacementLiters, .001f)
        for (field in listOf(VehicleField.DISPLACEMENT, VehicleField.TANK, VehicleField.WEIGHT,
            VehicleField.POWER, VehicleField.TORQUE, VehicleField.RPM)) {
            for (input in listOf("", "0", "-1", "NaN", "Infinity", "abc")) assertNotNull(draft.set(field, input).error(field))
        }
        assertNull(draft.set(VehicleField.BOOST, "-0,3").error(VehicleField.BOOST))
        assertNotNull(draft.set(VehicleField.EFFICIENCY, "101").error(VehicleField.EFFICIENCY))
    }
    @Test fun everyRequiredFieldIsValidatedAndOptionalInitCanRemainAutomatic() {
        val valid = VehicleProfileDraft.from(VehicleProfile()).set(VehicleField.INIT, "")
        assertNull(valid.firstInvalidField())
        assertEquals("", valid.toProfile("auto").customInitCommands)
        for (field in VehicleField.entries.filter { it.numeric || it == VehicleField.NAME })
            assertNotNull("Required: $field", valid.set(field, "").error(field))
        assertNotNull(valid.copy(fuel = null).error(VehicleField.FUEL))
        assertNotNull(valid.set(VehicleField.YEAR, "22").error(VehicleField.YEAR))
    }
}
