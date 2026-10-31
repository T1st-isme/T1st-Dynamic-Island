package com.akira.island.core

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandHealthTest {
    private val attached = ServiceRuntime(displayAttached = true, touchAttached = true)

    private fun health(
        accessibility: Boolean = true,
        connected: Boolean = true,
        runtime: ServiceRuntime = attached,
        enabled: Boolean = true,
        landscape: Boolean = false,
        schedule: Boolean = false,
    ) = IslandHealth.evaluate(accessibility, connected, runtime, enabled, landscape, schedule)

    @Test fun `permission alone never means running`() {
        assertEquals(IslandHealth.SERVICE_DISCONNECTED, health(connected = false))
    }

    @Test fun `revoked access takes precedence over stale callback state`() {
        assertEquals(IslandHealth.ACCESSIBILITY_OFF, health(accessibility = false))
        assertEquals(IslandHealth.ACCESSIBILITY_OFF, health(accessibility = false, connected = false))
    }

    @Test fun `both overlay windows must be attached`() {
        assertEquals(IslandHealth.OVERLAY_FAILED, health(runtime = ServiceRuntime()))
        assertEquals(IslandHealth.OVERLAY_FAILED, health(runtime = attached.copy(displayAttached = false)))
        assertEquals(IslandHealth.OVERLAY_FAILED, health(runtime = attached.copy(touchAttached = false)))
    }

    @Test fun `connection failures take precedence over hidden settings`() {
        assertEquals(IslandHealth.SERVICE_DISCONNECTED, health(connected = false, enabled = false))
        assertEquals(IslandHealth.OVERLAY_FAILED, health(runtime = ServiceRuntime(), enabled = false))
    }

    @Test fun `paused island is not described as starting`() {
        assertEquals(IslandHealth.PAUSED, health(enabled = false))
        assertEquals(IslandHealth.PAUSED, health(enabled = false, landscape = true, schedule = true))
    }

    @Test fun `landscape and schedule explain hidden windows`() {
        assertEquals(IslandHealth.LANDSCAPE, health(landscape = true))
        assertEquals(IslandHealth.SCHEDULE, health(schedule = true))
        assertEquals(IslandHealth.LANDSCAPE, health(landscape = true, schedule = true))
    }

    @Test fun `optional services do not gate the island`() {
        assertEquals(IslandHealth.READY, health(runtime = attached.copy(
            notificationConnected = false, keepAliveRunning = false, keepAliveError = "start: SecurityException",
        )))
        assertEquals(IslandHealth.READY, health(runtime = attached.copy(
            notificationConnected = true, keepAliveRunning = true,
        )))
    }

    @Test fun `fresh process does not report ready`() {
        assertEquals(IslandHealth.SERVICE_DISCONNECTED, health(connected = false, runtime = ServiceRuntime()))
    }
}
