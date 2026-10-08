package com.torqueobd2.dash

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.torqueobd2.dash.data.repository.InterfacePreferences
import androidx.activity.viewModels
import com.torqueobd2.dash.ui.MainScreen
import com.torqueobd2.dash.ui.theme.TorqueDashboardTheme
import com.torqueobd2.dash.viewmodel.ObdDashboardViewModel

/**
 * Actividad principal de Torque OBD2 Dash.
 * Comparte la conexión con el servicio de lectura en segundo plano
 * y protección de salida accidental mediante doble toque en el botón Atrás para seguridad al volante.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ObdDashboardViewModel by viewModels()
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener la pantalla encendida mientras la app está activa (imprescindible en el coche)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Protección contra pulsación accidental del botón atrás conduciendo
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(
                        this@MainActivity,
                        "Pulsa de nuevo para salir de Torque OBD2",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val preferences = remember { InterfacePreferences(this@MainActivity) }
            var carMode by remember { mutableStateOf(preferences.carMode) }
            TorqueDashboardTheme(carMode = carMode, onCarModeChanged = {
                preferences.carMode = it
                carMode = it
            }) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setUiVisible(this, true)
    }

    override fun onStop() {
        viewModel.setUiVisible(this, false)
        super.onStop()
    }
}
