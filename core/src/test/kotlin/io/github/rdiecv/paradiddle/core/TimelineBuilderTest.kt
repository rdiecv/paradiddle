// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineBuilderTest {
    @Test
    fun placesSixteenthsAfterCountIn() {
        val t = TimelineBuilder.build(rudiment("R L R L"), bpm = 60, repetitions = 2)
        assertEquals(4_000 * MS, t.countInNanos)
        assertEquals(8, t.notes.size)
        assertEquals(listOf(4000L, 4250L, 4500L, 4750L, 5000L), t.notes.take(5).map { it.timeNanos / MS })
        assertEquals(1, t.notes[4].repetition)
        assertEquals(250 * MS, t.notes[0].durationNanos)
    }

    @Test
    fun clicksOnEveryBeatWithStrongPatternStarts() {
        val t = TimelineBuilder.build(rudiment("R L R L R L R L"), bpm = 120, repetitions = 2)
        // 4 count-in + 2 reps × 2 beats
        assertEquals(8, t.clicks.size)
        assertEquals(listOf(true, false, false, false, true, false, true, false), t.clicks.map { it.strong })
        assertTrue(t.clicks.take(4).all { it.countIn })
        assertFalse(t.clicks[4].countIn)
        assertEquals(500 * MS, t.clicks[1].timeNanos)
    }

    @Test
    fun restsTakeTimeAndDurationsStretch() {
        val t = TimelineBuilder.build(rudiment("R:2 - L"), bpm = 60, repetitions = 1)
        assertEquals(2, t.notes.size)
        assertEquals(500 * MS, t.notes[0].durationNanos)
        assertEquals(4_750 * MS, t.notes[1].timeNanos)
    }

    @Test
    fun gracesComeJustBeforeTheirStroke() {
        val t = TimelineBuilder.build(rudiment("llR rrL", grid = 1), bpm = 60, repetitions = 1)
        val n = t.notes[0]
        assertEquals(listOf(Hand.LEFT, Hand.LEFT), n.graces.map { it.hand })
        assertEquals(listOf(n.timeNanos - 50 * MS, n.timeNanos - 25 * MS), n.graces.map { it.timeNanos })
    }

    @Test
    fun tripletTimesDoNotDrift() {
        val t = TimelineBuilder.build(rudiment("R L R", grid = 3), bpm = 100, repetitions = 50)
        val last = t.notes.last()
        // beat 49, third triplet: countIn + 49 beats + 2/3 beat
        val beat = 600 * MS
        assertEquals(4 * beat + 49 * beat + 2 * beat / 3, last.timeNanos)
    }

    @Test
    fun repetitionAt() {
        val t = TimelineBuilder.build(rudiment("R L R L"), bpm = 60, repetitions = 3)
        assertEquals(-1, t.repetitionAt(100 * MS))
        assertEquals(0, t.repetitionAt(4_100 * MS))
        assertEquals(2, t.repetitionAt(6_100 * MS))
        assertEquals(2, t.repetitionAt(60_000 * MS))
    }
}
