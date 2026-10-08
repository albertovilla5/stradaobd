package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.SensorAlertRule

/** Las reglas sobreviven al reinicio; el estado activo y los temporizadores no se guardan. */
class SensorAlertRepository(private val prefs: SharedPreferences) {
    fun load(): List<SensorAlertRule> = (0 until prefs.getInt("count", 0).coerceIn(0, 1000)).mapNotNull { index ->
        val key = "rule_${index}_"
        val lower = prefs.getString(key + "lower", null)
        val upper = prefs.getString(key + "upper", null)
        if ((lower != null && lower.toDoubleOrNull() == null) ||
            (upper != null && upper.toDoubleOrNull() == null)) return@mapNotNull null
        val rule = SensorAlertRule(
            id = prefs.getString(key + "id", "") ?: "",
            vehicleId = prefs.getString(key + "vehicle", "") ?: "",
            pidId = prefs.getString(key + "pid", "") ?: "",
            sensorName = prefs.getString(key + "name", "Sensor") ?: "Sensor",
            units = prefs.getString(key + "units", "") ?: "",
            lowerLimit = lower?.toDoubleOrNull(),
            upperLimit = upper?.toDoubleOrNull(),
            durationSeconds = prefs.getInt(key + "duration", 3),
            enabled = prefs.getBoolean(key + "enabled", true)
        )
        rule.takeIf { it.validationError() == null }
    }.distinctBy { it.id }

    fun save(rules: List<SensorAlertRule>) {
        require(rules.size <= 1000 && rules.all { it.validationError() == null })
        val editor = prefs.edit().clear().putInt("count", rules.size)
        rules.forEachIndexed { index, rule ->
            val key = "rule_${index}_"
            editor.putString(key + "id", rule.id).putString(key + "vehicle", rule.vehicleId)
                .putString(key + "pid", rule.pidId).putString(key + "name", rule.sensorName)
                .putString(key + "units", rule.units).putString(key + "lower", rule.lowerLimit?.toString())
                .putString(key + "upper", rule.upperLimit?.toString()).putInt(key + "duration", rule.durationSeconds)
                .putBoolean(key + "enabled", rule.enabled)
        }
        editor.apply()
    }
}
