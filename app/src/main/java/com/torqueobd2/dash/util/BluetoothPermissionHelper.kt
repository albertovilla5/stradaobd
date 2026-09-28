package com.torqueobd2.dash.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utilidad para comprobar y gestionar los permisos de Bluetooth y localización
 * en todas las versiones de Android desde API 29 (Android 10) hasta API 35 (Android 15).
 */
object BluetoothPermissionHelper {

    /**
     * Devuelve la lista de permisos requeridos por el sistema según la versión de Android del dispositivo.
     */
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) en adelante requiere permisos cercanos específicos
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            // Android 10 y 11 (API 29-30) requieren permisos clásicos y localización para escaneo Bluetooth
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Comprueba si todos los permisos necesarios están concedidos.
     */
    fun hasAllPermissions(context: Context): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Comprueba si el hardware Bluetooth está soportado y si está actualmente encendido.
     */
    fun isBluetoothEnabled(): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter != null && adapter.isEnabled
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Comprueba si el dispositivo tiene adaptador Bluetooth disponible en hardware.
     */
    fun isBluetoothSupported(): Boolean {
        return try {
            BluetoothAdapter.getDefaultAdapter() != null
        } catch (_: Exception) {
            false
        }
    }
}
