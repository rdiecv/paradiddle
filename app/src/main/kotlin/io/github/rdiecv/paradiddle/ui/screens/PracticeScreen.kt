// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Hand
import io.github.rdiecv.paradiddle.core.Judgement
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.Scoring
import io.github.rdiecv.paradiddle.core.SessionResult
import io.github.rdiecv.paradiddle.ui.rememberFrameDelayNanos
import io.github.rdiecv.paradiddle.data.InputSource
import io.github.rdiecv.paradiddle.input.InputProblem
import io.github.rdiecv.paradiddle.input.MidiDrumInput
import io.github.rdiecv.paradiddle.ui.components.LanesView
import io.github.rdiecv.paradiddle.ui.components.NotationView
import io.github.rdiecv.paradiddle.ui.components.BeatBall
import io.github.rdiecv.paradiddle.ui.components.beatsAt
import io.github.rdiecv.paradiddle.ui.theme.PadColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PracticeScreen(vm: PracticeViewModel, onBack: () -> Unit, onCalibrate: () -> Unit) {
    // Keep the screen on and stop (without saving) when the app goes to the background.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.abort() }
    // Back from calibrating: start again with the new calibration.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (vm.phase == Phase.NEEDS_CALIBRATION) vm.start() }

    val frameDelay by rememberFrameDelayNanos(active = vm.phase == Phase.PLAYING)
    LaunchedEffect(vm.phase) {
        while (vm.phase == Phase.PLAYING) {
            withFrameNanos { vm.onFrame(it + frameDelay) }
        }
    }

    val title = "${vm.rudiment.name} · ${stringResource(R.string.bpm_value, vm.bpm)}"
    Scaffold(topBar = { BackTopBar(title, onBack = { vm.abort(); onBack() }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (vm.phase) {
                Phase.FINISHED -> vm.result?.let { r ->
                    ResultsContent(
                        r, vm.bpm, realDrums = vm.source != InputSource.SCREEN,
                        onRetry = { vm.start() }, onTry = { vm.start(it) }, onDone = onBack,
                    )
                }
                Phase.ERROR -> ErrorContent(vm.inputProblem, onBack)
                Phase.NEEDS_CALIBRATION -> CalibrationNeeded(onCalibrate = onCalibrate, onSkip = vm::practiseUncalibrated)
                else -> PlayingContent(vm)
            }
        }
    }
}

@Composable
private fun PlayingContent(vm: PracticeViewModel) {
    val t = vm.timeline
    val now = vm.nowNanos
    val currentNote = if (t == null || vm.phase != Phase.PLAYING) -1
    else t.notes.lastOrNull { it.timeNanos <= now + 40 * NANOS_PER_MS }?.strokeIndex ?: -1
    val early = stringResource(R.string.early)
    val late = stringResource(R.string.late)
    val wrong = stringResource(R.string.wrong_hand)
    val wrongPad = stringResource(R.string.wrong_pad)
    val miss = stringResource(R.string.miss)
    val extra = stringResource(R.string.extra)
    // On-screen pads: the lanes and the pads are one tap area, so a note can be hit where it lands.
    val onScreen = !vm.listen && vm.source == InputSource.SCREEN
    val tapModifier = Modifier.handTaps(vm::onTap)

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val rep = t?.repetitionAt(now) ?: -1
            Text(
                when {
                    vm.phase != Phase.PLAYING -> ""
                    rep < 0 -> stringResource(R.string.count_in)
                    else -> stringResource(R.string.rep_progress, rep + 1, vm.repetitions)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (vm.listen) {
                Text(stringResource(R.string.listening), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    stringResource(R.string.accuracy_value, vm.liveAccuracy.roundToInt()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        BeatBall(
            if (t == null || vm.phase != Phase.PLAYING) null else t.beatsAt(now),
            Modifier.padding(horizontal = 16.dp),
        )
        NotationView(vm.rudiment, Modifier.fillMaxWidth(), highlight = currentNote, slotWidth = 18.dp)

        if (t != null) {
            LanesView(
                timeline = t,
                nowNanos = now,
                judgements = vm.judgements,
                leftFlash = vm.leftFlash,
                rightFlash = vm.rightFlash,
                flashLabel = { f ->
                    val off = f.feedback.offsetNanos
                    when (f.feedback.judgement) {
                        Judgement.WRONG_HAND -> wrong
                        Judgement.WRONG_PAD -> wrongPad
                        Judgement.MISSED -> miss
                        Judgement.EXTRA -> extra
                        Judgement.GOOD, Judgement.OK -> if (off != null && off < 0) early else late
                        else -> null
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f).then(if (onScreen) tapModifier else Modifier),
                hitZone = vm.hitZone,
            )
        } else {
            Box(Modifier.fillMaxWidth().weight(1f))
        }

        if (vm.phase == Phase.READY) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Button(onClick = { vm.start() }) { Text(stringResource(R.string.start)) }
            }
        }
        when {
            vm.listen -> Unit
            onScreen -> Pads(tapModifier)
            else -> RealDrumsPanel(vm.source, Modifier.fillMaxWidth().height(160.dp).padding(8.dp))
        }
    }
}

/** Taps anywhere in the area: the left half is the left hand, the right half the right hand. */
private fun Modifier.handTaps(onTap: (Hand, Long) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { c ->
                if (c.changedToDown()) {
                    val hand = if (c.position.x < size.width / 2f) Hand.LEFT else Hand.RIGHT
                    onTap(hand, c.uptimeMillis)
                    c.consume()
                }
            }
        }
    }
}

/** The two pads, right under the lanes so the notes land on them. */
@Composable
private fun Pads(modifier: Modifier = Modifier) {
    val leftDesc = stringResource(R.string.pad_left_desc)
    val rightDesc = stringResource(R.string.pad_right_desc)
    Row(
        modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pad(stringResource(R.string.pad_left), leftDesc, PadColors.left, Modifier.weight(1f))
        Pad(stringResource(R.string.pad_right), rightDesc, PadColors.right, Modifier.weight(1f))
    }
}

@Composable
private fun Pad(label: String, description: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(color.copy(alpha = 0.18f))
            .border(2.dp, color.copy(alpha = 0.6f), shape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}

/** The input has never been calibrated: calibrate first, or practise anyway. */
@Composable
private fun CalibrationNeeded(onCalibrate: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.calibration_needed_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.calibration_needed_body), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onCalibrate) { Text(stringResource(R.string.calibrate_now)) }
        TextButton(onClick = onSkip) { Text(stringResource(R.string.practise_anyway)) }
    }
}

/** Why practice can't start: the audio output, or the chosen real-drum input. */
@Composable
internal fun ErrorContent(problem: InputProblem?, onBack: () -> Unit, onSettings: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(problem?.let(::inputProblemText) ?: R.string.audio_error), textAlign = TextAlign.Center)
        if (problem != null && onSettings != null) {
            Button(onClick = onSettings) { Text(stringResource(R.string.open_settings)) }
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

/** The message explaining an [InputProblem]. */
internal fun inputProblemText(problem: InputProblem): Int = when (problem) {
    InputProblem.NO_REAL_DRUMS -> R.string.input_problem_screen
    InputProblem.NO_MIDI_DEVICE -> R.string.input_problem_midi
    InputProblem.NO_MIC_PERMISSION -> R.string.input_problem_mic_permission
    InputProblem.MIC_UNAVAILABLE -> R.string.input_problem_mic
}

/** Shown instead of the on-screen pads while the app listens to real drums. */
@Composable
internal fun RealDrumsPanel(source: InputSource, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (source == InputSource.MIDI) {
            val name = remember { MidiDrumInput.deviceName(context) ?: "MIDI" }
            Text(stringResource(R.string.listening_midi, name), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        } else {
            Text(stringResource(R.string.listening_mic), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.listening_mic_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun ResultsContent(
    r: SessionResult,
    bpm: Int,
    realDrums: Boolean,
    onRetry: () -> Unit,
    onTry: (Int) -> Unit,
    onDone: () -> Unit,
) {
    val suggested = Scoring.suggestedBpm(bpm, r.accuracy)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.results_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.accuracy_value, r.accuracy.roundToInt()),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                r.isClean -> PadColors.perfect
                r.accuracy >= 70 -> PadColors.good
                else -> PadColors.bad
            },
        )
        if (r.isClean) Text(stringResource(R.string.clean), style = MaterialTheme.typography.titleLarge)

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CountRow(stringResource(R.string.perfect_label), r.perfect.toString())
            CountRow(stringResource(R.string.good_label), r.good.toString())
            CountRow(stringResource(R.string.ok_label), r.ok.toString())
            if (!realDrums) CountRow(stringResource(R.string.wrong_hand_label), r.wrongHand.toString())
            if (r.wrongPad > 0) CountRow(stringResource(R.string.wrong_pad), r.wrongPad.toString())
            CountRow(stringResource(R.string.missed_label), r.missed.toString())
            CountRow(stringResource(R.string.extra_label), r.extra.toString())
            val graces = r.graceHit + r.graceMissed
            if (graces > 0) CountRow(stringResource(R.string.graces_label), stringResource(R.string.graces_value, r.graceHit, graces))
            r.accents?.let { a -> CountRow(stringResource(R.string.accents_label), stringResource(R.string.graces_value, a.right, a.total)) }
        }

        val mean = r.meanOffsetMs.roundToInt()
        Text(
            when {
                abs(r.meanOffsetMs) < Scoring.STEADY_MS -> stringResource(R.string.tendency_steady)
                mean < 0 -> stringResource(R.string.tendency_rushing, -mean)
                else -> stringResource(R.string.tendency_dragging, mean)
            },
            textAlign = TextAlign.Center,
        )
        Text(stringResource(R.string.consistency, r.spreadMs.roundToInt()), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when {
                suggested > bpm -> stringResource(R.string.suggestion_up, suggested)
                suggested < bpm -> stringResource(R.string.suggestion_down, suggested)
                else -> stringResource(R.string.suggestion_same, bpm)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.retry)) }
            if (suggested != bpm) {
                Button(onClick = { onTry(suggested) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.try_bpm, suggested))
                }
            }
        }
        TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
    }
}

@Composable
private fun CountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
