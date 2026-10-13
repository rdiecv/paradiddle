// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickGateTest {
    private fun ms(t: Long) = t * NANOS_PER_MS
    private val clicks = (0 until 8).map { ms(it * 500L) }

    @Test
    fun learnsTheClickBleedDuringTheCountInAndDropsItLater() {
        val gate = ClickGate(clicks, countInEndNanos = ms(2000))
        listOf(0L, 500, 1000, 1500).forEach { assertFalse(gate.accept(ms(it + 5), 0.1f)) }
        assertFalse("bleed of a later click", gate.accept(ms(2003), 0.12f))
        assertTrue("a real hit on the click", gate.accept(ms(2502), 0.4f))
        assertTrue("a soft hit away from clicks", gate.accept(ms(2750), 0.05f))
    }

    @Test
    fun aFirstNotePlayedEarlyIsNotDropped() {
        val gate = ClickGate(clicks, countInEndNanos = ms(2000))
        listOf(0L, 500, 1000, 1500).forEach { gate.accept(ms(it + 5), 0.1f) }
        assertTrue("20 ms before the first scored click", gate.accept(ms(1980), 0.4f))
        assertFalse("bleed of the first scored click", gate.accept(ms(2002), 0.1f))
    }

    @Test
    fun withHeadphonesNothingIsDropped() {
        val gate = ClickGate(clicks, countInEndNanos = ms(2000))
        assertTrue(gate.accept(ms(2001), 0.03f))
    }
}
