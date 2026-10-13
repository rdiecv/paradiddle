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

    /** Bass drum: a sine that drops from 120 to 50 Hz, with a short beater click. */
    fun kick(): FloatArray {
        val n = frames(0.3)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            phase += 2 * PI * (50 + 70 * exp(-t / 0.04)) / sampleRate
            val body = sin(phase) * exp(-t / 0.12) * 0.85
            val beater = sin(2 * PI * 1_200 * t) * exp(-t / 0.003) * 0.15
            (body + beater).toFloat()
        }
    }

    /** Hi-hat: high-passed noise, short when closed and ringing when [open]. */
    fun hihat(open: Boolean): FloatArray {
        val rnd = Random(if (open) 12 else 11)
        val decay = if (open) 0.12 else 0.018
        val n = frames(if (open) 0.35 else 0.08)
        var last = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val noise = rnd.nextDouble() * 2 - 1
            val high = (noise - last) / 2
            last = noise
            (high * exp(-t / decay) * 0.8).toFloat()
        }
    }

    /** Ride cymbal: inharmonic partials plus a little noise, long decay. */
    fun ride(): FloatArray {
        val rnd = Random(13)
        val partials = doubleArrayOf(3_150.0, 4_480.0, 5_920.0, 7_340.0)
        val n = frames(0.8)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val ping = partials.sumOf { sin(2 * PI * it * t) } / partials.size * exp(-t / 0.35) * 0.5
            val wash = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.08) * 0.2
            (ping + wash).toFloat()
        }
    }

    /** Cowbell: two detuned tones with a quick decay. */
    fun cowbell(): FloatArray {
        val n = frames(0.25)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            ((sin(2 * PI * 545 * t) + sin(2 * PI * 815 * t)) * exp(-t / 0.06) * 0.45).toFloat()
        }
    }

    /** Cross-stick (side stick): a dry, woody click. */
    fun crossStick(): FloatArray {
        val rnd = Random(14)
        val n = frames(0.06)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val tone = sin(2 * PI * 1_750 * t) * exp(-t / 0.012) * 0.5
            val noise = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.004) * 0.4
            (tone + noise).toFloat()
        }
    }

    /** Low tom: a sine that falls slightly in pitch, with a soft attack noise. */
    fun tom(): FloatArray {
        val rnd = Random(15)
        val n = frames(0.35)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            phase += 2 * PI * (105 + 35 * exp(-t / 0.05)) / sampleRate
            val body = sin(phase) * exp(-t / 0.15) * 0.75
            val attack = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.01) * 0.2
            (body + attack).toFloat()
        }
    }
}
