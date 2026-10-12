package com.torqueobd2.dash.data.model

import com.torqueobd2.dash.i18n.tr

enum class PollingMode {
    VISIBLE,
    BALANCED,
    ALL;

    val title: String
        get() = when (this) {
            VISIBLE -> tr("Solo pantalla", "Screen only")
            BALANCED -> tr("Equilibrado", "Balanced")
            ALL -> tr("Todos los habilitados", "All enabled")
        }

    val description: String
        get() = when (this) {
            VISIBLE -> tr(
                "Consulta los indicadores visibles y los sensores con avisos activados, aunque no estén en pantalla.",
                "Polls visible gauges and sensors with active alerts, even when off-screen."
            )
            BALANCED -> tr(
                "Prioriza la pantalla y mantiene RPM, velocidad, refrigerante y voltaje de fondo: como máximo una consulta adicional cada 2 segundos.",
                "Prioritizes the screen and keeps RPM, speed, coolant and voltage updated in the background: at most one extra query every 2 seconds."
            )
            ALL -> tr(
                "Consulta continuamente todos los sensores habilitados en Sensores, aunque no se muestren. Puede reducir la velocidad de actualización.",
                "Continuously polls all sensors enabled in Sensors, even when not displayed. May reduce refresh rate."
            )
        }
}
