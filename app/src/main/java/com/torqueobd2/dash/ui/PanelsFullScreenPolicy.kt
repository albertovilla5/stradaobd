package com.torqueobd2.dash.ui

/**
 * Reglas de activación y salida automática de pantalla completa para la vista de Paneles.
 * Tras 10 segundos de inactividad sin toques en pantalla, se activa la pantalla completa.
 * Cualquier toque en pantalla regresa inmediatamente al modo normal.
 */
object PanelsFullScreenPolicy {
    /** Tiempo de inactividad en milisegundos antes de activar pantalla completa automática (10 segundos). */
    const val AUTO_FULLSCREEN_TIMEOUT_MS = 10_000L

    /**
     * Determina si la pantalla de Paneles debe entrar en modo de pantalla completa tras agotar el tiempo sin toques.
     */
    fun shouldAutoFullScreen(
        isPanelsTab: Boolean,
        isFullScreen: Boolean,
        isModalOrEditing: Boolean,
        lastTouchTimeMs: Long,
        currentTimeMs: Long
    ): Boolean {
        if (!isPanelsTab || isFullScreen || isModalOrEditing) return false
        val elapsed = currentTimeMs - lastTouchTimeMs
        return elapsed >= AUTO_FULLSCREEN_TIMEOUT_MS
    }

    /**
     * Devuelve el retardo restante en milisegundos hasta la activación automática.
     */
    fun remainingDelayMs(lastTouchTimeMs: Long, currentTimeMs: Long): Long {
        val elapsed = currentTimeMs - lastTouchTimeMs
        return (AUTO_FULLSCREEN_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Determina si un toque en pantalla debe provocar la salida de pantalla completa.
     */
    fun shouldExitFullScreenOnTouch(
        isPanelsTab: Boolean,
        isFullScreen: Boolean
    ): Boolean {
        return isPanelsTab && isFullScreen
    }
}
