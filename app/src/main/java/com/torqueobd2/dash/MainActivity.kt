package com.torqueobd2.dash

import android.content.res.Configuration
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
import com.torqueobd2.dash.i18n.AppI18n
import com.torqueobd2.dash.i18n.tr
import com.torqueobd2.dash.ui.MainScreen
import com.torqueobd2.dash.ui.theme.TorqueDashboardTheme
import com.torqueobd2.dash.viewmodel.ObdDashboardViewModel

/**
 * Actividad principal de StradaOBD.
 * Comparte la conexión con el servicio de lectura en segundo plano
 * y protección de salida accidental mediante doble toque en el botón Atrás para seguridad al volante.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ObdDashboardViewModel by viewModels()
    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        AppI18n.syncWithContext(this)
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
                        tr("Pulsa de nuevo para salir de StradaOBD", "Press back again to exit StradaOBD"),
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
            val languagePreference = AppI18n.currentPreference
            TorqueDashboardTheme(
                carMode = carMode,
                onCarModeChanged = {
                    preferences.carMode = it
                    carMode = it
                },
                languagePreference = languagePreference,
                onLanguagePreferenceChanged = { newPref ->
                    AppI18n.applyPreference(this@MainActivity, newPref)
                }
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppI18n.syncWithContext(this)
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
