package com.akira.island.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One timeout per paused source. Unchanged updates retain its deadline; removal cancels ownership. */
internal class PausedClockTimeouts(
    private val scope: CoroutineScope,
    private val timeoutMs: () -> Long,
    private val onTimeout: (String, Long) -> Unit,
) {
    private val pending = HashMap<String, Pair<Long, Job>>()

    fun schedule(key: String, pausedMs: Long) {
        val previous = pending[key]
        if (previous?.first == pausedMs && previous.second.isActive) return
        cancel(key)
        val duration = timeoutMs()
        val job = scope.launch {
            delay(duration)
            pending.remove(key)
            onTimeout(key, pausedMs)
        }
        pending[key] = pausedMs to job
    }

    fun cancel(key: String) {
        pending.remove(key)?.second?.cancel()
    }
}
