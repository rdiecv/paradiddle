// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import kotlin.math.abs

/**
 * Drops the metronome heard through the microphone. During the count-in (when the player
 * listens) it learns how loud the clicks reach the mic; afterwards a sound near a click must be
 * clearly louder than that to count as a hit. Feed sounds in time order.
 */
class ClickGate(private val clickTimes: List<Long>, private val countInEndNanos: Long) {
    private var bleed = 0f

    /** True if the sound at [timeNanos] (session time) with [level] is a hit to score. */
    fun accept(timeNanos: Long, level: Float): Boolean {
        val click = nearestClick(timeNanos) ?: return true
        if (abs(timeNanos - click) > WINDOW_NANOS) return true
        if (click < countInEndNanos) {
            // A count-in click: the player is silent, so this is what the click sounds like.
            if (level > bleed) bleed = level
            return false
        }
        return level > bleed * MARGIN
    }

    private fun nearestClick(t: Long): Long? {
        val i = clickTimes.binarySearch(t)
        if (i >= 0) return clickTimes[i]
        val after = clickTimes.getOrNull(-i - 1)
        val before = clickTimes.getOrNull(-i - 2)
        return when {
            after == null -> before
            before == null -> after
            else -> if (after - t < t - before) after else before
        }
    }

    private companion object {
        const val WINDOW_NANOS = 30 * NANOS_PER_MS
        const val MARGIN = 1.5f
    }
}
