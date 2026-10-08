package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.SensorAlertRule
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class SensorAlertRepositoryTest {
    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when {
                method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }
                method.name == "clear" -> { values.clear(); proxy }
                method.name == "apply" -> null
                else -> error(method.name)
            }
        }
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getInt", "getBoolean" -> values[args!![0]] ?: args[1]
                "edit" -> editor
                else -> error(method.name)
            }
        } as SharedPreferences
    }

    @Test fun rulesSurviveReopeningWithVehiclesPrecisionAndEnablement() {
        val prefs = preferences()
        assertTrue(SensorAlertRepository(prefs).load().isEmpty())
        val first = SensorAlertRule(vehicleId = "car1", pidId = "voltage", sensorName = "Voltaje", units = "V",
            lowerLimit = 12.123456789, upperLimit = 15.0, durationSeconds = 8)
        val second = first.copy(id = "another", vehicleId = "car2", lowerLimit = null, enabled = false, durationSeconds = 0)
        SensorAlertRepository(prefs).save(listOf(first, second))
        assertEquals(listOf(first, second), SensorAlertRepository(prefs).load())
        SensorAlertRepository(prefs).save(listOf(second.copy(upperLimit = 14.5)))
        assertEquals(listOf(second.copy(upperLimit = 14.5)), SensorAlertRepository(prefs).load())
        SensorAlertRepository(prefs).save(emptyList())
        assertTrue(SensorAlertRepository(prefs).load().isEmpty())
    }

    @Test fun invalidPersistedRulesAreNotEnabled() {
        val prefs = preferences()
        val rule = SensorAlertRule(vehicleId = "car", pidId = "voltage", sensorName = "Voltaje", units = "V", upperLimit = 15.0)
        SensorAlertRepository(prefs).save(listOf(rule))
        prefs.edit().putString("rule_0_upper", "NaN").apply()
        assertTrue(SensorAlertRepository(prefs).load().isEmpty())
        SensorAlertRepository(prefs).save(listOf(rule.copy(lowerLimit = 10.0)))
        prefs.edit().putString("rule_0_lower", "texto").apply()
        assertTrue(SensorAlertRepository(prefs).load().isEmpty())
    }
}
