// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeNotationTest {
    @Test
    fun parsesPlainStrokes() {
        val s = StrokeNotation.parse("R L R R")
        assertEquals(listOf(Hand.RIGHT, Hand.LEFT, Hand.RIGHT, Hand.RIGHT), s.map { it.hand })
        assertTrue(s.all { it.slots == 1 && !it.accent && !it.buzz && it.graces.isEmpty() })
    }

    @Test
    fun parsesMarksGracesDurationsAndRests() {
        val s = StrokeNotation.parse("llR>:4 Rz:2 -:3 rL")
        assertEquals(Stroke(Hand.RIGHT, 4, accent = true, graces = listOf(Hand.LEFT, Hand.LEFT)), s[0])
        assertEquals(Stroke(Hand.RIGHT, 2, buzz = true), s[1])
        assertEquals(Stroke(null, 3), s[2])
        assertEquals(Stroke(Hand.LEFT, graces = listOf(Hand.RIGHT)), s[3])
    }

    @Test
    fun formatRoundTrips() {
        val text = "llR>:4 Rz:2 -:3 rL L"
        assertEquals(text, StrokeNotation.format(StrokeNotation.parse(text)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownToken() {
        StrokeNotation.parse("R X")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroDuration() {
        StrokeNotation.parse("R:0")
    }
}
