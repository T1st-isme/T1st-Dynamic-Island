package com.akira.island.service

/** One resource session per binding. Calls are serialized by Android's main-thread callbacks. */
internal class ServiceSession(
    private val startResources: () -> Unit,
    private val stopResources: () -> Unit,
) {
    private var started = false

    fun connect() {
        if (started) return
        started = true
        try {
            startResources()
        } catch (error: Throwable) {
            try {
                disconnect()
            } catch (cleanupError: Throwable) {
                error.addSuppressed(cleanupError)
            }
            throw error
        }
    }

    fun disconnect() {
        if (!started) return
        // Clear before disposing: unbind, destroy and failed startup must not dispose twice.
        started = false
        stopResources()
    }
}
