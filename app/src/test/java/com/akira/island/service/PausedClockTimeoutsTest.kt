package com.akira.island.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PausedClockTimeoutsTest {
    @Test fun `paused entry times out at its deadline`() = runTest {
        val expired = mutableListOf<Pair<String, Long>>()
        val timeouts = PausedClockTimeouts(this, { 1000 }) { key, paused -> expired += key to paused }
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(999)
        assertTrue(expired.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("clock" to 60_000L), expired)
    }

    @Test fun `identical paused updates do not postpone deadline`() = runTest {
        val expired = mutableListOf<String>()
        val timeouts = PausedClockTimeouts(this, { 1000 }) { key, _ -> expired += key }
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(700)
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(listOf("clock"), expired)
    }

    @Test fun `removal cancels old deadline before same key and paused value reappear`() = runTest {
        val expired = mutableListOf<String>()
        val timeouts = PausedClockTimeouts(this, { 1000 }) { key, _ -> expired += key }
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(700)
        // Used for non-chrono updates, resumed clocks, notification removal and listener disconnect.
        timeouts.cancel("clock")
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(300)
        runCurrent()
        assertTrue(expired.isEmpty())
        advanceTimeBy(700)
        runCurrent()
        assertEquals(listOf("clock"), expired)
    }

    @Test fun `changed pause value replaces prior deadline`() = runTest {
        val expired = mutableListOf<Long>()
        val timeouts = PausedClockTimeouts(this, { 1000 }) { _, paused -> expired += paused }
        timeouts.schedule("clock", 60_000)
        advanceTimeBy(700)
        timeouts.schedule("clock", 30_000)
        advanceTimeBy(300)
        runCurrent()
        assertTrue(expired.isEmpty())
        advanceTimeBy(700)
        runCurrent()
        assertEquals(listOf(30_000L), expired)
    }

    @Test fun `cancellation is idempotent and affects only owned key`() = runTest {
        val expired = mutableListOf<String>()
        val timeouts = PausedClockTimeouts(this, { 1000 }) { key, _ -> expired += key }
        timeouts.schedule("removed", 60_000)
        timeouts.schedule("remaining", 30_000)
        timeouts.cancel("removed")
        timeouts.cancel("removed")
        timeouts.cancel("unknown")
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(listOf("remaining"), expired)
    }

    @Test fun `new schedules use current duration without moving existing deadline`() = runTest {
        var duration = 1000L
        val expired = mutableListOf<String>()
        val timeouts = PausedClockTimeouts(this, { duration }) { key, _ -> expired += key }
        timeouts.schedule("original", 60_000)
        advanceTimeBy(500)
        duration = 2000L
        timeouts.schedule("original", 60_000) // Still the original deadline.
        timeouts.schedule("new", 30_000)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf("original"), expired)
        advanceTimeBy(1499)
        assertEquals(listOf("original"), expired)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("original", "new"), expired)
    }
}
