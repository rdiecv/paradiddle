// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

/** A sound starting at [startFrame], placed at [pan]: -1 = left, 0 = centre, 1 = right. */
class Voice(val startFrame: Long, val samples: FloatArray, val gain: Float = 1f, val pan: Float = 0f) {
    val endFrame: Long get() = startFrame + samples.size
}

/**
 * Mixes a fixed list of voices into consecutive stereo output buffers. Each voice starts on its
 * exact frame regardless of buffer boundaries. Output is clipped to [-1, 1].
 *
 * Panning only turns the far side down, so a centred voice plays at full gain on both sides.
 */
class Mixer(voices: List<Voice>) {
    private val voices = voices.sortedBy { it.startFrame }
    private val maxLength = this.voices.maxOfOrNull { it.samples.size } ?: 0
    private var firstCandidate = 0

    val endFrame: Long = this.voices.maxOfOrNull { it.endFrame } ?: 0L

    /**
     * Fills [out] with frames [startFrame, startFrame + out.size / 2), interleaved left, right.
     * Calls must move forward in time.
     */
    fun render(startFrame: Long, out: FloatArray) {
        out.fill(0f)
        val endFrame = startFrame + out.size / 2
        // Voices that started more than maxLength frames ago are finished for good.
        while (firstCandidate < voices.size && voices[firstCandidate].startFrame + maxLength <= startFrame) {
            firstCandidate++
        }
        var i = firstCandidate
        while (i < voices.size) {
            val v = voices[i++]
            if (v.startFrame >= endFrame) break
            if (v.endFrame <= startFrame) continue
            val from = maxOf(startFrame, v.startFrame)
            val to = minOf(endFrame, v.endFrame)
            val left = v.gain * (1f - v.pan).coerceIn(0f, 1f)
            val right = v.gain * (1f + v.pan).coerceIn(0f, 1f)
            var f = from
            while (f < to) {
                val sample = v.samples[(f - v.startFrame).toInt()]
                val k = (f - startFrame).toInt() * 2
                out[k] += sample * left
                out[k + 1] += sample * right
                f++
            }
        }
        for (k in out.indices) out[k] = out[k].coerceIn(-1f, 1f)
    }
}
