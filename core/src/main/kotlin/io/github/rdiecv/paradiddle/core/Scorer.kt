// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

enum class Judgement { PERFECT, GOOD, OK, WRONG_HAND, WRONG_PAD, MISSED, EXTRA, GRACE_HIT, GRACE_MISSED }

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
    /** How well accents were played (real drums only), null when not checked. */
    val accents: AccentResult? = null,
    /** Hits on time but on another drum (electronic kit only). */
    val wrongPad: Int = 0,
) {
    val isClean: Boolean get() = accuracy >= Scoring.CLEAN_ACCURACY
}

/** Notes played at the right loudness: accents loud, the others soft. */
data class AccentResult(val right: Int, val total: Int)

/**
 * How much timing slack a hit gets: it counts within [windowMs] of the note, is "perfect"
 * within [perfectMs] and "good" within [goodMs]. At fast tempos the window also shrinks so
 * neighbouring notes never overlap.
 */
enum class Difficulty(val windowMs: Int, val perfectMs: Int, val goodMs: Int) {
    EASY(150, 50, 90),
    NORMAL(100, 25, 50),
    HARD(70, 15, 35),
}

object Scoring {
    const val CLEAN_ACCURACY = 90.0
    const val GRACE_BEFORE_NANOS = 180 * NANOS_PER_MS
    const val GRACE_AFTER_NANOS = 10 * NANOS_PER_MS
    const val STEADY_MS = 10.0

    const val POINTS_PERFECT = 1.0
    const val POINTS_GOOD = 0.75
    const val POINTS_OK = 0.5
    const val POINTS_WRONG_HAND = 0.25
    const val POINTS_GRACE = 0.5
    const val PENALTY_EXTRA = 0.25
    /** An accent must be this much louder than the typical unaccented note. */
    const val ACCENT_RATIO = 1.25f

    fun windowNanos(slotNanos: Long, difficulty: Difficulty = Difficulty.NORMAL): Long =
        min(difficulty.windowMs * NANOS_PER_MS, slotNanos * 45 / 100)

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
 *
 * With [realDrums] (microphone or electronic kit) hits come through [hit]: hands can't be told
 * apart so they aren't checked, grace notes aren't scored (their strokes are absorbed by the
 * main note, as are a buzz stroke's bounces), the pad is checked when known, and the loudness
 * of each note is kept to judge the accents.
 */
class Scorer(
    private val timeline: Timeline,
    private val realDrums: Boolean = false,
    difficulty: Difficulty = Difficulty.NORMAL,
) {
    /** A hit counts within this distance of its note. */
    val windowNanos = Scoring.windowNanos(timeline.slotNanos, difficulty)
    /** A hit within this distance is perfect. */
    val perfectNanos = min(difficulty.perfectMs * NANOS_PER_MS, windowNanos / 2)
    /** A hit within this distance is good. */
    val goodNanos = min(difficulty.goodMs * NANOS_PER_MS, windowNanos)

    private class Grace(val hand: Hand, val nominal: Long, val mainTime: Long) {
        var done = false
    }

    private val notes = timeline.notes
    private val noteDone = BooleanArray(notes.size)
    private val graces =
        if (realDrums) emptyList() else notes.flatMap { n -> n.graces.map { Grace(it.hand, it.timeNanos, n.timeNanos) } }
    private val levels = FloatArray(notes.size) { Float.NaN }
    private var absorbUntil = Long.MIN_VALUE

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
        while (nextOpenNote < notes.size && notes[nextOpenNote].timeNanos + windowNanos < nowNanos) {
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
            if (n.timeNanos - windowNanos > timeNanos) break
            if (!noteDone[i] && n.hand == hand && abs(timeNanos - n.timeNanos) <= windowNanos) {
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
                abs(offset) <= perfectNanos -> Judgement.PERFECT
                abs(offset) <= goodNanos -> Judgement.GOOD
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
            if (n.timeNanos - windowNanos > timeNanos) break
            if (!noteDone[i] && abs(timeNanos - n.timeNanos) <= windowNanos &&
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

    /**
     * Judges a hit on a real drum at [timeNanos] with loudness [level] (0–1) on [pad] (null when
     * unknown). Hits must arrive in non-decreasing time order. Returns null for a hit that belongs
     * to the note just played (its main stroke after a grace, or a buzz bounce).
     */
    fun hit(timeNanos: Long, level: Float, pad: Instrument? = null): Feedback? {
        check(realDrums) { "hit() is for real drums; use tap()" }
        var best: NoteEvent? = null
        var bestDistance = Long.MAX_VALUE
        var other: NoteEvent? = null
        var otherDistance = Long.MAX_VALUE
        for (i in nextOpenNote until notes.size) {
            val n = notes[i]
            if (n.timeNanos - windowNanos > timeNanos + spanBefore(n)) break
            if (noteDone[i]) continue
            val d = distance(n, timeNanos)
            if (d > windowNanos) continue
            if (pad == null || n.pad == null || sameGroup(pad, n.pad)) {
                // Earliest matching note wins, like taps.
                if (best == null) { best = n; bestDistance = d }
            } else if (d < otherDistance) {
                other = n; otherDistance = d
            }
        }
        if (best == null && timeNanos <= absorbUntil) {
            return null
        }
        if (best != null) {
            noteDone[best.index] = true
            levels[best.index] = level
            absorbUntil = absorbEnd(best)
            val signed = if (timeNanos < best.timeNanos) -bestDistance else bestDistance
            val j = when {
                bestDistance <= perfectNanos -> Judgement.PERFECT
                bestDistance <= goodNanos -> Judgement.GOOD
                else -> Judgement.OK
            }
            record(j, pointsFor(j), Scoring.POINTS_PERFECT)
            offsets += signed
            return Feedback(j, best.hand, timeNanos, best.index, signed)
        }
        if (other != null) {
            noteDone[other.index] = true
            record(Judgement.WRONG_PAD, Scoring.POINTS_WRONG_HAND, Scoring.POINTS_PERFECT)
            return Feedback(Judgement.WRONG_PAD, other.hand, timeNanos, other.index, timeNanos - other.timeNanos)
        }
        record(Judgement.EXTRA, -Scoring.PENALTY_EXTRA, 0.0)
        return Feedback(Judgement.EXTRA, nearestHand(timeNanos), timeNanos)
    }

    /** How early a note's grace strokes start. */
    private fun spanBefore(n: NoteEvent): Long = n.graces.firstOrNull()?.let { n.timeNanos - it.timeNanos } ?: 0L

    /** Distance from a hit to the note, or to its nearest grace stroke (a flam heard as one hit). */
    private fun distance(n: NoteEvent, t: Long): Long {
        var d = abs(t - n.timeNanos)
        n.graces.forEach { g -> d = min(d, abs(t - g.timeNanos)) }
        return d
    }

    /** Until when further hits belong to [n]: its main stroke after a grace, or a buzz's bounces. */
    private fun absorbEnd(n: NoteEvent): Long {
        val own = when {
            n.buzz -> n.timeNanos + n.durationNanos
            n.graces.isNotEmpty() -> n.timeNanos + ABSORB_AFTER_MAIN_NANOS
            else -> return Long.MIN_VALUE
        }
        val next = notes.getOrNull(n.index + 1)?.let { it.timeNanos - spanBefore(it) - windowNanos } ?: Long.MAX_VALUE
        return min(own, next)
    }

    /** Pads that count as the same drum: the cowbell sits on the ride pad, cross-stick is the snare rim. */
    private fun sameGroup(a: Instrument, b: Instrument): Boolean = group(a) == group(b)

    private fun group(i: Instrument): Instrument = when (i) {
        Instrument.COWBELL -> Instrument.RIDE
        Instrument.CROSS_STICK -> Instrument.SNARE
        else -> i
    }

    private fun nearestHand(t: Long): Hand =
        notes.minByOrNull { abs(it.timeNanos - t) }?.hand ?: Hand.RIGHT

    private fun pointsFor(j: Judgement): Double = when (j) {
        Judgement.PERFECT -> Scoring.POINTS_PERFECT
        Judgement.GOOD -> Scoring.POINTS_GOOD
        else -> Scoring.POINTS_OK
    }

    /** Accents loud, other notes soft, measured against the typical unaccented note. */
    private fun accentResult(): AccentResult? {
        if (!realDrums) return null
        val played = notes.filter { !levels[it.index].isNaN() }
        val soft = played.filter { !it.accent }.map { levels[it.index] }.sorted()
        if (soft.isEmpty() || played.none { it.accent }) return null
        val median = if (soft.size % 2 == 1) soft[soft.size / 2] else (soft[soft.size / 2 - 1] + soft[soft.size / 2]) / 2
        val threshold = median * Scoring.ACCENT_RATIO
        val right = played.count { (levels[it.index] >= threshold) == it.accent }
        return AccentResult(right, played.size)
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
            accents = accentResult(),
            wrongPad = c(Judgement.WRONG_PAD),
        )
    }

    private companion object {
        const val ABSORB_AFTER_MAIN_NANOS = 40 * NANOS_PER_MS
    }
}
