package com.akira.island.island

/** A synchronous snapshot; neither selection nor continuation depends on animation completion. */
internal data class ActivitySelection(
    val primary: Act,
    val secondary: Act?,
    val showExpanded: Boolean,
    val focusGone: Boolean,
)

/**
 * Arbitration over eligible activities. Filtering settings, expiry and duplicate phone/app call
 * representations happens at the source boundary, before this pure policy.
 *
 * Default order (preserved): incoming call, notice, ringing timer, payment, ongoing call, torch,
 * running timer, live, stopwatch, notification chrono, media, progress, charging, upcoming alarm.
 * One activity per category is exposed. Newest postTime wins within notification categories;
 * equal times use ascending instanceKey, independent of map insertion order.
 *
 * Exact manual focus retains its source within a category, then promotes it below notices.
 * Incoming calls and ringing timers prevent promotion. Interruptions suspend expansion without
 * erasing focus; an expired/removed focused source cannot transfer expansion to its replacement.
 */
internal fun selectActivities(
    candidates: List<Act>,
    focus: String? = null,
    expanded: Boolean = false,
    bubbleEnabled: Boolean = true,
): ActivitySelection {
    val eligible = candidates.filterNot { it is Act.Idle }
    val ordered = eligible.sortedWith(
        compareBy<Act> { it.priority }
            .thenByDescending { it.postTime }
            .thenBy { it.instanceKey },
    ).groupBy { it.id }.values.map { sameCategory ->
        // An incoming call must also beat a focused ongoing call within the call category.
        sameCategory.firstOrNull { it.urgent }
            ?: sameCategory.firstOrNull { it.instanceKey == focus }
            ?: sameCategory.first()
    }.sortedBy { it.priority }.toMutableList()

    val focusedIndex = ordered.indexOfFirst { it.instanceKey == focus && !it.transient }
    if (focusedIndex > 0 && ordered.none { it.urgent }) {
        val focused = ordered.removeAt(focusedIndex)
        ordered.add(if (ordered.firstOrNull() is Act.NoticeA) 1 else 0, focused)
    }

    val primary = ordered.firstOrNull() ?: Act.Idle
    val focusGone = focus != null && focus != Act.Idle.instanceKey &&
        eligible.none { it.instanceKey == focus }
    val incoming = primary is Act.CallA && primary.c.incoming
    val secondary = if (!bubbleEnabled || primary.transient || incoming) null else
        ordered.firstOrNull {
            it.instanceKey != primary.instanceKey && !it.transient && it !is Act.AlarmA
        }
    val showExpanded = expanded && !focusGone && !primary.transient && !incoming &&
        (focus == null || focus == primary.instanceKey)
    return ActivitySelection(primary, secondary, showExpanded, focusGone)
}

private val Act.urgent: Boolean
    get() = this is Act.CallA && c.incoming || this is Act.TimerA && t.ringing

private val Act.priority: Int
    get() = when (this) {
        is Act.CallA -> if (c.incoming) 0 else 4
        is Act.NoticeA -> 1
        is Act.TimerA -> if (t.ringing) 2 else 6
        is Act.PayA -> 3
        is Act.TorchA -> 5
        is Act.LiveA -> 7
        is Act.StopwatchA -> 8
        is Act.ChronoA -> 9
        is Act.MediaA -> 10
        is Act.ProgressA -> 11
        is Act.ChargingA -> 12
        is Act.AlarmA -> 13
        Act.Idle -> 14
    }

private val Act.postTime: Long
    get() = when (this) {
        is Act.CallA -> c.postTime
        is Act.LiveA -> l.postTime
        is Act.ChronoA -> c.postTime
        is Act.ProgressA -> p.postTime
        else -> 0L
    }
