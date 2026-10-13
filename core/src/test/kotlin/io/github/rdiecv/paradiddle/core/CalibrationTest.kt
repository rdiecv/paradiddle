// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationTest {
    private val clicks = (0 until 16).map { it * 667 * MS }

    @Test
    fun medianOfOffsetsIgnoringFirstClicks() {
        val taps = clicks.mapIndexed { i, c -> c + (if (i < 4) 200 else 40 + (i % 3)) * MS }
        assertEquals(41 * MS, Calibration.offsetNanos(clicks, taps))
    }

    @Test
    fun outlierDoesNotMoveMedian() {
        val taps = clicks.map { it + 30 * MS }.toMutableList()
        taps[10] = clicks[10] + 240 * MS
        assertEquals(30 * MS, Calibration.offsetNanos(clicks, taps))
    }

    @Test
    fun tooFewTapsGivesNull() {
        val taps = clicks.drop(4).take(7).map { it + 30 * MS }
        assertNull(Calibration.offsetNanos(clicks, taps))
    }
}
