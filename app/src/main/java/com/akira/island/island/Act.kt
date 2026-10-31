package com.akira.island.island

import com.akira.island.core.AlarmInfo
import com.akira.island.core.BatteryInfo
import com.akira.island.core.CallInfo
import com.akira.island.core.ChronoInfo
import com.akira.island.core.LiveInfo
import com.akira.island.core.MediaInfo
import com.akira.island.core.Notice
import com.akira.island.core.PayReady
import com.akira.island.core.ProgressInfo
import com.akira.island.core.StopwatchState
import com.akira.island.core.TimerState
import com.akira.island.core.TorchState

/** То, что сейчас может показывать остров. Короткие (transient) не раскрываются и не уходят в пузырь. */
sealed class Act(val id: String, val transient: Boolean = false) {
    open val pkg: String? get() = null

    /**
     * Selection/focus identity, deliberately separate from [id], the animation/content category.
     * Notification updates keep their source key; music keeps its player package across track changes
     * (MediaInfo.key identifies a track, not a session). Local controls expose singleton activities.
     */
    val instanceKey: String
        get() = when (this) {
            Idle, is TimerA, is StopwatchA, is TorchA, is ChargingA -> id
            is NoticeA -> "$id:${n.id}"
            is ChronoA -> sourceKey(c.pkg, c.key)
            is MediaA -> sourceKey(m.pkg)
            is ProgressA -> sourceKey(p.pkg, p.key)
            is CallA -> sourceKey(c.pkg, c.key)
            is LiveA -> sourceKey(l.pkg, l.key)
            is PayA -> sourceKey(p.pkg)
            is AlarmA -> sourceKey(a.pkg.orEmpty(), a.triggerAt.toString())
        }

    private fun sourceKey(pkg: String, key: String = "") = "$id:${pkg.length}:$pkg:$key"

    data object Idle : Act("idle")
    data class NoticeA(val n: Notice) : Act("notice", transient = true)
    data class TimerA(val t: TimerState) : Act("timer")
    data class StopwatchA(val w: StopwatchState) : Act("stopwatch")
    data class TorchA(val t: TorchState) : Act("torch")
    data class ChronoA(val c: ChronoInfo) : Act("chrono") {
        override val pkg: String get() = c.pkg
    }
    data class MediaA(val m: MediaInfo) : Act("media") {
        override val pkg: String get() = m.pkg
    }
    data class ProgressA(val p: ProgressInfo) : Act("progress") {
        override val pkg: String get() = p.pkg
    }
    data class CallA(val c: CallInfo) : Act("call") {
        override val pkg: String get() = c.pkg
    }
    data class LiveA(val l: LiveInfo) : Act("live") {
        override val pkg: String get() = l.pkg
    }
    data class ChargingA(val b: BatteryInfo) : Act("charging")
    /** Оплата телефоном: ждём терминал. Не раскрывается и не уходит в пузырь */
    data class PayA(val p: PayReady) : Act("pay", transient = true) {
        override val pkg: String get() = p.pkg
    }
    data class AlarmA(val a: AlarmInfo) : Act("alarm") {
        override val pkg: String? get() = a.pkg
    }
}
