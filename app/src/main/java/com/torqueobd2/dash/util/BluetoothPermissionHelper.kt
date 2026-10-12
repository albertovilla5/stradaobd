package com.torqueobd2.dash.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permisos para listar y conectar dispositivos ya emparejados, desde Android 6.
 * La ubicación no es necesaria porque el emparejamiento se hace en Ajustes.
 */
object BluetoothPermissionHelper {

    /**
     * Devuelve la lista de permisos requeridos por el sistema según la versión de Android del dispositivo.
     */
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Permisos normales, concedidos al instalar. No se inicia un escaneo.
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
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

    fun getScanPermissions(): List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        getRequiredPermissions() + listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasScanPermissions(context: Context): Boolean = getScanPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * En Android 14+ (API 34+), iniciar un Foreground Service con tipo connectedDevice
     * requiere tener concedido al menos un permiso de dispositivo conectado en tiempo de ejecución
     * (BLUETOOTH_CONNECT o BLUETOOTH_SCAN). En Android 6–13 basta con FOREGROUND_SERVICE.
     */
    fun canStartConnectedDeviceService(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * En Android anterior a 13 intenta encender Bluetooth directamente.
     * Si devuelve false, la UI debe solicitar la activación con ACTION_REQUEST_ENABLE.
     */
    @Suppress("DEPRECATION")
    fun enableBluetoothIfAllowed(context: Context): Boolean {
        if (!hasAllPermissions(context)) return false
        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                ?: return false
            when (adapter.state) {
                BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_TURNING_ON -> true
                else -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && adapter.enable()
            }
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Comprueba si el hardware Bluetooth está soportado y si está actualmente encendido.
     */
    @Suppress("DEPRECATION")
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
    @Suppress("DEPRECATION")
    fun isBluetoothSupported(): Boolean {
        return try {
            BluetoothAdapter.getDefaultAdapter() != null
        } catch (_: Exception) {
            false
        }
    }
}
