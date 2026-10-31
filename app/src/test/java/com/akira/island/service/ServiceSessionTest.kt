package com.akira.island.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class ServiceSessionTest {
    @Test fun `repeated connection initializes only once`() {
        var starts = 0
        val session = ServiceSession({ starts++ }, {})
        session.connect()
        session.connect()
        assertEquals(1, starts)
    }

    @Test fun `unbind followed by destroy disposes only once`() {
        var stops = 0
        val session = ServiceSession({}, { stops++ })
        session.connect()
        session.disconnect()
        session.disconnect()
        assertEquals(1, stops)
    }

    @Test fun `disconnect before connection is harmless`() {
        var stops = 0
        ServiceSession({}, { stops++ }).disconnect()
        assertEquals(0, stops)
    }

    @Test fun `same instance may reconnect with fresh resources`() {
        val events = mutableListOf<String>()
        val session = ServiceSession({ events += "start" }, { events += "stop" })
        session.connect()
        session.disconnect()
        session.connect()
        session.disconnect()
        assertEquals(listOf("start", "stop", "start", "stop"), events)
    }

    @Test fun `partial initialization is rolled back and can be retried`() {
        val error = IllegalStateException("start")
        var attempts = 0
        var stops = 0
        val session = ServiceSession({
            attempts++
            if (attempts == 1) throw error
        }, { stops++ })
        try {
            session.connect()
            fail("Expected initialization failure")
        } catch (actual: IllegalStateException) {
            assertSame(error, actual)
        }
        session.disconnect()
        assertEquals(1, stops)
        session.connect()
        session.disconnect()
        assertEquals(2, attempts)
        assertEquals(2, stops)
    }

    @Test fun `cleanup failure is not retried by a duplicate disconnect`() {
        var stops = 0
        val session = ServiceSession({}, {
            stops++
            throw IllegalStateException("stop")
        })
        session.connect()
        try {
            session.disconnect()
            fail("Expected cleanup failure")
        } catch (_: IllegalStateException) {
            // The session is already disconnected even though disposal failed.
        }
        session.disconnect()
        assertEquals(1, stops)
    }

    @Test fun `initialization failure preserves cleanup failure as suppressed`() {
        val start = IllegalArgumentException("start")
        val stop = IllegalStateException("stop")
        val session = ServiceSession({ throw start }, { throw stop })
        try {
            session.connect()
            fail("Expected initialization failure")
        } catch (actual: IllegalArgumentException) {
            assertSame(start, actual)
            assertSame(stop, actual.suppressed.single())
        }
    }
}
