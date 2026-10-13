// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Synthesised sounds as mono float samples in [-1, 1]. Deterministic (fixed noise seed). */
class Synth(val sampleRate: Int) {

    private fun frames(seconds: Double) = (seconds * sampleRate).toInt().coerceAtLeast(1)

    /** Short sine "tick" with a fast decay. */
    fun click(strong: Boolean): FloatArray {
        val freq = if (strong) 1_600.0 else 1_000.0
        val n = frames(0.03)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            (sin(2 * PI * freq * t) * exp(-t / 0.006) * 0.9).toFloat()
        }
    }

    /** Snare-like hit: decaying noise plus a low body tone. [gain] in (0, 1]. */
    fun snare(gain: Float, seed: Int = 1): FloatArray {
        val rnd = Random(seed)
        val n = frames(0.18)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val noise = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.045) * 0.6
            val body = sin(2 * PI * 190 * t) * exp(-t / 0.03) * 0.4
            ((noise + body) * gain).toFloat()
        }
    }

    /** Buzz stroke: quick soft bounces every 25 ms for [durationSeconds]. */
    fun buzz(gain: Float, durationSeconds: Double): FloatArray {
        val rnd = Random(7)
        val n = frames(durationSeconds + 0.05)
        val bounce = 0.025
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            if (t > durationSeconds + 0.02) return@FloatArray 0f
            val local = t % bounce
            val envelope = exp(-local / 0.006) * (if (t < bounce) 1.0 else 0.55)
            ((rnd.nextDouble() * 2 - 1) * envelope * 0.6 * gain).toFloat()
        }
    }
}
