package com.akira.island.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope

/**
 * Visual work only: cancel delays as well as frame requests when the overlay is inactive.
 * Restart from the current clock/state when it becomes visible again. Domain work such as
 * ringing timers and receiving calls must stay in the service, outside this effect.
 */
@Composable
internal fun RenderingEffect(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, *keys) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}
