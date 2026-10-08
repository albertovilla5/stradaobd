package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import com.torqueobd2.dash.data.model.ObdPid
import com.torqueobd2.dash.data.model.PidCategory

/** Persiste identidades y definiciones; la compatibilidad se comprueba en cada conexión. */
class SensorLibraryRepository(private val prefs: SharedPreferences) {
    fun load(defaults: List<ObdPid>): List<ObdPid> {
        val saved = (0 until prefs.getInt("sensor_count", 0).coerceIn(0, 4096)).mapNotNull { index ->
            val key = "sensor_${index}_"
            val id = prefs.getString(key + "id", null)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val command = prefs.getString(key + "command", null)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val unavailable = (0 until prefs.getInt(key + "unavailable_count", 0).coerceIn(0, 256)).associate { n ->
                (prefs.getString(key + "raw_${n}_name", "") ?: "") to
                    (prefs.getString(key + "raw_${n}_values", "") ?: "").split(",").mapNotNull { it.toIntOrNull() }.toSet()
            }
            val labels = (0 until prefs.getInt(key + "label_count", 0).coerceIn(0, 256)).associate { n ->
                prefs.getInt(key + "label_${n}_value", 0) to (prefs.getString(key + "label_${n}_text", "") ?: "")
            }
            ObdPid(id = id, name = prefs.getString(key + "name", "Sensor") ?: "Sensor",
                shortName = prefs.getString(key + "short", "Sensor") ?: "Sensor", modeAndPid = command,
                equation = prefs.getString(key + "equation", "") ?: "", units = prefs.getString(key + "units", "") ?: "",
                header = prefs.getString(key + "header", "") ?: "", minValue = prefs.getFloat(key + "min", 0f),
                maxValue = prefs.getFloat(key + "max", 100f), isCustom = prefs.getBoolean(key + "custom", false),
                isEnabled = prefs.getBoolean(key + "enabled", true),
                category = PidCategory.entries.find { it.name == prefs.getString(key + "category", null) } ?: PidCategory.CUSTOM,
                availabilityEquation = prefs.getString(key + "availability", "") ?: "",
                unavailableRawValues = unavailable, valueLabels = labels)
        }.filterNot { it.cleanObdCommand().startsWith("CALC_") }.distinctBy { it.id }
        return saved + defaults.filter { default -> saved.none { it.id == default.id } }
    }

    fun save(pids: List<ObdPid>) {
        val editor = prefs.edit().putInt("sensor_count", pids.size)
        pids.forEachIndexed { index, pid ->
            val key = "sensor_${index}_"
            editor.putString(key + "id", pid.id).putString(key + "name", pid.name).putString(key + "short", pid.shortName)
                .putString(key + "command", pid.modeAndPid).putString(key + "equation", pid.equation)
                .putString(key + "units", pid.units).putString(key + "header", pid.header)
                .putFloat(key + "min", pid.minValue).putFloat(key + "max", pid.maxValue)
                .putBoolean(key + "custom", pid.isCustom).putBoolean(key + "enabled", pid.isEnabled)
                .putString(key + "category", pid.category.name).putString(key + "availability", pid.availabilityEquation)
                .putInt(key + "unavailable_count", pid.unavailableRawValues.size).putInt(key + "label_count", pid.valueLabels.size)
            pid.unavailableRawValues.entries.forEachIndexed { n, (name, values) ->
                editor.putString(key + "raw_${n}_name", name).putString(key + "raw_${n}_values", values.sorted().joinToString(","))
            }
            pid.valueLabels.entries.forEachIndexed { n, (value, label) ->
                editor.putInt(key + "label_${n}_value", value).putString(key + "label_${n}_text", label)
            }
        }
        editor.apply()
    }
}
