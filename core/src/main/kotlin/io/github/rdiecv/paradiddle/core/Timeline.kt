// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

const val NANOS_PER_MS = 1_000_000L
const val NANOS_PER_SECOND = 1_000_000_000L

/** A grace note's nominal (drawing/sound) time. Its timing is not scored. */
data class GraceEvent(val hand: Hand, val timeNanos: Long)

data class NoteEvent(
    /** Index in [Timeline.notes]. */
    val index: Int,
    val timeNanos: Long,
    val durationNanos: Long,
    val hand: Hand,
    val accent: Boolean,
    val buzz: Boolean,
    val graces: List<GraceEvent>,
    /** 0-based repetition of the pattern. */
    val repetition: Int,
    /** Index of the source [Stroke] in the rudiment's pattern. */
    val strokeIndex: Int,
    /** The drum to hit, when it matters (style beats played on an electronic kit). */
    val pad: Instrument? = null,
)

data class Click(val timeNanos: Long, val strong: Boolean, val countIn: Boolean)

/** Everything that happens in one session, in session time (0 = first audio frame). */
data class Timeline(
    val bpm: Int,
    val repetitions: Int,
    val beatNanos: Long,
    val slotNanos: Long,
    val countInNanos: Long,
    val patternNanos: Long,
    val endNanos: Long,
    val notes: List<NoteEvent>,
    val clicks: List<Click>,
) {
    /** Repetition being played at [timeNanos], or -1 during the count-in. */
    fun repetitionAt(timeNanos: Long): Int =
        if (timeNanos < countInNanos) -1
        else ((timeNanos - countInNanos) / patternNanos).toInt().coerceAtMost(repetitions - 1)
}

object TimelineBuilder {
    const val COUNT_IN_BEATS = 4
    /** Spacing between grace notes and their main stroke, for drawing and sound. */
    const val GRACE_SPACING_NANOS = 25 * NANOS_PER_MS
    /** Silence after the last note before the session ends. */
    private const val TAIL_BEATS = 1

    fun build(rudiment: Rudiment, bpm: Int, repetitions: Int, countInBeats: Int = COUNT_IN_BEATS): Timeline {
        require(bpm in 20..400) { "bpm out of range: $bpm" }
        require(repetitions >= 1) { "repetitions must be >= 1" }
        val beatNanos = 60 * NANOS_PER_SECOND / bpm
        // Slot times are computed from beats to avoid accumulating rounding errors.
        fun slotTime(slot: Long): Long = slot * beatNanos / rudiment.grid
        val countInNanos = countInBeats * beatNanos
        val patternNanos = rudiment.beats * beatNanos

        val notes = ArrayList<NoteEvent>()
        for (rep in 0 until repetitions) {
            val repStart = countInNanos + rep * patternNanos
            var slot = 0L
            rudiment.strokes.forEachIndexed { strokeIndex, stroke ->
                val t = repStart + slotTime(slot)
                val hand = stroke.hand
                if (hand != null) {
                    val k = stroke.graces.size
                    val graces = stroke.graces.mapIndexed { i, g -> GraceEvent(g, t - (k - i) * GRACE_SPACING_NANOS) }
                    notes += NoteEvent(
                        index = notes.size,
                        timeNanos = t,
                        durationNanos = slotTime(slot + stroke.slots) - slotTime(slot),
                        hand = hand,
                        accent = stroke.accent,
                        buzz = stroke.buzz,
                        graces = graces,
                        repetition = rep,
                        strokeIndex = strokeIndex,
                    )
                }
                slot += stroke.slots
            }
        }

        val totalBeats = countInBeats + repetitions * rudiment.beats
        val clicks = (0 until totalBeats).map { beat ->
            val countIn = beat < countInBeats
            val strong = if (countIn) beat == 0 else (beat - countInBeats) % rudiment.beats == 0
            Click(beat * beatNanos, strong, countIn)
        }
        return Timeline(
            bpm = bpm,
            repetitions = repetitions,
            beatNanos = beatNanos,
            slotNanos = beatNanos / rudiment.grid,
            countInNanos = countInNanos,
            patternNanos = patternNanos,
            endNanos = (totalBeats + TAIL_BEATS) * beatNanos,
            notes = notes,
            clicks = clicks,
        )
    }
}
