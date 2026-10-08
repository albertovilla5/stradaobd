package com.torqueobd2.dash.data.parser

import com.torqueobd2.dash.data.model.SensorTelemetryStatus.*
import org.junit.Assert.*
import org.junit.Test

class ResponseFailureTest {
    @Test fun transientFailuresDoNotClaimIncompatibility() {
        for (raw in listOf("NO DATA\r>", "\r>", "UNABLE TO CONNECT", "STOPPED", "BUS BUSY", "7F 22 78", "7F 22 21")) {
            assertEquals(raw, NO_RESPONSE, SupportedPidsDetector.responseFailure("222512", raw))
        }
        assertEquals(INVALID_RESPONSE, SupportedPidsDetector.responseFailure("222512", "CAN ERROR"))
        assertNull(SupportedPidsDetector.responseFailure("222512", "62 25 12 01"))
    }

    @Test fun explicitRejectionMustMatchRequestedMode() {
        for (nrc in listOf("11", "12", "31")) {
            assertEquals(INCOMPATIBLE, SupportedPidsDetector.responseFailure("222512", "7F 22 $nrc\r>"))
            assertEquals(INCOMPATIBLE, SupportedPidsDetector.responseFailure("222512", "7F22$nrc\r>"))
            assertNotEquals(INCOMPATIBLE, SupportedPidsDetector.responseFailure("010C", "7F 22 $nrc\r>"))
        }
    }

    @Test fun rejectionBytesInsideAPositivePayloadAreOrdinaryData() {
        assertNull(SupportedPidsDetector.responseFailure("222512", "62 25 12 7F 22 31\r>"))
        assertEquals(INCOMPATIBLE, SupportedPidsDetector.responseFailure("222512", "222512\r7F 22 31\r>"))
    }
}
