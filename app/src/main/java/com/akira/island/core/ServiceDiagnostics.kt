package com.akira.island.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-local evidence from service callbacks, never inferred from a permission or preference. */
data class ServiceRuntime(
    val accessibilityError: String? = null,
    val notificationConnected: Boolean = false,
    val displayAttached: Boolean = false,
    val touchAttached: Boolean = false,
    val overlayError: String? = null,
    val keepAliveRunning: Boolean = false,
    val keepAliveError: String? = null,
)

object ServiceDiagnostics {
    private val mutable = MutableStateFlow(ServiceRuntime())
    val state = mutable.asStateFlow()

    fun update(change: ServiceRuntime.() -> ServiceRuntime) {
        mutable.update { it.change() }
    }
}

enum class IslandHealth {
    ACCESSIBILITY_OFF, SERVICE_DISCONNECTED, OVERLAY_FAILED, PAUSED, LANDSCAPE, SCHEDULE, READY;

    companion object {
        fun evaluate(
            accessibility: Boolean,
            connected: Boolean,
            runtime: ServiceRuntime,
            enabled: Boolean,
            hiddenInLandscape: Boolean,
            outsideSchedule: Boolean,
        ): IslandHealth = when {
            !accessibility -> ACCESSIBILITY_OFF
            !connected -> SERVICE_DISCONNECTED
            !runtime.displayAttached || !runtime.touchAttached -> OVERLAY_FAILED
            !enabled -> PAUSED
            hiddenInLandscape -> LANDSCAPE
            outsideSchedule -> SCHEDULE
            else -> READY
        }
    }
}
