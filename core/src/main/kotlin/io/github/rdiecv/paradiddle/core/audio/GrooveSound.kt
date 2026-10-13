// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.Hit
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND

/** Turns a [Groove] into mixer voices, looped a number of times. */
class GrooveSound(private val synth: Synth) {
    private val closedHiHat = synth.hihat(open = false)
    private val openHiHat = synth.hihat(open = true)
    private val samples = mapOf(
        Instrument.RIDE to synth.ride(),
        Instrument.COWBELL to synth.cowbell(),
        Instrument.SNARE to synth.snare(1f),
        Instrument.CROSS_STICK to synth.crossStick(),
        Instrument.TOM to synth.tom(),
        Instrument.KICK to synth.kick(),
    )

    /** Balances the kit: cymbals sit under the drums. */
    private fun level(instrument: Instrument): Float = when (instrument) {
        Instrument.HIHAT, Instrument.RIDE -> 0.5f
        Instrument.COWBELL -> 0.6f
        else -> 1f
    }

    private fun gain(hit: Hit): Float = when (hit) {
        Hit.ACCENT -> 1f
        Hit.NORMAL, Hit.OPEN -> 0.65f
        Hit.GHOST -> 0.3f
    }

    /** Frame where 16th number [step] (counted from the start of the first loop) begins. */
    private fun frameOfStep(step: Long, bpm: Int): Long = step * 15L * synth.sampleRate / bpm

    private fun stepNanos(bpm: Int): Long = 15L * NANOS_PER_SECOND / bpm

    fun voices(groove: Groove, bpm: Int, loops: Int): List<Voice> {
        val out = ArrayList<Voice>()
        repeat(loops) { loop ->
            groove.tracks.forEach { (instrument, hits) ->
                hits.forEachIndexed { step, hit ->
                    if (hit == null) return@forEachIndexed
                    val sample = when {
                        instrument != Instrument.HIHAT -> samples.getValue(instrument)
                        hit == Hit.OPEN -> openHiHat
                        else -> closedHiHat
                    }
                    val frame = frameOfStep(loop.toLong() * groove.steps + step, bpm)
                    out += Voice(frame, sample, gain(hit) * level(instrument))
                }
            }
        }
        return out
    }

    /** Frames from the first step to the end of the last loop (the tail of the last hit is not included). */
    fun lengthFrames(groove: Groove, bpm: Int, loops: Int): Long = frameOfStep(loops.toLong() * groove.steps, bpm)

    /** How many loops are needed to play for about [seconds] (at least one). */
    fun loopsFor(groove: Groove, bpm: Int, seconds: Int): Int {
        val loopNanos = stepNanos(bpm) * groove.steps
        return (seconds * NANOS_PER_SECOND / loopNanos).toInt().coerceAtLeast(1)
    }

    /** Step of the groove heard at [sessionNanos] since playback start, or -1 before it starts. */
    fun stepAt(groove: Groove, bpm: Int, sessionNanos: Long): Int =
        if (sessionNanos < 0) -1 else ((sessionNanos / stepNanos(bpm)) % groove.steps).toInt()

    /** Like [stepAt], plus how far into that step we are (e.g. 3.25), or -1.0 before playback starts. */
    fun positionAt(groove: Groove, bpm: Int, sessionNanos: Long): Double {
        if (sessionNanos < 0) return -1.0
        val step = stepNanos(bpm)
        return stepAt(groove, bpm, sessionNanos) + (sessionNanos % step).toDouble() / step
    }
}
