// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import io.github.rdiecv.paradiddle.core.StyleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrooveSoundTest {
    private val sound = GrooveSound(Synth(48_000))

    private val groove = StyleCatalog.parse(
        """[{"id":"s","family":"rock","name":"S","kit":10,"setup":{"en":"Setup"},"beats":[
            {"id":"x","name":{"en":"X"},"bpm":120,"minBpm":60,"maxBpm":180,"how":{"en":"How"},
             "tracks":{"hihat":"x.x.x.x.x.x.x.o.","snare":"..g.X.......X...","kick":"x.......x......."}}]}]""",
    ).single().grooves.single()

    @Test
    fun oneVoicePerHitPerLoop() {
        assertEquals(13, sound.voices(groove, 120, loops = 1).size)
        assertEquals(39, sound.voices(groove, 120, loops = 3).size)
    }

    @Test
    fun hitsStartOnTheirSixteenth() {
        // 120 BPM: a 16th lasts 125 ms = 6000 frames; a bar = 96000 frames.
        val starts = sound.voices(groove, 120, loops = 2).map { it.startFrame }.toSet()
        assertEquals(setOf(0L, 12_000L, 24_000L, 36_000L, 48_000L, 60_000L, 72_000L, 84_000L), starts.filter { it < 96_000 }.toSet())
        assertTrue(96_000L in starts)
        assertTrue(96_000L + 84_000L in starts)
    }

    @Test
    fun accentsAreLouderThanGhostNotes() {
        val snare = sound.voices(groove, 120, loops = 1).filter { it.startFrame == 12_000L || it.startFrame == 24_000L }
        val ghost = snare.first { it.startFrame == 12_000L && it.gain < 0.5f }
        val accent = snare.maxBy { it.gain }
        assertTrue(accent.gain > ghost.gain * 2)
    }

    @Test
    fun lengthCoversAllLoops() {
        assertEquals(96_000L * 4, sound.lengthFrames(groove, 120, loops = 4))
    }

    @Test
    fun loopsForFillsTheRequestedTime() {
        // One bar at 120 BPM lasts 2 s.
        assertEquals(150, sound.loopsFor(groove, 120, seconds = 300))
        assertEquals(1, sound.loopsFor(groove, 120, seconds = 1))
    }

    @Test
    fun stepAtFollowsSessionTime() {
        assertEquals(0, sound.stepAt(groove, 120, 0L))
        assertEquals(1, sound.stepAt(groove, 120, 125_000_000L))
        assertEquals(15, sound.stepAt(groove, 120, 1_999_000_000L))
        assertEquals(0, sound.stepAt(groove, 120, 2_000_000_000L))
        assertEquals(-1, sound.stepAt(groove, 120, -1L))
    }

    @Test
    fun positionAtIncludesTheFractionOfTheStep() {
        assertEquals(0.5, sound.positionAt(groove, 120, 62_500_000L), 1e-9)
        // Step 17 and a half wraps to 1.5 in a 16-step groove.
        assertEquals(1.5, sound.positionAt(groove, 120, 17 * 125_000_000L + 62_500_000L), 1e-9)
        assertEquals(-1.0, sound.positionAt(groove, 120, -1L), 1e-9)
    }
}
