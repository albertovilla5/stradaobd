package com.torqueobd2.dash.data.repository

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test

class VehicleProfileRepositoryTest {
    @Test fun readingFreshStorageNeverCreatesOrPersistsASampleVehicle() {
        val values = mutableMapOf<String, Any?>()
        val preferences = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "contains" -> values.containsKey(args!![0])
                "getString" -> values[args!![0]] ?: args[1]
                "edit" -> error("An empty garage must not write a sample profile")
                else -> error(method.name)
            }
        } as SharedPreferences
        val repository = VehicleProfileRepository(preferences)
        assertTrue(repository.getAllVehicles().isEmpty())
        assertEquals("", repository.getActiveVehicleId())
        assertEquals("Sin vehículo", repository.getActiveVehicle().name)
        assertEquals("", repository.getActiveVehicle().customInitCommands)
        assertTrue(values.isEmpty())
    }
}
