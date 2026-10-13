// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.rdiecv.paradiddle.audio.AudioEngine
import io.github.rdiecv.paradiddle.audio.Playback
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.Style
import io.github.rdiecv.paradiddle.core.audio.GrooveSound
import io.github.rdiecv.paradiddle.core.audio.Synth

/** The chosen beat of a style, looped for up to [MAX_SECONDS] at the chosen tempo. */
class StyleViewModel(val style: Style) : ViewModel() {
    private val sound by lazy { GrooveSound(Synth(AudioEngine.sampleRate, style.sound)) }
    private var playback: Playback? = null

    var selected by mutableIntStateOf(0)
        private set

    val groove: Groove get() = style.grooves[selected]

    private var playingBpm = groove.bpm

    var bpm by mutableIntStateOf(groove.bpm)
        private set

    /** Switches to another beat of the style: stops playback and starts from that beat's tempo. */
    fun select(index: Int) {
        if (index == selected) return
        stop()
        selected = index
        bpm = groove.bpm
    }

    /** Written from the audio thread when the loop ends, so it is plain Compose state. */
    var playing by mutableStateOf(false)
        private set

    var audioError by mutableStateOf(false)
        private set

    /** Changes the shown tempo; call [applyTempo] when the change is final. */
    fun changeTempo(value: Int) {
        bpm = value.coerceIn(Groove.MIN_BPM, Groove.MAX_BPM)
    }

    /** Restarts the loop at the new tempo if it is playing. */
    fun applyTempo() {
        if (playing && bpm != playingBpm) start()
    }

    fun toggle() = if (playing) stop() else start()

    private fun start() {
        playback?.stop()
        val loops = sound.loopsFor(groove, bpm, MAX_SECONDS)
        playingBpm = bpm
        playback = try {
            AudioEngine.play(sound.voices(groove, bpm, loops), sound.lengthFrames(groove, bpm, loops)) { playing = false }
        } catch (e: IllegalStateException) {
            audioError = true
            null
        }
        playing = playback != null
    }

    fun stop() {
        playback?.stop()
        playback = null
        playing = false
    }

    /** Step being heard now plus how far into it (e.g. 3.25), or -1.0 when not playing. */
    fun currentPosition(): Double {
        val p = playback ?: return -1.0
        if (!playing) return -1.0
        return sound.positionAt(groove, playingBpm, p.sessionTimeAt(System.nanoTime()))
    }

    override fun onCleared() = stop()

    companion object {
        const val MAX_SECONDS = 300
    }
}
