// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

/** Turns a style beat into a [Timeline] to practise and score it on real drums. */
object GrooveTimeline {
    const val COUNT_IN_BEATS = 4
    private const val STEPS_PER_BEAT = Groove.STEPS_PER_BAR / 4

    /**
     * [loops] times through [groove] after a one-bar count-in. With [perPad] (an electronic kit
     * that says which pad was hit) every hit is a note on its pad; otherwise (a microphone) the
     * hits of one step are a single note, accented if any of them is.
     */
    fun build(groove: Groove, bpm: Int, loops: Int, perPad: Boolean): Timeline {
        require(bpm in Groove.MIN_BPM..Groove.MAX_BPM) { "bpm out of range: $bpm" }
        require(loops >= 1) { "loops must be >= 1" }
        val beatNanos = 60 * NANOS_PER_SECOND / bpm
        fun stepTime(step: Long): Long = step * beatNanos / STEPS_PER_BEAT
        val countInNanos = COUNT_IN_BEATS * beatNanos
        val beatsPerLoop = groove.bars * 4

        val notes = ArrayList<NoteEvent>()
        for (loop in 0 until loops) {
            for (step in 0 until groove.steps) {
                val absolute = loop.toLong() * groove.steps + step
                val time = countInNanos + stepTime(absolute)
                val duration = stepTime(absolute + 1) - stepTime(absolute)
                val here = groove.tracks.mapNotNull { (instrument, hits) -> hits[step]?.let { instrument to it } }
                if (here.isEmpty()) continue
                fun note(hand: Hand, accent: Boolean, pad: Instrument?) = NoteEvent(
                    index = notes.size, timeNanos = time, durationNanos = duration, hand = hand, accent = accent,
                    buzz = false, graces = emptyList(), repetition = loop, strokeIndex = step, pad = pad,
                )
                if (perPad) {
                    here.forEach { (instrument, hit) -> notes += note(instrument.hand(), hit == Hit.ACCENT, instrument) }
                } else {
                    notes += note(Hand.RIGHT, here.any { it.second == Hit.ACCENT }, null)
                }
            }
        }

        val totalBeats = COUNT_IN_BEATS + loops * beatsPerLoop
        val clicks = (0 until totalBeats).map { beat ->
            val countIn = beat < COUNT_IN_BEATS
            Click(beat * beatNanos, strong = beat % 4 == 0, countIn = countIn)
        }
        return Timeline(
            bpm = bpm,
            repetitions = loops,
            beatNanos = beatNanos,
            slotNanos = beatNanos / STEPS_PER_BEAT,
            countInNanos = countInNanos,
            patternNanos = beatsPerLoop * beatNanos,
            endNanos = (totalBeats + 1) * beatNanos,
            notes = notes,
            clicks = clicks,
        )
    }

    /** Right-handed kit: right hand on the cymbals and kick foot, left hand on snare and toms. */
    private fun Instrument.hand(): Hand = when (this) {
        Instrument.SNARE, Instrument.CROSS_STICK, Instrument.TOM -> Hand.LEFT
        else -> Hand.RIGHT
    }
}
