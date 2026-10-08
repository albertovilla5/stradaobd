package com.torqueobd2.dash.data.model

enum class PollingMode(val title: String, val description: String) {
    VISIBLE("Solo pantalla", "Consulta los indicadores visibles y los sensores con avisos activados, aunque no estén en pantalla."),
    BALANCED("Equilibrado", "Prioriza la pantalla y mantiene RPM, velocidad, refrigerante y voltaje de fondo: como máximo una consulta adicional cada 2 segundos."),
    ALL("Todos los habilitados", "Consulta continuamente todos los sensores habilitados en Sensores, aunque no se muestren. Puede reducir la velocidad de actualización.")
}
