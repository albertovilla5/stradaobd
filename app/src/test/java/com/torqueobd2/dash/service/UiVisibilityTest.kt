package com.torqueobd2.dash.service

import org.junit.Assert.*
import org.junit.Test

class UiVisibilityTest {
    @Test fun stoppingPreviousActivityDoesNotHideItsVisibleReplacement() {
        val tracker = UiVisibility(); val previous = Any(); val replacement = Any()
        assertTrue(tracker.update(previous, true))
        assertTrue(tracker.update(replacement, true))
        assertTrue(tracker.update(previous, false))
        assertFalse(tracker.update(replacement, false))
    }

    @Test fun repeatedStartsAndUnknownStopsDoNotLeakVisibility() {
        val tracker = UiVisibility(); val owner = Any()
        assertFalse(tracker.update(Any(), false))
        assertTrue(tracker.update(owner, true))
        assertTrue(tracker.update(owner, true))
        assertTrue(tracker.update(Any(), false))
        assertFalse(tracker.update(owner, false))
        assertFalse(tracker.update(owner, false))
    }
}
