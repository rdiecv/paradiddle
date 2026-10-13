// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Synthesised sounds as mono float samples in [-1, 1]. Deterministic (fixed noise seed). The drum
 * kit sounds follow [kit], so each music style can have its own sound.
 */
class Synth(val sampleRate: Int, private val kit: KitSound = KitSound.STANDARD) {

    private fun frames(seconds: Double) = (seconds * sampleRate).toInt().coerceAtLeast(1)

    /** Adds the kit's room reflections to a dry sound, keeping the peak under 1. */
    private fun FloatArray.inRoom(): FloatArray {
        if (kit.room.isEmpty()) return this
        val delays = kit.room.map { (seconds, gain) -> (seconds * sampleRate).toInt() to gain.toFloat() }
        val out = FloatArray(size + delays.maxOf { it.first })
        for (i in indices) {
            out[i] += this[i]
            for ((d, g) in delays) out[i + d] += this[i] * g
        }
        val peak = out.maxOf { kotlin.math.abs(it) }
        if (peak > 0.98f) for (i in out.indices) out[i] *= 0.98f / peak
        return out
    }

    /** Short sine "tick" with a fast decay. */
    fun click(strong: Boolean): FloatArray {
        val freq = if (strong) 1_600.0 else 1_000.0
        val n = frames(0.03)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            (sin(2 * PI * freq * t) * exp(-t / 0.006) * 0.9).toFloat()
        }
    }

    /** Snare-like hit: decaying noise (brighter with the kit's snare brightness) plus a body tone. [gain] in (0, 1]. */
    fun snare(gain: Float, seed: Int = 1): FloatArray {
        val rnd = Random(seed)
        val n = frames(kit.snareLength)
        var last = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val raw = rnd.nextDouble() * 2 - 1
            val tone = raw * (1 - kit.snareBrightness) + (raw - last) * kit.snareBrightness
            last = raw
            val noise = tone * exp(-t / kit.snareNoiseDecay) * kit.snareNoiseGain
            val body = sin(2 * PI * kit.snareTone * t) * exp(-t / kit.snareToneDecay) * kit.snareToneGain
            ((noise + body) * gain).toFloat()
        }.inRoom()
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

    /** Bass drum: a sine that drops onto the kit's low pitch, with a beater click. */
    fun kick(): FloatArray {
        val n = frames(kit.kickLength)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            phase += 2 * PI * (kit.kickLow + kit.kickSweep * exp(-t / kit.kickSweepTime)) / sampleRate
            val body = sin(phase) * exp(-t / kit.kickDecay) * (1 - kit.kickBeater)
            val beater = sin(2 * PI * 1_200 * t) * exp(-t / 0.003) * kit.kickBeater
            (body + beater).toFloat()
        }.inRoom()
    }

    /** Hi-hat: high-passed noise (darker with the kit's hi-hat darkness), short when closed and ringing when [open]. */
    fun hihat(open: Boolean): FloatArray {
        val rnd = Random(if (open) 12 else 11)
        val decay = if (open) kit.hatOpenDecay else kit.hatClosedDecay
        val n = frames(if (open) 0.35 else 0.08)
        var last = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val noise = rnd.nextDouble() * 2 - 1
            val high = (noise - last) / 2 * (1 - kit.hatDarkness) + noise / 2 * kit.hatDarkness
            last = noise
            (high * exp(-t / decay) * 0.8).toFloat()
        }.inRoom()
    }

    /** Ride cymbal: inharmonic partials plus a little noise, long decay. */
    fun ride(): FloatArray {
        val rnd = Random(13)
        val partials = doubleArrayOf(3_150.0, 4_480.0, 5_920.0, 7_340.0).map { it * kit.ridePitch }
        val n = frames(0.8)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val ping = partials.sumOf { sin(2 * PI * it * t) } / partials.size * exp(-t / kit.rideDecay) * 0.5
            val wash = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.08) * kit.rideWash
            (ping + wash).toFloat()
        }.inRoom()
    }

    /** Cowbell: two detuned tones with a quick decay. */
    fun cowbell(): FloatArray {
        val n = frames(0.25)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            ((sin(2 * PI * kit.bellLow * t) + sin(2 * PI * kit.bellHigh * t)) * exp(-t / kit.bellDecay) * 0.45).toFloat()
        }.inRoom()
    }

    /** Cross-stick (side stick): a dry, woody click. */
    fun crossStick(): FloatArray {
        val rnd = Random(14)
        val n = frames(0.06)
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val tone = sin(2 * PI * kit.stickTone * t) * exp(-t / kit.stickDecay) * 0.5
            val noise = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.004) * 0.4
            (tone + noise).toFloat()
        }.inRoom()
    }

    /** Low tom: a sine that falls slightly in pitch, with a soft attack noise. */
    fun tom(): FloatArray {
        val rnd = Random(15)
        val n = frames(0.35)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            phase += 2 * PI * (kit.tomLow + kit.tomLow / 3 * exp(-t / 0.05)) / sampleRate
            val body = sin(phase) * exp(-t / kit.tomDecay) * 0.75
            val attack = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.01) * 0.2
            (body + attack).toFloat()
        }.inRoom()
    }

    /** Crash cymbal: bright, high-passed noise with a long wash. */
    fun crash(): FloatArray {
        val rnd = Random(16)
        val n = frames(1.4)
        var last = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / sampleRate
            val noise = rnd.nextDouble() * 2 - 1
            val high = (noise - last) / 2
            last = noise
            val body = high * (0.75 * exp(-t / 0.45) + 0.25 * exp(-t / 0.05))
            val shimmer = sin(2 * PI * 5_300 * t) * exp(-t / 0.3) * 0.08
            (body + shimmer).toFloat()
        }.inRoom()
    }
}
