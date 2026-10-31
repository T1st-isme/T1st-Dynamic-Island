package com.akira.island.island

import com.akira.island.core.CallInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSelectionTest {
    private val phone = CallInfo("telephony", "dialer", "Phone number", null, false, 50, 100, null, null, null, null)
    private val notification = phone.copy(key = "notification-key", name = "Contact name", incoming = true, base = 100)

    @Test fun `notification arrival and removal do not change phone call identity`() {
        val focused = Act.CallA(phone).instanceKey
        val rich = Act.CallA(reconcileCall(phone, notification)!!)
        assertEquals(focused, rich.instanceKey)
        assertEquals("Contact name", rich.c.name)
        assertTrue(selectActivities(listOf(rich), focused, expanded = true).showExpanded)
        val afterRemoval = Act.CallA(reconcileCall(phone, null)!!)
        assertEquals(focused, afterRemoval.instanceKey)
        assertTrue(selectActivities(listOf(afterRemoval), focused, expanded = true).showExpanded)
    }

    @Test fun `phone state and answered base win over stale ringing notification`() {
        val rich = reconcileCall(phone, notification)!!
        assertFalse(rich.incoming)
        assertEquals(phone.base, rich.base)
    }

    @Test fun `notification alone retains its own identity`() {
        assertEquals(notification, reconcileCall(null, notification))
        assertEquals(phone, reconcileCall(phone, null))
        assertNull(reconcileCall(null, null))
    }
}
