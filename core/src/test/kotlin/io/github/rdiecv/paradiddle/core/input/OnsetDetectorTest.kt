// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

class OnsetDetectorTest {
    private val rate = 48_000
    private fun ms(t: Double) = (t * rate / 1000).toInt()

    /** A stick hit: a noise burst decaying over ~30 ms, starting at [atMs]. */
    private fun FloatArray.hit(atMs: Double, amplitude: Float, seed: Int = 1): FloatArray {
        val r = Random(seed)
        val start = ms(atMs)
        for (i in 0 until ms(120.0)) {
            if (start + i >= size) break
            this[start + i] += amplitude * (r.nextFloat() * 2 - 1) * exp(-i / (0.010 * rate)).toFloat()
        }
        return this
    }

    private fun noise(lengthMs: Double, level: Float) = Random(9).let { r -> FloatArray(ms(lengthMs)) { level * (r.nextFloat() * 2 - 1) } }

    private fun detect(signal: FloatArray, chunk: Int = 192): List<OnsetDetector.Onset> {
        val d = OnsetDetector(rate)
        val out = ArrayList<OnsetDetector.Onset>()
        var i = 0
        while (i < signal.size) {
            val n = minOf(chunk, signal.size - i)
            out += d.feed(signal.copyOfRange(i, i + n))
            i += n
        }
        return out
    }

    private fun assertAt(expectedMs: List<Double>, onsets: List<OnsetDetector.Onset>) {
        assertEquals(onsets.toString(), expectedMs.size, onsets.size)
        expectedMs.zip(onsets).forEach { (e, o) -> assertTrue("$e ms vs frame ${o.frame}", abs(o.frame - ms(e)) <= ms(2.0)) }
    }

    @Test
    fun silenceAndBackgroundNoiseGiveNothing() {
        assertEquals(0, detect(FloatArray(ms(1000.0))).size)
        assertEquals(0, detect(noise(1000.0, 0.005f)).size)
    }

    @Test
    fun findsEachHitOnTime() {
        val s = noise(2000.0, 0.003f).hit(200.0, 0.5f).hit(700.0, 0.3f, 2).hit(1300.0, 0.6f, 3)
        assertAt(listOf(200.0, 700.0, 1300.0), detect(s))
    }

    @Test
    fun chunkSizeDoesNotMatter() {
        val s = noise(1500.0, 0.003f).hit(300.0, 0.5f).hit(900.0, 0.4f, 2)
        assertEquals(detect(s, 192).map { it.frame }, detect(s, 37).map { it.frame })
    }

    @Test
    fun fastSixteenthsAt240BpmAreAllFound() {
        val s = noise(1500.0, 0.003f)
        val times = (0 until 16).map { 100.0 + it * 62.5 }
        times.forEachIndexed { i, t -> s.hit(t, if (i % 4 == 0) 0.6f else 0.3f, i + 1) }
        assertAt(times, detect(s))
    }

    @Test
    fun aFlamIsOneHitAtItsGraceNote() {
        val s = noise(1000.0, 0.003f).hit(300.0, 0.15f).hit(320.0, 0.6f, 2)
        assertAt(listOf(300.0), detect(s))
    }

    @Test
    fun louderHitsHaveAHigherLevel() {
        val s = noise(1500.0, 0.003f).hit(300.0, 0.2f).hit(900.0, 0.6f, 2)
        val (soft, loud) = detect(s)
        assertTrue(loud.level > soft.level * 2)
        assertTrue(loud.level <= 1f)
    }
}
