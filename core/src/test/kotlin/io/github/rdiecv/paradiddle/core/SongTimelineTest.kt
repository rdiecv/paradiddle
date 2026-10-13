// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongTimelineTest {
    private fun groove(bars: Int) = Groove(
        "g$bars", LocalizedText(mapOf("en" to "G")), 100, 80, 120, LocalizedText(mapOf("en" to "How")),
        mapOf(Instrument.KICK to List(bars * 16) { if (it == 0) Hit.NORMAL else null }),
    )

    private fun section(bars: Int, groove: Groove?) = SongSection(LocalizedText(mapOf("en" to "S")), groove, bars)

    private val sections = listOf(section(2, null), section(4, groove(1)), section(4, groove(2)))

    @Test
    fun totalStepsCountsEveryBar() {
        assertEquals((2 + 4 + 4) * 16, SongTimeline(sections).totalSteps)
        assertEquals(4 * 16, SongTimeline(sections, from = 2).totalSteps)
    }

    @Test
    fun placesStepsInSectionsBarsAndGrooves() {
        val t = SongTimeline(sections)
        assertEquals(SongPlace(0, 0, 0, true), t.placeAt(0))
        assertEquals(SongPlace(0, 1, 1, false), t.placeAt(17))
        assertEquals(SongPlace(1, 0, 0, true), t.placeAt(32))
        // A one-bar groove repeats every bar.
        assertEquals(SongPlace(1, 3, 5, false), t.placeAt(32 + 3 * 16 + 5))
        // A two-bar groove runs on into its second bar.
        assertEquals(SongPlace(2, 1, 16 + 2, false), t.placeAt(96 + 16 + 2))
        assertEquals(SongPlace(2, 2, 2, false), t.placeAt(96 + 32 + 2))
        assertNull(t.placeAt(-1))
        assertNull(t.placeAt(160))
    }

    @Test
    fun startingLaterSkipsEarlierSections() {
        val t = SongTimeline(sections, from = 1)
        assertEquals(SongPlace(1, 0, 0, true), t.placeAt(0))
        assertEquals(SongPlace(2, 0, 0, true), t.placeAt(64))
        assertEquals(64, t.startOf(2))
    }
}
