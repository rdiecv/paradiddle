// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.input

import android.os.Handler
import android.os.Looper
import io.github.rdiecv.paradiddle.audio.Playback
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.Timeline
import io.github.rdiecv.paradiddle.core.input.ClickGate

/**
 * Feeds the hits of a [DrumInput] into a playing session, on the main thread: in session time,
 * corrected by the input's calibration [offsetNanos], with the metronome heard by a
 * microphone ([mic]) filtered out.
 */
class LiveDrumInput(
    private val input: DrumInput,
    private val playback: Playback,
    timeline: Timeline,
    mic: Boolean,
    private val offsetNanos: Long,
    private val onHit: (timeNanos: Long, level: Float, pad: Instrument?) -> Unit,
) {
    private val gate = if (mic) ClickGate(timeline.clicks.map { it.timeNanos }, timeline.countInNanos) else null
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var active = false

    /** Throws [IllegalStateException] if the input can't be opened. */
    fun start() {
        active = true
        input.start { nanoTime, level, pad -> main.post { deliver(nanoTime, level, pad) } }
    }

    private fun deliver(nanoTime: Long, level: Float, pad: Instrument?) {
        if (!active) return
        val heard = playback.sessionTimeAt(nanoTime)
        if (gate?.accept(heard, level) == false) return
        onHit(heard - offsetNanos, level, pad)
    }

    fun stop() {
        active = false
        input.stop()
        main.removeCallbacksAndMessages(null)
    }
}
