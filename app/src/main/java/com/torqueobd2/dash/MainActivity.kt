package com.torqueobd2.dash

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.torqueobd2.dash.ui.MainScreen
import com.torqueobd2.dash.ui.theme.TorqueDashboardTheme
import com.torqueobd2.dash.viewmodel.ObdDashboardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ObdDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantener la pantalla encendida mientras la app está activa (indispensable en el coche)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        enableEdgeToEdge()

        setContent {
            TorqueDashboardTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
