// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui

import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameDelayTest {
    @Test
    fun estimateIsTwoRefreshes() {
        assertEquals(33_333_333L, FrameDelay.estimateNanos(60f))
        assertEquals(16_666_666L, FrameDelay.estimateNanos(120f))
    }

    @Test
    fun unknownRefreshRateCountsAs60Hz() {
        assertEquals(FrameDelay.estimateNanos(60f), FrameDelay.estimateNanos(0f))
    }

    @Test
    fun measuredDelayIsUsedOnlyWhenPlausible() {
        val estimate = FrameDelay.estimateNanos(60f)
        assertEquals(40 * NANOS_PER_MS, FrameDelay.pick(measured = 40 * NANOS_PER_MS, estimate = estimate))
        assertEquals(estimate, FrameDelay.pick(measured = -5 * NANOS_PER_MS, estimate = estimate))
        assertEquals(estimate, FrameDelay.pick(measured = 900 * NANOS_PER_MS, estimate = estimate))
    }
}
