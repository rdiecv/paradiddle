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
import io.github.rdiecv.paradiddle.core.Song
import io.github.rdiecv.paradiddle.core.SongLesson
import io.github.rdiecv.paradiddle.core.SongTimeline
import io.github.rdiecv.paradiddle.core.Style
import io.github.rdiecv.paradiddle.core.audio.GrooveSound
import io.github.rdiecv.paradiddle.core.audio.Synth

/** Plays a whole song lesson, or the song from one of its sections, with the style's drum sound. */
class SongViewModel(val style: Style, val song: Song) : ViewModel() {
    val lesson: SongLesson = requireNotNull(song.lesson)
    private val sound by lazy { GrooveSound(Synth(AudioEngine.sampleRate, style.sound)) }
    private var playback: Playback? = null

    var timeline by mutableStateOf(SongTimeline(lesson.sections))
        private set

    var bpm by mutableIntStateOf(lesson.bpm)
        private set

    /** Written from the audio thread when the song ends, so it is plain Compose state. */
    var playing by mutableStateOf(false)
        private set

    var audioError by mutableStateOf(false)
        private set

    private var playingBpm = bpm

    fun changeTempo(value: Int) {
        bpm = value.coerceIn(Groove.MIN_BPM, Groove.MAX_BPM)
        if (playing) stop()
    }

    /** Plays from section [from] to the end, after a one-bar count-in. */
    fun play(from: Int = 0) {
        playback?.stop()
        timeline = SongTimeline(lesson.sections, from)
        playingBpm = bpm
        playback = try {
            AudioEngine.play(sound.songVoices(timeline, bpm), sound.songLengthFrames(timeline, bpm)) { playing = false }
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

    /** Song position in 16ths since section [SongTimeline.from] started (negative in the count-in), or null when stopped. */
    fun currentStep(): Double? {
        val p = playback ?: return null
        if (!playing) return null
        return sound.songStepAt(playingBpm, p.sessionTimeAt(System.nanoTime()))
    }

    override fun onCleared() = stop()
}
