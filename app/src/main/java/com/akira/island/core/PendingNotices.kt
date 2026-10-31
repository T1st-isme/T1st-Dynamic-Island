package com.akira.island.core

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

/** A bounded queue whose message sources can be withdrawn without affecting other events. */
internal class PendingNotices(capacity: Int = 6) {
    private class Entry(val notice: Notice) {
        @Volatile var valid = true
    }

    private val pending = ConcurrentHashMap.newKeySet<Entry>()
    private val channel = Channel<Entry>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { pending.remove(it) },
    )

    fun post(notice: Notice) {
        val entry = Entry(notice)
        pending.add(entry)
        if (channel.trySend(entry).isFailure) pending.remove(entry)
    }

    fun discardSource(key: String) {
        pending.forEach { entry ->
            if (entry.notice.style == NoticeStyle.MESSAGE && entry.notice.message?.key == key) {
                entry.valid = false
            }
        }
    }

    suspend fun receive(): Notice {
        while (true) {
            val entry = channel.receive()
            pending.remove(entry)
            if (entry.valid) return entry.notice
        }
    }
}
