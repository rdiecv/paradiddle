// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixerTest {
    private val sound = floatArrayOf(0.5f, 0.25f, 0.125f)

    @Test
    fun placesVoiceOnExactFrameAcrossBuffers() {
        val mixer = Mixer(listOf(Voice(6, sound)))
        val a = FloatArray(4)
        val b = FloatArray(4)
        mixer.render(0, a)
        mixer.render(4, b)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 0f), a, 0f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0.5f, 0.25f), b, 0f)
        val c = FloatArray(4)
        mixer.render(8, c)
        assertArrayEquals(floatArrayOf(0.125f, 0f, 0f, 0f), c, 0f)
    }

    @Test
    fun sumsAndClips() {
        val mixer = Mixer(listOf(Voice(0, floatArrayOf(0.8f, 0.1f)), Voice(0, floatArrayOf(0.8f, 0.1f), gain = 0.5f)))
        val out = FloatArray(2)
        mixer.render(0, out)
        assertEquals(1f, out[0], 0f)
        assertEquals(0.15f, out[1], 1e-6f)
    }

    @Test
    fun endFrame() {
        assertEquals(9L, Mixer(listOf(Voice(6, sound), Voice(2, sound))).endFrame)
    }

    @Test
    fun synthSoundsAreFiniteAndInRange() {
        val synth = Synth(48_000)
        listOf(synth.click(true), synth.click(false), synth.snare(1f), synth.snare(0.3f), synth.buzz(1f, 0.25))
            .forEach { s ->
                assertTrue(s.isNotEmpty())
                assertTrue(s.all { it.isFinite() && it in -1f..1f })
            }
    }
}
