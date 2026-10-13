// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

enum class Judgement { PERFECT, GOOD, OK, WRONG_HAND, MISSED, EXTRA, GRACE_HIT, GRACE_MISSED }

/**
 * Result of one tap or one closed window.
 * [offsetNanos] = tap time − expected time (negative = early), for timed hits only.
 */
data class Feedback(
    val judgement: Judgement,
    val hand: Hand,
    val timeNanos: Long,
    val noteIndex: Int? = null,
    val offsetNanos: Long? = null,
)

data class SessionResult(
    val accuracy: Double,
    val perfect: Int,
    val good: Int,
    val ok: Int,
    val wrongHand: Int,
    val missed: Int,
    val extra: Int,
    val graceHit: Int,
    val graceMissed: Int,
    /** Mean of tap − expected, in ms. Negative = rushing, positive = dragging. */
    val meanOffsetMs: Double,
    /** Standard deviation of the offsets, in ms. */
    val spreadMs: Double,
) {
    val isClean: Boolean get() = accuracy >= Scoring.CLEAN_ACCURACY
}

object Scoring {
    const val CLEAN_ACCURACY = 90.0
    const val MAX_WINDOW_NANOS = 100 * NANOS_PER_MS
    const val GRACE_BEFORE_NANOS = 180 * NANOS_PER_MS
    const val GRACE_AFTER_NANOS = 10 * NANOS_PER_MS
    const val STEADY_MS = 10.0

    const val POINTS_PERFECT = 1.0
    const val POINTS_GOOD = 0.75
    const val POINTS_OK = 0.5
    const val POINTS_WRONG_HAND = 0.25
    const val POINTS_GRACE = 0.5
    const val PENALTY_EXTRA = 0.25

    fun windowNanos(slotNanos: Long): Long = min(MAX_WINDOW_NANOS, slotNanos * 45 / 100)

    /** Next tempo to try after a session. */
    fun suggestedBpm(bpm: Int, accuracy: Double): Int = when {
        accuracy >= CLEAN_ACCURACY -> bpm + 4
        accuracy < 70.0 -> bpm - 8
        else -> bpm
    }.coerceIn(MIN_BPM, MAX_BPM)

    const val MIN_BPM = 40
    const val MAX_BPM = 240
}

/**
 * Matches taps to the expected notes of a [Timeline] (tolerant mode: grace notes only need
 * the right hand shortly before their main stroke). Feed taps in time order with [tap] and
 * call [advanceTo] regularly so missed notes are reported; [finish] returns the result.
 */
class Scorer(private val timeline: Timeline) {
    private val window = Scoring.windowNanos(timeline.slotNanos)
    private val perfectLimit = min(25 * NANOS_PER_MS, window / 2)
    private val goodLimit = min(50 * NANOS_PER_MS, window)

    private class Grace(val hand: Hand, val nominal: Long, val mainTime: Long) {
        var done = false
    }

    private val notes = timeline.notes
    private val noteDone = BooleanArray(notes.size)
    private val graces = notes.flatMap { n -> n.graces.map { Grace(it.hand, it.timeNanos, n.timeNanos) } }

    private var nextOpenNote = 0
    private var nextOpenGrace = 0

    private var points = 0.0
    private var judgedMax = 0.0
    private val counts = HashMap<Judgement, Int>()
    private val offsets = ArrayList<Long>()

    /** Accuracy (0–100) over what has been judged so far; 100 before anything is judged. */
    val liveAccuracy: Double
        get() = if (judgedMax == 0.0) 100.0 else (points.coerceAtLeast(0.0) / judgedMax * 100).coerceIn(0.0, 100.0)

    /** Reports notes and graces whose windows closed before [nowNanos]. */
    fun advanceTo(nowNanos: Long): List<Feedback> {
        val out = ArrayList<Feedback>()
        while (nextOpenNote < notes.size && notes[nextOpenNote].timeNanos + window < nowNanos) {
            val n = notes[nextOpenNote]
            if (!noteDone[n.index]) {
                noteDone[n.index] = true
                record(Judgement.MISSED, 0.0, Scoring.POINTS_PERFECT)
                out += Feedback(Judgement.MISSED, n.hand, n.timeNanos, n.index)
            }
            nextOpenNote++
        }
        while (nextOpenGrace < graces.size && graces[nextOpenGrace].mainTime + Scoring.GRACE_AFTER_NANOS < nowNanos) {
            val g = graces[nextOpenGrace]
            if (!g.done) {
                g.done = true
                record(Judgement.GRACE_MISSED, 0.0, Scoring.POINTS_GRACE)
                out += Feedback(Judgement.GRACE_MISSED, g.hand, g.nominal)
            }
            nextOpenGrace++
        }
        return out
    }

    /** Judges a tap. Taps must arrive in non-decreasing time order. */
    fun tap(hand: Hand, timeNanos: Long): Feedback {
        // Same-hand candidates: earliest expected time wins.
        var bestNote: NoteEvent? = null
        for (i in nextOpenNote until notes.size) {
            val n = notes[i]
            if (n.timeNanos - window > timeNanos) break
            if (!noteDone[i] && n.hand == hand && abs(timeNanos - n.timeNanos) <= window) {
                bestNote = n
                break
            }
        }
        var bestGrace: Grace? = null
        for (i in nextOpenGrace until graces.size) {
            val g = graces[i]
            if (g.mainTime - Scoring.GRACE_BEFORE_NANOS > timeNanos) break
            if (!g.done && g.hand == hand && timeNanos >= g.mainTime - Scoring.GRACE_BEFORE_NANOS &&
                timeNanos <= g.mainTime + Scoring.GRACE_AFTER_NANOS
            ) {
                bestGrace = g
                break
            }
        }
        if (bestGrace != null && (bestNote == null || bestGrace.nominal < bestNote.timeNanos)) {
            bestGrace.done = true
            record(Judgement.GRACE_HIT, Scoring.POINTS_GRACE, Scoring.POINTS_GRACE)
            return Feedback(Judgement.GRACE_HIT, hand, timeNanos)
        }
        if (bestNote != null) {
            noteDone[bestNote.index] = true
            val offset = timeNanos - bestNote.timeNanos
            val j = when {
                abs(offset) <= perfectLimit -> Judgement.PERFECT
                abs(offset) <= goodLimit -> Judgement.GOOD
                else -> Judgement.OK
            }
            val p = when (j) {
                Judgement.PERFECT -> Scoring.POINTS_PERFECT
                Judgement.GOOD -> Scoring.POINTS_GOOD
                else -> Scoring.POINTS_OK
            }
            record(j, p, Scoring.POINTS_PERFECT)
            offsets += offset
            return Feedback(j, hand, timeNanos, bestNote.index, offset)
        }
        // Other hand: nearest open note → wrong hand.
        var wrong: NoteEvent? = null
        for (i in nextOpenNote until notes.size) {
            val n = notes[i]
            if (n.timeNanos - window > timeNanos) break
            if (!noteDone[i] && abs(timeNanos - n.timeNanos) <= window &&
                (wrong == null || abs(timeNanos - n.timeNanos) < abs(timeNanos - wrong.timeNanos))
            ) wrong = n
        }
        if (wrong != null) {
            noteDone[wrong.index] = true
            record(Judgement.WRONG_HAND, Scoring.POINTS_WRONG_HAND, Scoring.POINTS_PERFECT)
            return Feedback(Judgement.WRONG_HAND, hand, timeNanos, wrong.index, timeNanos - wrong.timeNanos)
        }
        record(Judgement.EXTRA, -Scoring.PENALTY_EXTRA, 0.0)
        return Feedback(Judgement.EXTRA, hand, timeNanos)
    }

    private fun record(j: Judgement, p: Double, max: Double) {
        points += p
        judgedMax += max
        counts[j] = (counts[j] ?: 0) + 1
    }

    fun finish(): SessionResult {
        advanceTo(Long.MAX_VALUE)
        val totalMax = notes.size * Scoring.POINTS_PERFECT + graces.size * Scoring.POINTS_GRACE
        val accuracy = if (totalMax == 0.0) 0.0 else (points.coerceAtLeast(0.0) / totalMax * 100).coerceIn(0.0, 100.0)
        val ms = offsets.map { it.toDouble() / NANOS_PER_MS }
        val mean = if (ms.isEmpty()) 0.0 else ms.average()
        val spread = if (ms.size < 2) 0.0 else sqrt(ms.sumOf { (it - mean) * (it - mean) } / ms.size)
        fun c(j: Judgement) = counts[j] ?: 0
        return SessionResult(
            accuracy = accuracy,
            perfect = c(Judgement.PERFECT),
            good = c(Judgement.GOOD),
            ok = c(Judgement.OK),
            wrongHand = c(Judgement.WRONG_HAND),
            missed = c(Judgement.MISSED),
            extra = c(Judgement.EXTRA),
            graceHit = c(Judgement.GRACE_HIT),
            graceMissed = c(Judgement.GRACE_MISSED),
            meanOffsetMs = mean,
            spreadMs = spread,
        )
    }
}
