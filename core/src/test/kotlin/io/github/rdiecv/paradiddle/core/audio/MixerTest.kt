// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixerTest {
    private val sound = floatArrayOf(0.5f, 0.25f, 0.125f)

    /** Left channel of an interleaved stereo buffer. */
    private fun FloatArray.left() = FloatArray(size / 2) { this[it * 2] }

    @Test
    fun placesVoiceOnExactFrameAcrossBuffers() {
        val mixer = Mixer(listOf(Voice(6, sound)))
        val a = FloatArray(8)
        val b = FloatArray(8)
        mixer.render(0, a)
        mixer.render(4, b)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 0f), a.left(), 0f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0.5f, 0.25f), b.left(), 0f)
        val c = FloatArray(8)
        mixer.render(8, c)
        assertArrayEquals(floatArrayOf(0.125f, 0f, 0f, 0f), c.left(), 0f)
    }

    @Test
    fun sumsAndClips() {
        val mixer = Mixer(listOf(Voice(0, floatArrayOf(0.8f, 0.1f)), Voice(0, floatArrayOf(0.8f, 0.1f), gain = 0.5f)))
        val out = FloatArray(4)
        mixer.render(0, out)
        assertEquals(1f, out[0], 0f)
        assertEquals(1f, out[1], 0f)
        assertEquals(0.15f, out[2], 1e-6f)
        assertEquals(0.15f, out[3], 1e-6f)
    }

    @Test
    fun centredVoicePlaysFullOnBothSides() {
        val out = FloatArray(2)
        Mixer(listOf(Voice(0, floatArrayOf(0.5f)))).render(0, out)
        assertArrayEquals(floatArrayOf(0.5f, 0.5f), out, 0f)
    }

    @Test
    fun panTurnsTheFarSideDown() {
        val out = FloatArray(4)
        Mixer(listOf(Voice(0, floatArrayOf(0.8f), pan = 0.5f), Voice(1, floatArrayOf(0.8f), pan = -1f))).render(0, out)
        assertArrayEquals(floatArrayOf(0.4f, 0.8f, 0.8f, 0f), out, 1e-6f)
    }

    @Test
    fun endFrame() {
        assertEquals(9L, Mixer(listOf(Voice(6, sound), Voice(2, sound))).endFrame)
    }

    @Test
    fun synthSoundsAreFiniteAndInRange() {
        val synth = Synth(48_000)
        listOf(
            synth.click(true), synth.click(false), synth.snare(1f), synth.snare(0.3f), synth.buzz(1f, 0.25),
            synth.kick(), synth.hihat(open = false), synth.hihat(open = true), synth.ride(), synth.cowbell(),
            synth.crossStick(), synth.tom(), synth.crash(),
        )
            .forEach { s ->
                assertTrue(s.isNotEmpty())
                assertTrue(s.all { it.isFinite() && it in -1f..1f })
            }
    }

    @Test
    fun everyKitSoundIsFiniteAndInRange() {
        KitSound.PRESETS.forEach { (name, kit) ->
            val synth = Synth(48_000, kit)
            listOf(
                synth.snare(1f), synth.kick(), synth.hihat(open = false), synth.hihat(open = true), synth.ride(),
                synth.cowbell(), synth.crossStick(), synth.tom(), synth.crash(),
            ).forEach { s -> assertTrue(name, s.isNotEmpty() && s.all { it.isFinite() && it in -1f..1f }) }
        }
    }

    @Test
    fun stylesSoundDifferent() {
        val reggae = Synth(48_000, KitSound.PRESETS.getValue("reggae")).kick()
        val metal = Synth(48_000, KitSound.PRESETS.getValue("metal")).kick()
        assertTrue("a reggae kick rings longer than a metal kick", reggae.size > metal.size * 2)
    }
}
