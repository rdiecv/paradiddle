// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTest {
    // 60 BPM 16ths: slot 250 ms → window min(100, 112) = 100 ms.
    private fun singles(reps: Int = 1) = TimelineBuilder.build(rudiment("R L R L"), bpm = 60, repetitions = reps)

    private fun playAll(scorer: Scorer, t: Timeline, offset: Long = 0) {
        t.notes.forEach { n ->
            n.graces.forEach { g -> scorer.tap(g.hand, g.timeNanos + offset) }
            scorer.tap(n.hand, n.timeNanos + offset)
        }
    }

    @Test
    fun perfectPlayScoresHundred() {
        val t = singles(2)
        val s = Scorer(t)
        playAll(s, t)
        val r = s.finish()
        assertEquals(100.0, r.accuracy, 0.001)
        assertEquals(8, r.perfect)
        assertTrue(r.isClean)
    }

    @Test
    fun judgesByOffset() {
        val t = singles()
        val s = Scorer(t)
        val n = t.notes
        assertEquals(Judgement.PERFECT, s.tap(Hand.RIGHT, n[0].timeNanos + 20 * MS).judgement)
        assertEquals(Judgement.GOOD, s.tap(Hand.LEFT, n[1].timeNanos - 40 * MS).judgement)
        assertEquals(Judgement.OK, s.tap(Hand.RIGHT, n[2].timeNanos + 90 * MS).judgement)
        val f = s.tap(Hand.LEFT, n[3].timeNanos - 30 * MS)
        assertEquals(Judgement.GOOD, f.judgement)
        assertEquals(-30 * MS, f.offsetNanos)
        val r = s.finish()
        assertEquals((1.0 + 0.75 + 0.5 + 0.75) / 4 * 100, r.accuracy, 0.001)
        assertEquals((20 - 40 + 90 - 30) / 4.0, r.meanOffsetMs, 0.001)
    }

    @Test
    fun wrongHandAndExtraAndMissed() {
        val t = singles()
        val s = Scorer(t)
        val n = t.notes
        assertEquals(Judgement.WRONG_HAND, s.tap(Hand.LEFT, n[0].timeNanos).judgement)
        assertEquals(Judgement.EXTRA, s.tap(Hand.LEFT, n[0].timeNanos + 130 * MS).judgement)
        // n[1] (L) never played
        val missed = s.advanceTo(n[1].timeNanos + 101 * MS)
        assertEquals(listOf(Judgement.MISSED), missed.map { it.judgement })
        s.tap(Hand.RIGHT, n[2].timeNanos)
        s.tap(Hand.LEFT, n[3].timeNanos)
        val r = s.finish()
        assertEquals(1, r.wrongHand)
        assertEquals(1, r.extra)
        assertEquals(1, r.missed)
        assertEquals((0.25 - 0.25 + 0 + 1 + 1) / 4 * 100, r.accuracy, 0.001)
    }

    @Test
    fun liveAccuracyCountsOnlyJudgedNotes() {
        val t = singles()
        val s = Scorer(t)
        assertEquals(100.0, s.liveAccuracy, 0.0)
        s.tap(Hand.RIGHT, t.notes[0].timeNanos + 90 * MS)
        assertEquals(50.0, s.liveAccuracy, 0.001)
    }

    @Test
    fun flamGraceIsTolerant() {
        val t = TimelineBuilder.build(rudiment("lR rL", grid = 1), bpm = 60, repetitions = 1)
        val s = Scorer(t)
        val n = t.notes
        // Grace 120 ms early (not at its nominal 25 ms) still counts.
        assertEquals(Judgement.GRACE_HIT, s.tap(Hand.LEFT, n[0].timeNanos - 120 * MS).judgement)
        assertEquals(Judgement.PERFECT, s.tap(Hand.RIGHT, n[0].timeNanos).judgement)
        // Flat flam (simultaneous) is accepted too.
        assertEquals(Judgement.PERFECT, s.tap(Hand.LEFT, n[1].timeNanos).judgement)
        assertEquals(Judgement.GRACE_HIT, s.tap(Hand.RIGHT, n[1].timeNanos + 5 * MS).judgement)
        assertEquals(100.0, s.finish().accuracy, 0.001)
    }

    @Test
    fun missingGraceCostsHalfPoint() {
        val t = TimelineBuilder.build(rudiment("lR", grid = 1), bpm = 60, repetitions = 1)
        val s = Scorer(t)
        s.tap(Hand.RIGHT, t.notes[0].timeNanos)
        val r = s.finish()
        assertEquals(1, r.graceMissed)
        assertEquals(1.0 / 1.5 * 100, r.accuracy, 0.001)
    }

    @Test
    fun flamTapPrefersEarliestSameHandNote() {
        // lR> R rL> L at 120 BPM: the tap R before the flam L must not steal the grace.
        val t = TimelineBuilder.build(rudiment("lR> R rL> L"), bpm = 120, repetitions = 4)
        val s = Scorer(t)
        playAll(s, t)
        val r = s.finish()
        assertEquals(100.0, r.accuracy, 0.001)
        assertEquals(0, r.wrongHand + r.extra + r.missed + r.graceMissed)
    }

    @Test
    fun dragNeedsBothGraces() {
        val t = TimelineBuilder.build(rudiment("llR", grid = 1), bpm = 60, repetitions = 1)
        val s = Scorer(t)
        s.tap(Hand.LEFT, t.notes[0].timeNanos - 40 * MS)
        s.tap(Hand.RIGHT, t.notes[0].timeNanos)
        val r = s.finish()
        assertEquals(1, r.graceHit)
        assertEquals(1, r.graceMissed)
    }

    @Test
    fun everyCatalogRudimentScoresHundredWhenPlayedExactly() {
        val text = java.io.File(System.getProperty("rudimentsJson")).readText()
        RudimentCatalog.parse(text).forEach { r ->
            listOf(60, 120, 200).forEach { bpm ->
                val t = TimelineBuilder.build(r, bpm, repetitions = 3)
                val s = Scorer(t)
                playAll(s, t)
                assertEquals("${r.id} @ $bpm", 100.0, s.finish().accuracy, 0.001)
            }
        }
    }

    @Test
    fun suggestedBpm() {
        assertEquals(104, Scoring.suggestedBpm(100, 95.0))
        assertEquals(100, Scoring.suggestedBpm(100, 80.0))
        assertEquals(92, Scoring.suggestedBpm(100, 50.0))
        assertEquals(240, Scoring.suggestedBpm(240, 99.0))
        assertEquals(40, Scoring.suggestedBpm(44, 10.0))
    }
}
