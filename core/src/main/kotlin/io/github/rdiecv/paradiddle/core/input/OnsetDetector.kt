// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.input

import kotlin.math.PI
import kotlin.math.abs

/**
 * Finds drum hits in a microphone signal. Feed consecutive mono chunks with [feed]; each onset
 * gives the frame (counted from the first sample fed) where the hit starts and its level (peak
 * amplitude in its first 10 ms, 0–1). Hits closer than [REFRACTORY_MS] merge, so a flam is one
 * hit at its grace note.
 */
class OnsetDetector(sampleRate: Int, private val minLevel: Float = 0.02f) {
    data class Onset(val frame: Long, val level: Float)

    private val hop = maxOf(1, sampleRate / 1000) // 1 ms
    private val hp: Float // one-pole high-pass at ~60 Hz removes rumble and handling noise
    private var hpIn = 0f
    private var hpOut = 0f

    // Current hop.
    private var frame = 0L
    private var hopStart = 0L
    private var hopFill = 0
    private var hopPeak = 0f
    private val hopSamples = FloatArray(hop)

    // Recent hop peaks, for the "has it risen?" test.
    private val recent = FloatArray(RISE_HOPS)
    private var recentCount = 0
    private var recentPos = 0

    private var floor = minLevel / 8
    private var refractory = 0
    private var pendingFrame = -1L
    private var pendingLevel = 0f
    private var pendingHops = 0

    init {
        val rc = 1.0 / (2 * PI * 60.0)
        val dt = 1.0 / sampleRate
        hp = (rc / (rc + dt)).toFloat()
    }

    fun feed(samples: FloatArray, count: Int = samples.size): List<Onset> {
        var out: ArrayList<Onset>? = null
        for (i in 0 until count) {
            val x = samples[i]
            hpOut = hp * (hpOut + x - hpIn)
            hpIn = x
            val a = abs(hpOut)
            hopSamples[hopFill++] = a
            if (a > hopPeak) hopPeak = a
            frame++
            if (hopFill == hop) {
                endOfHop()?.let { (out ?: ArrayList<Onset>().also { l -> out = l }).add(it) }
                hopStart = frame
                hopFill = 0
                hopPeak = 0f
            }
        }
        return out ?: emptyList()
    }

    private fun endOfHop(): Onset? {
        var emitted: Onset? = null
        if (pendingFrame >= 0) {
            if (hopPeak > pendingLevel) pendingLevel = hopPeak
            if (--pendingHops == 0) {
                emitted = Onset(pendingFrame, pendingLevel.coerceAtMost(1f))
                pendingFrame = -1
            }
        }
        val threshold = maxOf(minLevel, floor * FLOOR_RATIO)
        var before = if (recentCount == 0) 0f else Float.MAX_VALUE
        for (k in 0 until recentCount) if (recent[k] < before) before = recent[k]
        if (refractory > 0) {
            refractory--
        } else if (hopPeak > threshold && hopPeak >= before * RISE_RATIO) {
            var k = 0
            while (k < hop - 1 && hopSamples[k] <= threshold) k++
            pendingFrame = hopStart + k
            pendingLevel = hopPeak
            pendingHops = LEVEL_HOPS
            refractory = REFRACTORY_MS - 1
        }
        if (refractory == 0 && hopPeak <= threshold) floor += (hopPeak - floor) * FLOOR_SPEED
        recent[recentPos] = hopPeak
        recentPos = (recentPos + 1) % RISE_HOPS
        if (recentCount < RISE_HOPS) recentCount++
        return emitted
    }

    private companion object {
        const val RISE_HOPS = 10
        const val RISE_RATIO = 2f
        const val FLOOR_RATIO = 4f
        const val FLOOR_SPEED = 0.005f
        const val LEVEL_HOPS = 10
        const val REFRACTORY_MS = 35
    }
}
