package com.akira.island.service

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayLifecycleOwnerTest {
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    @Test
    fun inactiveBeforeCreationDoesNotStartLifecycle() {
        val owner = OverlayLifecycleOwner()
        owner.setActive(false)
        assertEquals(Lifecycle.State.INITIALIZED, owner.lifecycle.currentState)
    }

    @Test
    fun hidingCancelsVisualDelaysAndShowingRestartsWithoutCatchUp() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = OverlayLifecycleOwner()
        try {
            owner.create()
            var ticks = 0
            var cancellations = 0
            val visual = launch {
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    try {
                        while (true) {
                            ticks++
                            delay(100)
                        }
                    } finally {
                        cancellations++
                    }
                }
            }
            runCurrent()
            assertEquals(1, ticks)
            advanceTimeBy(250)
            runCurrent()
            assertEquals(3, ticks)

            owner.setActive(false)
            runCurrent()
            assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
            assertEquals(1, cancellations)
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(3, ticks)

            owner.setActive(true)
            runCurrent()
            assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
            assertEquals(4, ticks)
            // Reapplying settings must not start a duplicate loop.
            owner.setActive(true)
            advanceTimeBy(100)
            runCurrent()
            assertEquals(5, ticks)

            owner.destroy()
            runCurrent()
            assertEquals(2, cancellations)
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(5, ticks)
            visual.join()
            owner.setActive(true)
            assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        } finally {
            owner.destroy()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun domainWorkContinuesWhileVisualWorkIsInactive() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = OverlayLifecycleOwner()
        try {
            owner.create()
            var visualFired = false
            var alertFired = false
            val visual = launch {
                owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    delay(1000)
                    visualFired = true
                }
            }
            val alert = launch {
                delay(1000)
                alertFired = true
            }
            runCurrent()
            owner.setActive(false)
            runCurrent()
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(false, visualFired)
            assertEquals(true, alertFired)
            owner.destroy()
            visual.join()
            alert.join()
        } finally {
            owner.destroy()
            Dispatchers.resetMain()
        }
    }
}
