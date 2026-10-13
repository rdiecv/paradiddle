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
    val taps = remember { mutableStateListOf<Long>() }
    // One-beat "pattern" just to get the clicks: count-in 4 + 12 beats = 16 clicks.
    val timeline = remember {
        val beat = Rudiment("calibration", 0, "", Category.ROLL, 1, StrokeNotation.parse("R"))
        TimelineBuilder.build(beat, Calibration.BPM, repetitions = Calibration.CLICKS - TimelineBuilder.COUNT_IN_BEATS)
    }
    val clickTimes = remember { timeline.clicks.map { it.timeNanos } }

    DisposableEffect(Unit) { onDispose { playback?.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        playback?.stop()
        if (state == CalState.RUNNING) state = CalState.IDLE
    }

    fun start() {
        taps.clear()
        val sound = SessionSound(Synth(AudioEngine.sampleRate))
        val voices = sound.voices(timeline, clickGain = 0.9f, patternGain = 0f)
        playback = try {
            AudioEngine.play(voices, sound.frameOf(timeline.endNanos)) {
                scope.launch {
                    val offset = Calibration.offsetNanos(clickTimes, taps.toList())
                    if (offset == null) {
                        state = CalState.FAILED
                    } else {
                        resultMs = (offset / NANOS_PER_MS).toInt()
                        app.settings.setCalibration(resultMs)
                        state = CalState.DONE
                    }
                }
            }
        } catch (e: IllegalStateException) {
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
            Text(stringResource(R.string.calibration_intro), textAlign = TextAlign.Center)
            when (state) {
                CalState.DONE -> Text(
                    stringResource(R.string.calibration_result, resultMs),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                CalState.FAILED -> Text(stringResource(R.string.calibration_failed), color = PadColors.bad, textAlign = TextAlign.Center)
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
                                        if (p != null && p.isRunning) {
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
                Text(stringResource(R.string.tap_here), fontSize = 28.sp, color = PadColors.left, fontWeight = FontWeight.Bold)
            }
        }
    }
}
