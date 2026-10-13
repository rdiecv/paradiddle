// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.Hit
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND
import io.github.rdiecv.paradiddle.core.SongTimeline

/** Turns a [Groove] into mixer voices, looped a number of times. */
class GrooveSound(private val synth: Synth) {
    private val closedHiHat = synth.hihat(open = false)
    private val openHiHat = synth.hihat(open = true)
    private val clickStrong = synth.click(strong = true)
    private val clickWeak = synth.click(strong = false)
    private val samples = mapOf(
        Instrument.CRASH to synth.crash(),
        Instrument.RIDE to synth.ride(),
        Instrument.COWBELL to synth.cowbell(),
        Instrument.SNARE to synth.snare(1f),
        Instrument.CROSS_STICK to synth.crossStick(),
        Instrument.TOM to synth.tom(),
        Instrument.KICK to synth.kick(),
    )

    /** Balances the kit: cymbals sit under the drums. */
    private fun level(instrument: Instrument): Float = when (instrument) {
        Instrument.HIHAT, Instrument.RIDE, Instrument.CRASH -> 0.5f
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

    private fun sample(instrument: Instrument, hit: Hit): FloatArray = when {
        instrument != Instrument.HIHAT -> samples.getValue(instrument)
        hit == Hit.OPEN -> openHiHat
        else -> closedHiHat
    }

    /** Adds the hits of [groove]'s step [grooveStep] at absolute 16th [at]. */
    private fun addStep(out: MutableList<Voice>, groove: Groove, grooveStep: Int, at: Long, bpm: Int) {
        groove.tracks.forEach { (instrument, hits) ->
            val hit = hits[grooveStep] ?: return@forEach
            out += Voice(frameOfStep(at, bpm), sample(instrument, hit), gain(hit) * level(instrument))
        }
    }

    fun voices(groove: Groove, bpm: Int, loops: Int): List<Voice> {
        val out = ArrayList<Voice>()
        repeat(loops) { loop ->
            for (step in 0 until groove.steps) addStep(out, groove, step, loop.toLong() * groove.steps + step, bpm)
        }
        return out
    }

    /**
     * Voices for [timeline] after a one-bar count-in ([COUNT_IN_STEPS]): each section plays its
     * groove, sections without one click every beat so you can count, and a crash marks the
     * start of every section after the first one of the song.
     */
    fun songVoices(timeline: SongTimeline, bpm: Int): List<Voice> {
        val out = ArrayList<Voice>()
        for (beat in 0 until 4) {
            out += Voice(frameOfStep(beat * 4L, bpm), if (beat == 0) clickStrong else clickWeak, 0.8f)
        }
        for (step in 0 until timeline.totalSteps) {
            val place = timeline.placeAt(step) ?: continue
            val at = COUNT_IN_STEPS + step.toLong()
            val groove = timeline.sections[place.section].groove
            if (groove == null) {
                if (step % 4 == 0) out += Voice(frameOfStep(at, bpm), if (step % 16 == 0) clickStrong else clickWeak, 0.6f)
                continue
            }
            addStep(out, groove, place.grooveStep, at, bpm)
            if (place.sectionStart && place.section > 0 && Instrument.CRASH !in groove.tracks) {
                out += Voice(frameOfStep(at, bpm), samples.getValue(Instrument.CRASH), level(Instrument.CRASH))
            }
        }
        return out
    }

    /** Frames from the count-in to the end of the song. */
    fun songLengthFrames(timeline: SongTimeline, bpm: Int): Long = frameOfStep(COUNT_IN_STEPS + timeline.totalSteps.toLong(), bpm)

    /** Song 16th heard at [sessionNanos] since playback start: negative during the count-in. */
    fun songStepAt(bpm: Int, sessionNanos: Long): Double =
        sessionNanos.toDouble() / stepNanos(bpm) - COUNT_IN_STEPS

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

    companion object {
        const val COUNT_IN_STEPS = 16
    }
}
