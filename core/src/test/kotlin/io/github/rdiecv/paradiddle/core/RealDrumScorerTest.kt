// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealDrumScorerTest {
    // 60 BPM 16ths: slot 250 ms, window 100 ms.
    private fun timeline(strokes: String) = TimelineBuilder.build(rudiment(strokes), bpm = 60, repetitions = 1)

    @Test
    fun handsAreNotChecked() {
        val t = timeline("R L R L")
        val s = Scorer(t, realDrums = true)
        t.notes.forEach { assertEquals(Judgement.PERFECT, s.hit(it.timeNanos + 5 * MS, 0.5f)!!.judgement) }
        val r = s.finish()
        assertEquals(100.0, r.accuracy, 0.001)
        assertEquals(0, r.wrongHand)
    }

    @Test
    fun feedbackUsesTheNotesHandForTheLane() {
        val t = timeline("R L R L")
        val s = Scorer(t, realDrums = true)
        assertEquals(Hand.RIGHT, s.hit(t.notes[0].timeNanos, 0.5f)!!.hand)
        assertEquals(Hand.LEFT, s.hit(t.notes[1].timeNanos, 0.5f)!!.hand)
    }

    @Test
    fun graceNotesAreNotScoredAndTheirHitsAreAbsorbed() {
        // Flams: a grace 25 ms before each main stroke.
        val t = timeline("lR rL lR rL")
        val s = Scorer(t, realDrums = true)
        t.notes.forEach { n ->
            // MIDI hears both strokes; the first is judged against the grace's time.
            assertEquals(Judgement.PERFECT, s.hit(n.graces[0].timeNanos, 0.2f)!!.judgement)
            s.hit(n.timeNanos, 0.7f)
        }
        val r = s.finish()
        assertEquals(100.0, r.accuracy, 0.001)
        assertEquals(0, r.extra)
        assertEquals(0, r.graceHit + r.graceMissed)
    }

    @Test
    fun buzzStrokesAbsorbTheirBounces() {
        val t = timeline("Rz:2 Lz:2")
        val s = Scorer(t, realDrums = true)
        t.notes.forEach { n -> (0 until 8).forEach { s.hit(n.timeNanos + it * 40 * MS, 0.4f) } }
        val r = s.finish()
        assertEquals(0, r.extra)
        assertEquals(2, r.perfect)
    }

    @Test
    fun wrongPadIsJudgedWhenThePadIsKnown() {
        val base = timeline("R L")
        val t = base.copy(notes = base.notes.mapIndexed { i, n -> n.copy(pad = if (i == 0) Instrument.KICK else Instrument.SNARE) })
        val s = Scorer(t, realDrums = true)
        assertEquals(Judgement.WRONG_PAD, s.hit(t.notes[0].timeNanos, 0.5f, Instrument.SNARE)!!.judgement)
        assertEquals(Judgement.PERFECT, s.hit(t.notes[1].timeNanos, 0.5f, Instrument.CROSS_STICK)!!.judgement)
    }

    @Test
    fun simultaneousNotesMatchByPad() {
        val base = timeline("R")
        val n = base.notes[0]
        val t = base.copy(notes = listOf(n.copy(pad = Instrument.HIHAT), n.copy(index = 1, pad = Instrument.KICK)))
        val s = Scorer(t, realDrums = true)
        assertEquals(1, s.hit(n.timeNanos, 0.5f, Instrument.KICK)!!.noteIndex)
        assertEquals(0, s.hit(n.timeNanos + 3 * MS, 0.5f, Instrument.HIHAT)!!.noteIndex)
        assertEquals(100.0, s.finish().accuracy, 0.001)
    }

    @Test
    fun accentsAreCheckedAgainstTheSoftNotes() {
        val t = timeline("R> L R L R> L R L")
        val s = Scorer(t, realDrums = true)
        val levels = listOf(0.8f, 0.3f, 0.32f, 0.28f, 0.33f, 0.31f, 0.3f, 0.6f) // 2nd accent too soft, last soft note too loud
        t.notes.zip(levels).forEach { (n, l) -> s.hit(n.timeNanos, l) }
        val a = s.finish().accents!!
        assertEquals(8, a.total)
        assertEquals(6, a.right)
    }

    @Test
    fun noAccentCheckWithoutBothKindsOfNotes() {
        val t = timeline("R L R L")
        val s = Scorer(t, realDrums = true)
        t.notes.forEach { s.hit(it.timeNanos, 0.5f) }
        assertNull(s.finish().accents)
    }

    @Test
    fun screenModeHasNoAccentResult() {
        val t = timeline("R> L R L")
        val s = Scorer(t)
        t.notes.forEach { s.tap(it.hand, it.timeNanos) }
        assertNull(s.finish().accents)
    }
}
