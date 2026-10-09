package com.torqueobd2.dash.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelsFullScreenPolicyTest {

    @Test
    fun `should auto fullscreen when on panels tab, not fullscreen, not editing and 10s elapsed`() {
        val lastTouch = 1_000L
        val current = lastTouch + PanelsFullScreenPolicy.AUTO_FULLSCREEN_TIMEOUT_MS

        val result = PanelsFullScreenPolicy.shouldAutoFullScreen(
            isPanelsTab = true,
            isFullScreen = false,
            isModalOrEditing = false,
            lastTouchTimeMs = lastTouch,
            currentTimeMs = current
        )

        assertTrue(result)
    }

    @Test
    fun `should not auto fullscreen before 10s timeout`() {
        val lastTouch = 1_000L
        val current = lastTouch + 9_999L

        val result = PanelsFullScreenPolicy.shouldAutoFullScreen(
            isPanelsTab = true,
            isFullScreen = false,
            isModalOrEditing = false,
            lastTouchTimeMs = lastTouch,
            currentTimeMs = current
        )

        assertFalse(result)
    }

    @Test
    fun `should not auto fullscreen when not on panels tab`() {
        val lastTouch = 1_000L
        val current = lastTouch + 20_000L

        val result = PanelsFullScreenPolicy.shouldAutoFullScreen(
            isPanelsTab = false,
            isFullScreen = false,
            isModalOrEditing = false,
            lastTouchTimeMs = lastTouch,
            currentTimeMs = current
        )

        assertFalse(result)
    }

    @Test
    fun `should not auto fullscreen when already in fullscreen`() {
        val lastTouch = 1_000L
        val current = lastTouch + 20_000L

        val result = PanelsFullScreenPolicy.shouldAutoFullScreen(
            isPanelsTab = true,
            isFullScreen = true,
            isModalOrEditing = false,
            lastTouchTimeMs = lastTouch,
            currentTimeMs = current
        )

        assertFalse(result)
    }

    @Test
    fun `should not auto fullscreen when editing or modal is open`() {
        val lastTouch = 1_000L
        val current = lastTouch + 20_000L

        val result = PanelsFullScreenPolicy.shouldAutoFullScreen(
            isPanelsTab = true,
            isFullScreen = false,
            isModalOrEditing = true,
            lastTouchTimeMs = lastTouch,
            currentTimeMs = current
        )

        assertFalse(result)
    }

    @Test
    fun `remainingDelayMs returns correct remaining time`() {
        val lastTouch = 10_000L
        assertEquals(10_000L, PanelsFullScreenPolicy.remainingDelayMs(lastTouch, 10_000L))
        assertEquals(6_000L, PanelsFullScreenPolicy.remainingDelayMs(lastTouch, 14_000L))
        assertEquals(0L, PanelsFullScreenPolicy.remainingDelayMs(lastTouch, 20_000L))
        assertEquals(0L, PanelsFullScreenPolicy.remainingDelayMs(lastTouch, 25_000L))
    }

    @Test
    fun `shouldExitFullScreenOnTouch returns true only when on panels and in fullscreen`() {
        assertTrue(PanelsFullScreenPolicy.shouldExitFullScreenOnTouch(isPanelsTab = true, isFullScreen = true))
        assertFalse(PanelsFullScreenPolicy.shouldExitFullScreenOnTouch(isPanelsTab = true, isFullScreen = false))
        assertFalse(PanelsFullScreenPolicy.shouldExitFullScreenOnTouch(isPanelsTab = false, isFullScreen = true))
        assertFalse(PanelsFullScreenPolicy.shouldExitFullScreenOnTouch(isPanelsTab = false, isFullScreen = false))
    }
}
