// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rdiecv.paradiddle.ParadiddleApp
import io.github.rdiecv.paradiddle.audio.AudioEngine
import io.github.rdiecv.paradiddle.audio.Playback
import io.github.rdiecv.paradiddle.core.Feedback
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.GrooveTimeline
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.Scorer
import io.github.rdiecv.paradiddle.core.Scoring
import io.github.rdiecv.paradiddle.core.SessionResult
import io.github.rdiecv.paradiddle.core.Style
import io.github.rdiecv.paradiddle.core.Timeline
import io.github.rdiecv.paradiddle.core.audio.GrooveSound
import io.github.rdiecv.paradiddle.core.audio.SessionSound
import io.github.rdiecv.paradiddle.core.audio.Synth
import io.github.rdiecv.paradiddle.core.audio.Voice
import io.github.rdiecv.paradiddle.data.InputSource
import io.github.rdiecv.paradiddle.input.DrumInputs
import io.github.rdiecv.paradiddle.input.InputProblem
import io.github.rdiecv.paradiddle.input.LiveDrumInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * A style beat played on real drums for [BARS] bars after a one-bar count-in, scored for timing,
 * accents and (on an electronic kit) the pad. Results aren't saved: the log is per rudiment.
 */
class GroovePracticeViewModel(
    private val app: ParadiddleApp,
    val style: Style,
    val groove: Groove,
    startBpm: Int,
) : ViewModel() {
    var bpm by mutableIntStateOf(startBpm)
        private set
    var phase by mutableStateOf(Phase.STARTING)
        private set
    var timeline by mutableStateOf<Timeline?>(null)
        private set
    /** Session time when the current frame is on screen. */
    var nowNanos by mutableLongStateOf(Long.MIN_VALUE)
        private set
    var liveAccuracy by mutableDoubleStateOf(100.0)
        private set
    var result by mutableStateOf<SessionResult?>(null)
        private set
    var source by mutableStateOf(InputSource.SCREEN)
        private set
    var inputProblem by mutableStateOf<InputProblem?>(null)
        private set
    /** The last judged hit or miss. */
    var lastFeedback by mutableStateOf<Flash?>(null)
        private set

    private var playback: Playback? = null
    private var live: LiveDrumInput? = null
    private var scorer: Scorer? = null
    @Volatile private var audioDone = false

    val loops: Int get() = maxOf(1, BARS / groove.bars)
    val totalBars: Int get() = loops * groove.bars

    init {
        start()
    }

    fun start(newBpm: Int = bpm) {
        stopPlayback()
        phase = Phase.STARTING
        bpm = newBpm.coerceIn(Groove.MIN_BPM, Groove.MAX_BPM)
        viewModelScope.launch {
            val settings = app.settings.settings.first()
            source = settings.inputSource
            inputProblem = if (source == InputSource.SCREEN) InputProblem.NO_REAL_DRUMS else DrumInputs.problem(app, source)
            if (inputProblem != null) {
                phase = Phase.ERROR
                return@launch
            }
            val t = GrooveTimeline.build(groove, bpm, loops, perPad = source == InputSource.MIDI)
            val synth = Synth(AudioEngine.sampleRate, style.sound)
            val clicks = SessionSound(synth).let { it.voices(t, clickGain = settings.clickVolume, patternGain = 0f) }
            // The beat itself as a quiet guide, only on the electronic kit: a microphone would hear it.
            val guide = if (settings.guideSound && source == InputSource.MIDI) {
                val sound = GrooveSound(synth)
                val shift = SessionSound(synth).frameOf(t.countInNanos)
                sound.voices(groove, bpm, loops).map { Voice(it.startFrame + shift, it.samples, it.gain * GUIDE_GAIN) }
            } else {
                emptyList()
            }
            result = null
            lastFeedback = null
            liveAccuracy = 100.0
            timeline = t
            scorer = Scorer(t, realDrums = true)
            audioDone = false
            val p = try {
                AudioEngine.play(clicks + guide, SessionSound(synth).frameOf(t.endNanos)) { audioDone = true }
            } catch (e: IllegalStateException) {
                phase = Phase.ERROR
                return@launch
            }
            playback = p
            val offset = (settings.offsetMs(source) ?: 0) * NANOS_PER_MS
            live = DrumInputs.create(app, source)?.let { input ->
                LiveDrumInput(input, p, t, mic = source == InputSource.MICROPHONE, offset, ::onHit)
            }
            try {
                live?.start()
            } catch (e: IllegalStateException) {
                stopPlayback()
                inputProblem = if (source == InputSource.MIDI) InputProblem.NO_MIDI_DEVICE else InputProblem.MIC_UNAVAILABLE
                phase = Phase.ERROR
                return@launch
            }
            phase = Phase.PLAYING
        }
    }

    /** Called once per display frame while playing; [shownAtNanoTime] is when the frame will be on screen. */
    fun onFrame(shownAtNanoTime: Long) {
        val p = playback ?: return
        if (phase != Phase.PLAYING) return
        nowNanos = p.sessionTimeAt(shownAtNanoTime)
        scorer?.let { s ->
            s.advanceTo(p.sessionTimeAt(System.nanoTime()) - INPUT_DELIVERY_NANOS).forEach(::show)
            liveAccuracy = s.liveAccuracy
        }
        if (audioDone) finish()
    }

    /** Position in the beat (step plus fraction) for the kit drawing, -1 outside the bars. */
    fun position(): Double {
        val t = timeline ?: return -1.0
        if (phase != Phase.PLAYING || nowNanos < t.countInNanos) return -1.0
        val steps = (nowNanos - t.countInNanos).toDouble() / t.slotNanos
        if (steps >= loops * groove.steps) return -1.0
        return steps - floor(steps / groove.steps) * groove.steps
    }

    /** Bar being played (1-based), 0 in the count-in. */
    fun bar(): Int {
        val t = timeline ?: return 0
        if (nowNanos < t.countInNanos) return 0
        return ((nowNanos - t.countInNanos) / (t.beatNanos * 4) + 1).toInt().coerceAtMost(totalBars)
    }

    private fun onHit(time: Long, level: Float, pad: Instrument?) {
        val s = scorer ?: return
        val t = timeline ?: return
        if (phase != Phase.PLAYING) return
        if (time < t.countInNanos - Scoring.MAX_WINDOW_NANOS) return
        s.advanceTo(time).forEach(::show)
        s.hit(time, level, pad)?.let(::show)
        liveAccuracy = s.liveAccuracy
    }

    private fun show(f: Feedback) {
        lastFeedback = Flash(f, nowNanos)
    }

    private fun finish() {
        phase = Phase.FINISHED
        playback = null
        live?.stop()
        live = null
        result = scorer?.finish()
    }

    /** Stops without results (back button, app in background). */
    fun abort() {
        if (phase == Phase.PLAYING) {
            stopPlayback()
            phase = Phase.READY
        }
    }

    private fun stopPlayback() {
        live?.stop()
        live = null
        playback?.stop()
        playback = null
    }

    override fun onCleared() {
        stopPlayback()
    }

    companion object {
        const val BARS = 8
        private const val GUIDE_GAIN = 0.35f
        private const val INPUT_DELIVERY_NANOS = 60 * NANOS_PER_MS
    }
}
