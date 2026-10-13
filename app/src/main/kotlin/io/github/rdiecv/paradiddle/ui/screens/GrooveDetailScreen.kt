// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.NitroMesh
import io.github.rdiecv.paradiddle.ui.components.GrooveGrid
import kotlin.math.roundToInt

@Composable
fun GrooveDetailScreen(vm: GrooveViewModel, onBack: () -> Unit) {
    val groove = vm.groove
    // Stop when the screen goes away (other tab, back) or the app goes to the background.
    DisposableEffect(vm) { onDispose { vm.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stop() }

    var step by remember { mutableIntStateOf(-1) }
    LaunchedEffect(vm.playing) {
        if (!vm.playing) {
            step = -1
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            step = vm.currentStep()
        }
    }

    Scaffold(topBar = { BackTopBar(groove.name, onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                GrooveGrid(groove, step, Modifier.padding(12.dp))
            }

            Text(
                stringResource(R.string.tempo) + " · " + stringResource(R.string.bpm_range, groove.minBpm, groove.maxBpm),
                style = MaterialTheme.typography.titleSmall,
            )
            val slower = stringResource(R.string.slower)
            val faster = stringResource(R.string.faster)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedIconButton(
                    onClick = { vm.changeTempo(vm.bpm - 4); vm.applyTempo() },
                    modifier = Modifier.semantics { contentDescription = slower },
                ) { Text("−4") }
                Text(
                    stringResource(R.string.bpm_value, vm.bpm),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedIconButton(
                    onClick = { vm.changeTempo(vm.bpm + 4); vm.applyTempo() },
                    modifier = Modifier.semantics { contentDescription = faster },
                ) { Text("+4") }
            }
            Slider(
                value = vm.bpm.toFloat(),
                onValueChange = { vm.changeTempo(it.roundToInt()) },
                onValueChangeFinished = vm::applyTempo,
                valueRange = Groove.MIN_BPM.toFloat()..Groove.MAX_BPM.toFloat(),
            )
            Button(onClick = vm::toggle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (vm.playing) R.string.stop else R.string.play))
            }
            if (vm.audioError) {
                Text(stringResource(R.string.audio_error), color = MaterialTheme.colorScheme.error)
            }

            val texts = grooveTexts(groove.id)
            if (texts != null) {
                InfoCard(stringResource(R.string.how_to_play), stringResource(texts.first))
            }
            InfoCard(
                stringResource(R.string.nitro_setup),
                stringResource(R.string.kit_select, groove.kit, NitroMesh.kitName(groove.kit)) +
                    (texts?.let { "\n\n" + stringResource(it.second) } ?: ""),
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
