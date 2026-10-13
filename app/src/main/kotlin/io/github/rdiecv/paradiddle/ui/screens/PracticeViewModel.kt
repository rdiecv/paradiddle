// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.rdiecv.paradiddle.ParadiddleApp
import io.github.rdiecv.paradiddle.audio.AudioEngine
import io.github.rdiecv.paradiddle.audio.Playback
import io.github.rdiecv.paradiddle.core.Feedback
import io.github.rdiecv.paradiddle.core.Hand
import io.github.rdiecv.paradiddle.core.Judgement
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.NANOS_PER_SECOND
import io.github.rdiecv.paradiddle.core.Rudiment
import io.github.rdiecv.paradiddle.core.Scorer
import io.github.rdiecv.paradiddle.core.Scoring
import io.github.rdiecv.paradiddle.core.SessionResult
import io.github.rdiecv.paradiddle.core.Timeline
import io.github.rdiecv.paradiddle.core.TimelineBuilder
import io.github.rdiecv.paradiddle.core.audio.SessionSound
import io.github.rdiecv.paradiddle.core.audio.Synth
import io.github.rdiecv.paradiddle.data.PracticeSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Phase { STARTING, READY, PLAYING, FINISHED, ERROR }

/** Last tap/miss shown on a lane, with the session time it happened. */
data class Flash(val feedback: Feedback, val atNanos: Long)

class PracticeViewModel(
    private val app: ParadiddleApp,
    val rudiment: Rudiment,
    startBpm: Int,
    val repetitions: Int,
    /** Listen mode: plays the pattern, no scoring and nothing saved. */
    val listen: Boolean,
) : ViewModel() {
    var bpm by mutableIntStateOf(startBpm)
        private set
    var phase by mutableStateOf(Phase.STARTING)
        private set
    var timeline by mutableStateOf<Timeline?>(null)
        private set
    /** Session time "now", updated every frame while playing. */
    var nowNanos by mutableLongStateOf(Long.MIN_VALUE)
        private set
    var liveAccuracy by mutableDoubleStateOf(100.0)
        private set
    var result by mutableStateOf<SessionResult?>(null)
        private set
    val judgements = mutableStateMapOf<Int, Judgement>()
    var leftFlash by mutableStateOf<Flash?>(null)
        private set
    var rightFlash by mutableStateOf<Flash?>(null)
        private set

    private var playback: Playback? = null
    private var scorer: Scorer? = null
    private var calibrationNanos = 0L
    private var startedAtMillis = 0L
    @Volatile private var audioDone = false

    init {
        start()
    }

    fun start(newBpm: Int = bpm) {
        stopPlayback()
        phase = Phase.STARTING
        bpm = newBpm.coerceIn(Scoring.MIN_BPM, Scoring.MAX_BPM)
        viewModelScope.launch {
            val settings = app.settings.settings.first()
            calibrationNanos = (settings.calibrationOffsetMs ?: 0) * NANOS_PER_MS
            val t = TimelineBuilder.build(rudiment, bpm, repetitions)
            val sound = SessionSound(Synth(AudioEngine.sampleRate))
            val voices = if (listen) {
                sound.voices(t, clickGain = settings.clickVolume * 0.6f, patternGain = 0.9f)
            } else {
                sound.voices(t, clickGain = settings.clickVolume, patternGain = if (settings.guideSound) 0.25f else 0f)
            }
            judgements.clear()
            leftFlash = null
            rightFlash = null
            result = null
            liveAccuracy = 100.0
            timeline = t
            scorer = if (listen) null else Scorer(t)
            audioDone = false
            playback = try {
                AudioEngine.play(voices, sound.frameOf(t.endNanos)) { audioDone = true }
            } catch (e: IllegalStateException) {
                phase = Phase.ERROR
                return@launch
            }
            startedAtMillis = System.currentTimeMillis()
            phase = Phase.PLAYING
        }
    }

    /** Called once per display frame while playing. */
    fun onFrame() {
        val p = playback ?: return
        if (phase != Phase.PLAYING) return
        val now = p.sessionTimeAt(System.nanoTime())
        nowNanos = now
        scorer?.let { s ->
            // Lag behind "now" so taps delivered a little late can still match their note.
            s.advanceTo(now - calibrationNanos - INPUT_DELIVERY_NANOS).forEach(::show)
            liveAccuracy = s.liveAccuracy
        }
        if (audioDone) finish()
    }

    fun onTap(hand: Hand, uptimeMillis: Long) {
        val p = playback ?: return
        val s = scorer ?: return
        if (phase != Phase.PLAYING) return
        val t = timeline ?: return
        val time = p.sessionTimeAt(AudioEngine.uptimeMillisToNanoTime(uptimeMillis)) - calibrationNanos
        // Taps during the count-in are warm-up, not extra taps.
        if (time < t.countInNanos - Scoring.MAX_WINDOW_NANOS - Scoring.GRACE_BEFORE_NANOS) return
        s.advanceTo(time).forEach(::show)
        show(s.tap(hand, time))
        liveAccuracy = s.liveAccuracy
    }

    private fun show(f: Feedback) {
        f.noteIndex?.let { judgements[it] = f.judgement }
        val flash = Flash(f, nowNanos)
        if (f.hand == Hand.LEFT) leftFlash = flash else rightFlash = flash
    }

    private fun finish() {
        phase = Phase.FINISHED
        playback = null
        val s = scorer ?: run {
            // Listen mode: go back to ready so it can be played again.
            phase = Phase.READY
            return
        }
        val r = s.finish()
        result = r
        val t = timeline ?: return
        viewModelScope.launch {
            app.database.sessions().insert(
                PracticeSession(
                    rudimentId = rudiment.id,
                    startedAt = startedAtMillis,
                    bpm = bpm,
                    repetitions = repetitions,
                    accuracy = r.accuracy,
                    meanOffsetMs = r.meanOffsetMs,
                    spreadMs = r.spreadMs,
                    perfect = r.perfect,
                    good = r.good,
                    ok = r.ok,
                    wrongHand = r.wrongHand,
                    missed = r.missed,
                    extra = r.extra,
                    durationSec = (t.endNanos / NANOS_PER_SECOND).toInt(),
                ),
            )
        }
    }

    /** Stops without saving (back button, app in background). */
    fun abort() {
        if (phase == Phase.PLAYING) {
            stopPlayback()
            phase = Phase.READY
        }
    }

    private fun stopPlayback() {
        playback?.stop()
        playback = null
    }

    override fun onCleared() {
        stopPlayback()
    }

    private companion object {
        const val INPUT_DELIVERY_NANOS = 60 * NANOS_PER_MS
    }
}
