// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlin.math.abs

/** Measures the constant delay between hearing a click and the tap reaching the app. */
object Calibration {
    const val CLICKS = 16
    const val BPM = 90
    const val IGNORED_CLICKS = 4
    const val MIN_MATCHES = 8
    private const val MATCH_WINDOW_NANOS = 250 * NANOS_PER_MS

    /**
     * Returns the median of (tap − click) over the matched clicks after the first
     * [IGNORED_CLICKS], or null if fewer than [MIN_MATCHES] clicks got a tap nearby.
     */
    fun offsetNanos(clickTimes: List<Long>, tapTimes: List<Long>): Long? {
        val diffs = clickTimes.drop(IGNORED_CLICKS).mapNotNull { click ->
            tapTimes.minByOrNull { abs(it - click) }
                ?.takeIf { abs(it - click) <= MATCH_WINDOW_NANOS }
                ?.let { it - click }
        }
        if (diffs.size < MIN_MATCHES) return null
        val sorted = diffs.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
