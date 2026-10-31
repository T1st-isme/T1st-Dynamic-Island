package com.akira.island.island

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import com.akira.island.core.AlarmInfo
import com.akira.island.core.BatteryInfo
import com.akira.island.core.CallInfo
import com.akira.island.core.ChronoInfo
import com.akira.island.core.LiveInfo
import com.akira.island.core.LiveKind
import com.akira.island.core.MediaInfo
import com.akira.island.core.Notice
import com.akira.island.core.PayReady
import com.akira.island.core.ProgressInfo
import com.akira.island.core.StopwatchState
import com.akira.island.core.TimerState
import com.akira.island.core.TorchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitySelectionTest {
    private fun music(pkg: String = "player", track: String = "song") = Act.MediaA(
        MediaInfo(
            key = "$pkg|$track|artist", pkg = pkg, title = track, artist = "artist", album = null,
            art = null, accent = Color.White, durationMs = 60_000, positionMs = 0, positionTime = 0,
            speed = 1f, playing = true, canSeek = true,
        ),
    )

    private fun call(incoming: Boolean = true, key: String = "call") = Act.CallA(
        CallInfo(key, "phone", "Caller", null, incoming, 0, 100, null, null, null, null),
    )

    private fun timer(ringing: Boolean = false) = Act.TimerA(TimerState(60_000, 60_000, ringing = ringing))

    private fun nav(key: String = "nav", time: Long = 100, pkg: String = "maps") = Act.LiveA(
        LiveInfo(key, pkg, LiveKind.NAV, "Turn right", "", "", null, null, time, null),
    )

    private fun progress(key: String = "download", time: Long = 100) = Act.ProgressA(
        ProgressInfo(key, "browser", "Download", null, 5, 10, false, time),
    )

    private fun chrono(key: String = "chrono", time: Long = 100) = Act.ChronoA(
        ChronoInfo(key, "clock", "Timer", null, 60_000, true, time),
    )

    private fun notice() = Act.NoticeA(Notice("Connected", icon = Icons.Default.Info, tint = Color.White))

    @Test fun `default category order is preserved independent of input order`() {
        val ordered = listOf(
            call(), notice(), timer(true), Act.PayA(PayReady("wallet", null)),
            Act.TorchA(TorchState(on = true)), nav(),
            Act.StopwatchA(StopwatchState(0, 0)), chrono(), music(), progress(),
            Act.ChargingA(BatteryInfo(50, true)), Act.AlarmA(AlarmInfo(60_000, "clock", null)),
        )
        // Each suffix reveals the next default primary; source map order must not matter.
        ordered.indices.forEach { i ->
            assertEquals(ordered[i], selectActivities(ordered.drop(i).reversed()).primary)
        }
        assertEquals(call(false), selectActivities(listOf(timer(), call(false))).primary)
        assertEquals(timer(), selectActivities(listOf(nav(), timer())).primary)
    }

    @Test fun `incoming call interrupts focused expanded music and hides bubble`() {
        val music = music()
        val selected = selectActivities(listOf(music, call()), music.instanceKey, expanded = true)
        assertEquals(call(), selected.primary)
        assertNull(selected.secondary)
        assertFalse(selected.showExpanded)
        assertFalse(selected.focusGone)
    }

    @Test fun `ongoing call and music have deterministic default primary and secondary`() {
        val music = music()
        val call = call(false)
        val selected = selectActivities(listOf(music, call))
        assertEquals(call, selected.primary)
        assertEquals(music, selected.secondary)
        // Existing ordinary manual focus behavior is retained for an ongoing (not ringing) call.
        val focused = selectActivities(listOf(call, music), music.instanceKey, expanded = true)
        assertEquals(music, focused.primary)
        assertEquals(call, focused.secondary)
        assertTrue(focused.showExpanded)
    }

    @Test fun `ending or hiding incoming call resumes valid music focus`() {
        val player = music()
        val before = selectActivities(listOf(player), player.instanceKey, expanded = true)
        val interrupted = selectActivities(listOf(call(), player), player.instanceKey, expanded = true)
        assertFalse(interrupted.focusGone)
        // Hidden/ended calls are filtered out at the eligibility boundary.
        assertEquals(before, selectActivities(listOf(player), player.instanceKey, expanded = true))
        val nextTrack = music(track = "next")
        val resumed = selectActivities(listOf(nextTrack), player.instanceKey, expanded = true)
        assertEquals(nextTrack, resumed.primary)
        assertTrue(resumed.showExpanded)
    }

    @Test fun `ringing timer interrupts expanded navigation and returns after dismissal`() {
        val nav = nav()
        val selected = selectActivities(listOf(nav, timer(true)), nav.instanceKey, expanded = true)
        assertEquals(timer(true), selected.primary)
        assertEquals(nav, selected.secondary)
        assertFalse(selected.showExpanded)
        assertFalse(selected.focusGone)
        val after = selectActivities(listOf(nav), nav.instanceKey, expanded = true)
        assertEquals(nav, after.primary)
        assertTrue(after.showExpanded)
    }

    @Test fun `notice keeps existing precedence over ringing timer`() {
        val notice = notice()
        val selected = selectActivities(listOf(timer(true), notice, nav()), nav().instanceKey, expanded = true)
        assertEquals(notice, selected.primary)
        assertNull(selected.secondary)
        assertFalse(selected.showExpanded)
    }

    @Test fun `notice ending or dismissal returns exact previous focused activity`() {
        val focused = progress()
        val candidates = listOf(nav(), focused, music())
        val before = selectActivities(candidates, focused.instanceKey, expanded = true)
        val interrupted = selectActivities(candidates + notice(), focused.instanceKey, expanded = true)
        assertTrue(interrupted.primary is Act.NoticeA)
        assertFalse(interrupted.showExpanded)
        assertFalse(interrupted.focusGone)
        assertEquals(before, selectActivities(candidates, focused.instanceKey, expanded = true))
    }

    @Test fun `payment ending returns normal default without remembered stale payload`() {
        val nav = nav()
        val music = music()
        val payment = Act.PayA(PayReady("wallet", null))
        assertEquals(payment, selectActivities(listOf(music, nav, payment)).primary)
        val after = selectActivities(listOf(music, nav))
        assertEquals(nav, after.primary)
        assertEquals(music, after.secondary)
    }

    @Test fun `removed focused source cannot transfer expansion to same type replacement`() {
        val old = nav("old", time = 100)
        val replacement = nav("new", time = 200)
        val selected = selectActivities(listOf(replacement, music()), old.instanceKey, expanded = true)
        assertEquals(replacement, selected.primary)
        assertTrue(selected.focusGone)
        assertFalse(selected.showExpanded)
    }

    @Test fun `removed focus falls back to another category or idle immediately`() {
        val focused = progress()
        val selected = selectActivities(listOf(music()), focused.instanceKey, expanded = true)
        assertEquals(music(), selected.primary)
        assertTrue(selected.focusGone)
        assertFalse(selected.showExpanded)
        val empty = selectActivities(emptyList(), focused.instanceKey, expanded = true)
        assertEquals(Act.Idle, empty.primary)
        assertNull(empty.secondary)
        assertFalse(empty.showExpanded)
    }

    @Test fun `same type defaults to newest but exact focus retains older valid source`() {
        val old = nav("old", time = 100)
        val newer = nav("new", time = 200)
        val selected = selectActivities(listOf(old, newer, music()))
        assertEquals(newer, selected.primary)
        assertEquals(music(), selected.secondary) // One representative per category, as before.
        val focused = selectActivities(listOf(old, newer, music()), old.instanceKey, expanded = true)
        assertEquals(old, focused.primary)
        assertEquals(music(), focused.secondary)
        assertTrue(focused.showExpanded)
    }

    @Test fun `equal timestamps break ties by stable source key in every multi-entry category`() {
        val pairs = listOf(
            nav("a") to nav("b"),
            chrono("a") to chrono("b"),
            progress("a") to progress("b"),
            call(false, "a") to call(false, "b"),
        )
        pairs.forEach { (a, b) ->
            assertEquals(a, selectActivities(listOf(a, b)).primary)
            assertEquals(a, selectActivities(listOf(b, a)).primary)
        }
    }

    @Test fun `focused ongoing call cannot mask incoming call in same category`() {
        val ongoing = call(false, "ongoing")
        val incoming = call(true, "incoming")
        val selected = selectActivities(listOf(ongoing, incoming, music()), ongoing.instanceKey, expanded = true)
        assertEquals(incoming, selected.primary)
        assertNull(selected.secondary)
        assertFalse(selected.showExpanded)
        assertFalse(selected.focusGone)
    }

    @Test fun `source payload updates retain identity but different sources do not`() {
        val old = nav()
        val updated = Act.LiveA(old.l.copy(title = "Turn left", postTime = 200))
        assertEquals(old.instanceKey, updated.instanceKey)
        assertNotEquals(old.instanceKey, nav("other").instanceKey)
        assertNotEquals(old.instanceKey, nav(pkg = "other.maps").instanceKey)
        assertEquals(updated, selectActivities(listOf(updated), old.instanceKey, expanded = true).primary)
        assertTrue(selectActivities(listOf(updated), old.instanceKey, expanded = true).showExpanded)
        assertEquals("live", old.id) // Animation/content category remains unchanged.
    }

    @Test fun `music track changes retain player focus but player changes do not`() {
        val old = music(track = "old")
        val updated = music(track = "new")
        assertNotEquals(old.m.key, updated.m.key)
        assertEquals(old.instanceKey, updated.instanceKey)
        assertTrue(selectActivities(listOf(updated), old.instanceKey, expanded = true).showExpanded)
        val switchedPlayer = selectActivities(listOf(music(pkg = "other")), old.instanceKey, expanded = true)
        assertTrue(switchedPlayer.focusGone)
        assertFalse(switchedPlayer.showExpanded)
    }

    @Test fun `singleton focus consumers and idle expansion remain compatible`() {
        assertEquals("timer", timer().instanceKey)
        assertEquals("stopwatch", Act.StopwatchA(StopwatchState(0, 0)).instanceKey)
        assertEquals("torch", Act.TorchA(TorchState(on = true)).instanceKey)
        assertTrue(selectActivities(listOf(timer()), "timer", expanded = true).showExpanded)
        assertTrue(selectActivities(emptyList(), "idle", expanded = true).showExpanded)
        assertFalse(selectActivities(listOf(music()), "idle", expanded = true).showExpanded)
    }

    @Test fun `primary and secondary never duplicate instances or categories`() {
        val a = nav("a")
        val b = nav("b")
        val music = music()
        val candidates = listOf(a, b, a, music, music, timer())
        listOf(null, a.instanceKey, music.instanceKey, "removed").forEach { focus ->
            val selected = selectActivities(candidates, focus)
            assertNotEquals(selected.primary.instanceKey, selected.secondary?.instanceKey)
            assertNotEquals(selected.primary.id, selected.secondary?.id)
        }
        assertNull(selectActivities(listOf(a, a, b)).secondary)
    }

    @Test fun `bubble setting and alarm exclusion are preserved`() {
        assertNull(selectActivities(listOf(nav(), music()), bubbleEnabled = false).secondary)
        assertNull(selectActivities(listOf(nav(), Act.AlarmA(AlarmInfo(60_000, "clock", null)))).secondary)
        assertEquals(Act.Idle, selectActivities(emptyList()).primary)
    }
}
