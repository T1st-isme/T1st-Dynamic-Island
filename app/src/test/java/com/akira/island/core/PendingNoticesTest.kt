package com.akira.island.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class PendingNoticesTest {
    private fun event(title: String) = Notice(title, icon = Icons.Default.Info, tint = Color.White)
    private fun message(key: String) = event(key).copy(
        style = NoticeStyle.MESSAGE,
        message = AppMessage(key, "chat", "Chat", "Private text", null, null, null),
    )

    @Test fun `bounded queue drops oldest and preserves order`() = runTest {
        val queue = PendingNotices(2)
        val first = event("first")
        val second = event("second")
        val third = event("third")
        queue.post(first)
        queue.post(second)
        queue.post(third)
        assertSame(second, queue.receive())
        assertSame(third, queue.receive())
    }

    @Test fun `withdrawn provider cannot deliver queued banners after ranking is forgotten`() = runTest {
        val queue = PendingNotices()
        val withdrawn = message("removed")
        val unrelated = message("other")
        queue.post(withdrawn)
        queue.post(unrelated)
        queue.discardSource("removed")
        assertSame(unrelated, queue.receive())
    }

    @Test fun `new notification may reuse key without reviving old queued payload`() = runTest {
        val queue = PendingNotices()
        val old = message("same-key")
        val replacement = message("same-key")
        queue.post(old)
        queue.discardSource("same-key")
        queue.post(replacement)
        assertSame(replacement, queue.receive())
    }

    @Test fun `source withdrawal is idempotent and preserves non-message events`() = runTest {
        val queue = PendingNotices()
        val regular = event("charging")
        queue.post(message("removed"))
        queue.post(regular)
        queue.discardSource("removed")
        queue.discardSource("removed")
        queue.discardSource("unknown")
        assertSame(regular, queue.receive())
    }
}
