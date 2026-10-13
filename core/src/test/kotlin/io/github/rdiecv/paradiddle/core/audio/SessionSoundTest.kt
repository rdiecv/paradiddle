// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import io.github.rdiecv.paradiddle.core.Hand
import io.github.rdiecv.paradiddle.core.TimelineBuilder
import io.github.rdiecv.paradiddle.core.rudiment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSoundTest {
    private val sound = SessionSound(Synth(48_000))
    private fun pan(hand: Hand) = if (hand == Hand.RIGHT) 0.5f else -0.5f

    @Test
    fun eachHandSitsOnItsSide() {
        val t = TimelineBuilder.build(rudiment("R L R R L R L L"), bpm = 60, repetitions = 1)
        val voices = sound.voices(t, clickGain = 0f, patternGain = 1f, handPan = 0.5f)
        t.notes.forEach { n ->
            assertEquals(pan(n.hand), voices.single { it.startFrame == sound.frameOf(n.timeNanos) }.pan, 0f)
        }
    }

    @Test
    fun graceNotesSitOnTheirOwnHandsSide() {
        val t = TimelineBuilder.build(rudiment("llR rrL", grid = 1), bpm = 60, repetitions = 1)
        val voices = sound.voices(t, clickGain = 0f, patternGain = 1f, handPan = 0.5f)
        val graces = t.notes.flatMap { it.graces }
        assertTrue(graces.isNotEmpty())
        graces.forEach { g ->
            assertEquals(pan(g.hand), voices.single { it.startFrame == sound.frameOf(g.timeNanos) }.pan, 0f)
        }
    }

    @Test
    fun clicksAndUnseparatedHandsStayCentred() {
        val t = TimelineBuilder.build(rudiment("R L R R L R L L"), bpm = 60, repetitions = 1)
        assertTrue(sound.voices(t, clickGain = 1f, patternGain = 1f).all { it.pan == 0f })
        assertTrue(sound.voices(t, clickGain = 1f, patternGain = 0f, handPan = 0.5f).all { it.pan == 0f })
    }
}
