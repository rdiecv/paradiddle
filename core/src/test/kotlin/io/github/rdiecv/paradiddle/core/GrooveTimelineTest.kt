// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrooveTimelineTest {
    private fun hits(s: String): List<Hit?> = s.map {
        when (it) {
            'x' -> Hit.NORMAL
            'X' -> Hit.ACCENT
            'g' -> Hit.GHOST
            'o' -> Hit.OPEN
            else -> null
        }
    }

    private val rock = Groove(
        id = "rock", name = LocalizedText(mapOf("en" to "Rock")), bpm = 100, minBpm = 60, maxBpm = 160,
        how = LocalizedText(mapOf("en" to "")),
        tracks = mapOf(
            Instrument.HIHAT to hits("x.x.x.x.x.x.x.x."),
            Instrument.SNARE to hits("....X.......X..."),
            Instrument.KICK to hits("x.......x......."),
        ),
    )

    @Test
    fun perPadHasANoteForEveryHit() {
        val t = GrooveTimeline.build(rock, bpm = 60, loops = 2, perPad = true)
        assertEquals(2 * (8 + 2 + 2), t.notes.size)
        assertEquals(4 * NANOS_PER_SECOND, t.countInNanos)
        val first = t.notes.filter { it.timeNanos == t.countInNanos }.map { it.pad }.toSet()
        assertEquals(setOf(Instrument.HIHAT, Instrument.KICK), first)
        val snare = t.notes.first { it.pad == Instrument.SNARE }
        assertEquals(t.countInNanos + NANOS_PER_SECOND, snare.timeNanos)
        assertTrue(snare.accent)
        assertEquals(t.notes.indices.toList(), t.notes.map { it.index })
    }

    @Test
    fun forTheMicrophoneHitsOnTheSameStepAreOneNote() {
        val t = GrooveTimeline.build(rock, bpm = 60, loops = 1, perPad = false)
        assertEquals(8, t.notes.size)
        assertTrue(t.notes.all { it.pad == null })
        assertEquals(listOf(false, false, true, false, false, false, true, false), t.notes.map { it.accent })
    }

    @Test
    fun clicksOnEveryBeatAndTheBarStartIsStrong() {
        val t = GrooveTimeline.build(rock, bpm = 120, loops = 2, perPad = true)
        assertEquals(4 + 8, t.clicks.size)
        assertEquals(listOf(true, false, false, false), t.clicks.drop(4).take(4).map { it.strong })
        assertTrue(t.endNanos > t.notes.last().timeNanos)
        assertEquals(-1, t.repetitionAt(0))
        assertEquals(1, t.repetitionAt(t.countInNanos + t.patternNanos + 1))
    }
}
