// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.audio.AudioEngine
import io.github.rdiecv.paradiddle.audio.Playback
import io.github.rdiecv.paradiddle.core.Calibration
import io.github.rdiecv.paradiddle.core.Category
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.Rudiment
import io.github.rdiecv.paradiddle.core.StrokeNotation
import io.github.rdiecv.paradiddle.core.TimelineBuilder
import io.github.rdiecv.paradiddle.core.audio.SessionSound
import io.github.rdiecv.paradiddle.core.audio.Synth
import io.github.rdiecv.paradiddle.data.InputSource
import io.github.rdiecv.paradiddle.input.DrumInputs
import io.github.rdiecv.paradiddle.input.InputProblem
import io.github.rdiecv.paradiddle.input.LiveDrumInput
import io.github.rdiecv.paradiddle.ui.theme.PadColors
import kotlinx.coroutines.launch

private enum class CalState { IDLE, RUNNING, DONE, FAILED }

@Composable
fun CalibrationScreen(onBack: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(CalState.IDLE) }
    var resultMs by remember { mutableIntStateOf(0) }
    var playback by remember { mutableStateOf<Playback?>(null) }
    var live by remember { mutableStateOf<LiveDrumInput?>(null) }
    var problem by remember { mutableStateOf<InputProblem?>(null) }
    val taps = remember { mutableStateListOf<Long>() }
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)
    val source = settings?.inputSource ?: InputSource.SCREEN
    val context = LocalContext.current
    // One-beat "pattern" just to get the clicks: count-in 4 + 12 beats = 16 clicks.
    val timeline = remember {
        val beat = Rudiment("calibration", 0, "", Category.ROLL, 1, StrokeNotation.parse("R"))
        TimelineBuilder.build(beat, Calibration.BPM, repetitions = Calibration.CLICKS - TimelineBuilder.COUNT_IN_BEATS)
    }
    val clickTimes = remember { timeline.clicks.map { it.timeNanos } }

    fun stopAll() {
        live?.stop()
        live = null
        playback?.stop()
    }
    DisposableEffect(Unit) { onDispose { stopAll() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        stopAll()
        if (state == CalState.RUNNING) state = CalState.IDLE
    }

    fun start() {
        taps.clear()
        problem = DrumInputs.problem(context, source)
        if (problem != null) {
            state = CalState.FAILED
            return
        }
        val sound = SessionSound(Synth(AudioEngine.sampleRate))
        val voices = sound.voices(timeline, clickGain = 0.9f, patternGain = 0f)
        playback = try {
            AudioEngine.play(voices, sound.frameOf(timeline.endNanos)) {
                scope.launch {
                    live?.stop()
                    live = null
                    val offset = Calibration.offsetNanos(clickTimes, taps.toList())
                    if (offset == null) {
                        state = CalState.FAILED
                    } else {
                        resultMs = (offset / NANOS_PER_MS).toInt()
                        app.settings.setCalibration(resultMs, source)
                        state = CalState.DONE
                    }
                }
            }
        } catch (e: IllegalStateException) {
            state = CalState.FAILED
            return
        }
        val p = playback ?: return
        live = DrumInputs.create(context, source)?.let { input ->
            LiveDrumInput(input, p, timeline, mic = source == InputSource.MICROPHONE, offsetNanos = 0) { time, _, _ -> taps += time }
        }
        try {
            live?.start()
        } catch (e: IllegalStateException) {
            stopAll()
            problem = if (source == InputSource.MIDI) InputProblem.NO_MIDI_DEVICE else InputProblem.MIC_UNAVAILABLE
            state = CalState.FAILED
            return
        }
        state = CalState.RUNNING
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.calibration_title), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(
                    when (source) {
                        InputSource.SCREEN -> R.string.calibration_intro
                        InputSource.MICROPHONE -> R.string.calibration_intro_mic
                        InputSource.MIDI -> R.string.calibration_intro_midi
                    },
                ),
                textAlign = TextAlign.Center,
            )
            when (state) {
                CalState.DONE -> Text(
                    stringResource(R.string.calibration_result, resultMs),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                CalState.FAILED -> Text(
                    stringResource(problem?.let(::inputProblemText) ?: R.string.calibration_failed),
                    color = PadColors.bad,
                    textAlign = TextAlign.Center,
                )
                else -> Unit
            }
            if (state == CalState.RUNNING) {
                LinearProgressIndicator(
                    progress = { taps.size.coerceAtMost(Calibration.CLICKS) / Calibration.CLICKS.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Button(onClick = ::start) {
                    Text(stringResource(if (state == CalState.IDLE) R.string.calibrate else R.string.retry))
                }
            }
            val shape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(shape)
                    .background(PadColors.left.copy(alpha = if (state == CalState.RUNNING) 0.3f else 0.1f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { c ->
                                    if (c.changedToDown()) {
                                        val p = playback
                                        if (source == InputSource.SCREEN && p != null && p.isRunning) {
                                            taps += p.sessionTimeAt(AudioEngine.uptimeMillisToNanoTime(c.uptimeMillis))
                                        }
                                        c.consume()
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (source == InputSource.SCREEN) R.string.tap_here else R.string.play_here),
                    fontSize = 28.sp,
                    color = PadColors.left,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
