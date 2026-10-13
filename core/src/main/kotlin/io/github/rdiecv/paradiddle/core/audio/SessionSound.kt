// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core.audio

import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND
import io.github.rdiecv.paradiddle.core.Timeline

/** Turns a [Timeline] into mixer voices: metronome clicks and, optionally, the pattern itself. */
class SessionSound(private val synth: Synth) {
    private val clickStrong = synth.click(strong = true)
    private val clickWeak = synth.click(strong = false)
    private val accent = synth.snare(1.0f)
    private val normal = synth.snare(0.55f, seed = 2)
    private val grace = synth.snare(0.3f, seed = 3)

    fun frameOf(timeNanos: Long): Long = timeNanos * synth.sampleRate / NANOS_PER_SECOND

    /**
     * @param clickGain metronome volume, 0 = no clicks
     * @param patternGain volume of the played pattern, 0 = don't play it
     */
    fun voices(timeline: Timeline, clickGain: Float, patternGain: Float): List<Voice> {
        val out = ArrayList<Voice>()
        if (clickGain > 0f) {
            timeline.clicks.forEach { c ->
                out += Voice(frameOf(c.timeNanos), if (c.strong) clickStrong else clickWeak, clickGain)
            }
        }
        if (patternGain > 0f) {
            timeline.notes.forEach { n ->
                n.graces.forEach { g -> out += Voice(frameOf(g.timeNanos), grace, patternGain) }
                val samples = when {
                    n.buzz -> synth.buzz(if (n.accent) 1f else 0.55f, n.durationNanos.toDouble() / NANOS_PER_SECOND)
                    n.accent -> accent
                    else -> normal
                }
                out += Voice(frameOf(n.timeNanos), samples, patternGain)
            }
        }
        return out
    }
}
