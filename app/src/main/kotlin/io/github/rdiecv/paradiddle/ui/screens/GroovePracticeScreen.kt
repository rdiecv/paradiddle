// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Judgement
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.ui.components.DrumKitView
import io.github.rdiecv.paradiddle.ui.components.judgementColor
import io.github.rdiecv.paradiddle.ui.rememberFrameDelayNanos
import kotlin.math.roundToInt

private const val FEEDBACK_NANOS = 400L * NANOS_PER_MS

@Composable
fun GroovePracticeScreen(vm: GroovePracticeViewModel, onBack: () -> Unit, onSettings: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.abort() }

    val frameDelay by rememberFrameDelayNanos(active = vm.phase == Phase.PLAYING)
    LaunchedEffect(vm.phase) {
        while (vm.phase == Phase.PLAYING) {
            withFrameNanos { vm.onFrame(it + frameDelay) }
        }
    }

    val title = "${vm.groove.name.text()} · ${stringResource(R.string.bpm_value, vm.bpm)}"
    Scaffold(topBar = { BackTopBar(title, onBack = { vm.abort(); onBack() }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (vm.phase) {
                Phase.FINISHED -> vm.result?.let { r ->
                    ResultsContent(r, vm.bpm, realDrums = true, onRetry = { vm.start() }, onTry = { vm.start(it) }, onDone = onBack)
                }
                Phase.ERROR -> ErrorContent(vm.inputProblem, onBack, onSettings)
                else -> PlayingGroove(vm)
            }
        }
    }
}

@Composable
private fun PlayingGroove(vm: GroovePracticeViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val bar = vm.bar()
            Text(
                when {
                    vm.phase != Phase.PLAYING -> ""
                    bar == 0 -> stringResource(R.string.count_in)
                    else -> stringResource(R.string.bar_progress, bar, vm.totalBars)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.accuracy_value, vm.liveAccuracy.roundToInt()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Card(Modifier.fillMaxWidth()) {
            DrumKitView(vm.groove, vm.position(), Modifier.padding(12.dp))
        }
        FeedbackText(vm)
        if (vm.phase == Phase.READY) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = { vm.start() }) { Text(stringResource(R.string.start)) }
            }
        }
        RealDrumsPanel(vm.source, Modifier.fillMaxWidth().height(140.dp))
    }
}

/** The last hit's judgement, fading out. */
@Composable
private fun FeedbackText(vm: GroovePracticeViewModel) {
    val f = vm.lastFeedback
    val age = if (f == null) Long.MAX_VALUE else vm.nowNanos - f.atNanos
    val text = when {
        f == null || age !in 0..FEEDBACK_NANOS -> ""
        else -> when (f.feedback.judgement) {
            Judgement.PERFECT -> stringResource(R.string.perfect_label)
            Judgement.GOOD, Judgement.OK ->
                stringResource(if ((f.feedback.offsetNanos ?: 0) < 0) R.string.early else R.string.late)
            Judgement.WRONG_PAD -> stringResource(R.string.wrong_pad)
            Judgement.MISSED -> stringResource(R.string.miss)
            Judgement.EXTRA -> stringResource(R.string.extra)
            else -> ""
        }
    }
    val alpha = if (text.isEmpty()) 0f else 1f - age.toFloat() / FEEDBACK_NANOS
    Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
        if (f != null && text.isNotEmpty()) {
            Text(text, color = judgementColor(f.feedback.judgement).copy(alpha = alpha), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
