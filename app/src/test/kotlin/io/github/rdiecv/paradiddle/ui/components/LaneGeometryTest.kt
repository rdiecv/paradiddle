// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaneGeometryTest {
    private val lanes = LaneGeometry(hitY = 400f, topY = 0f, lookaheadNanos = 2_000 * NANOS_PER_MS)

    @Test
    fun noteIsCentredOnTheHitLineAtItsTime() {
        val t = 5_000 * NANOS_PER_MS
        listOf(9f, 16f).forEach { h ->
            assertEquals(400f, lanes.noteTop(t, nowNanos = t, height = h) + h / 2, 0.001f)
        }
    }

    @Test
    fun noteIsWhollyAboveTheLineHalfItsHeightBeforeItsTime() {
        val t = 5_000 * NANOS_PER_MS
        val h = 16f
        // 400 px per 2 s = 0.2 px per ms, so half of 16 px is 40 ms.
        val top = lanes.noteTop(t, nowNanos = t - 40 * NANOS_PER_MS, height = h)
        assertEquals(400f, top + h, 0.001f)
        assertTrue(lanes.yOf(t + 2_000 * NANOS_PER_MS, nowNanos = t) == 0f)
    }
}
