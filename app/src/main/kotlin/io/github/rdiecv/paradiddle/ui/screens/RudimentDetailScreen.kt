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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Rudiment
import io.github.rdiecv.paradiddle.core.Scoring
import io.github.rdiecv.paradiddle.data.Settings
import io.github.rdiecv.paradiddle.ui.components.NotationView
import kotlin.math.roundToInt

private const val DEFAULT_BPM = 60
private val REPETITION_CHOICES = listOf(4, 8, 16)

@Composable
fun RudimentDetailScreen(
    rudiment: Rudiment,
    onBack: () -> Unit,
    onStart: (bpm: Int, reps: Int, listen: Boolean) -> Unit,
) {
    val app = app()
    val sessionsFlow = remember(rudiment.id) { app.database.sessions().sessionsFor(rudiment.id) }
    val sessions by sessionsFlow.collectAsStateWithLifecycle(null)
    val settings by app.settings.settings.collectAsStateWithLifecycle(null)

    var bpm by rememberSaveable { mutableIntStateOf(-1) }
    var reps by rememberSaveable { mutableIntStateOf(-1) }
    // Start from the tempo suggested by the last session.
    LaunchedEffect(sessions) {
        val s = sessions ?: return@LaunchedEffect
        if (bpm < 0) bpm = s.firstOrNull()?.let { Scoring.suggestedBpm(it.bpm, it.accuracy) } ?: DEFAULT_BPM
    }
    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        if (reps < 0) reps = s.defaultRepetitions
    }
    val shownBpm = if (bpm < 0) DEFAULT_BPM else bpm
    val shownReps = if (reps < 0) Settings().defaultRepetitions else reps

    Scaffold(topBar = { BackTopBar("#${rudiment.pas} · ${rudiment.name}", onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                NotationView(rudiment, Modifier.padding(vertical = 8.dp))
            }

            val list = sessions.orEmpty()
            val best = list.filter { it.accuracy >= Scoring.CLEAN_ACCURACY }.maxOfOrNull { it.bpm }
            val last = list.firstOrNull()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(stringResource(R.string.stats_best), best?.let { stringResource(R.string.bpm_value, it) } ?: stringResource(R.string.none))
                Stat(stringResource(R.string.stats_last), last?.let { stringResource(R.string.accuracy_value, it.accuracy.roundToInt()) } ?: stringResource(R.string.none))
                Stat(stringResource(R.string.stats_time), formatDuration(list.sumOf { it.durationSec }))
            }

            Text(stringResource(R.string.tempo), style = MaterialTheme.typography.titleSmall)
            val slower = stringResource(R.string.slower)
            val faster = stringResource(R.string.faster)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedIconButton(
                    onClick = { bpm = (shownBpm - 4).coerceAtLeast(Scoring.MIN_BPM) },
                    modifier = Modifier.semantics { contentDescription = slower },
                ) { Text("−4") }
                Text(
                    stringResource(R.string.bpm_value, shownBpm),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedIconButton(
                    onClick = { bpm = (shownBpm + 4).coerceAtMost(Scoring.MAX_BPM) },
                    modifier = Modifier.semantics { contentDescription = faster },
                ) { Text("+4") }
            }
            Slider(
                value = shownBpm.toFloat(),
                onValueChange = { bpm = it.roundToInt() },
                valueRange = Scoring.MIN_BPM.toFloat()..Scoring.MAX_BPM.toFloat(),
            )

            Text(stringResource(R.string.repetitions), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REPETITION_CHOICES.forEach { n ->
                    FilterChip(selected = shownReps == n, onClick = { reps = n }, label = { Text("$n") })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { onStart(shownBpm, shownReps, true) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.listen))
                }
                Button(onClick = { onStart(shownBpm, shownReps, false) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.practice))
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.tip), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(rudiment.category.tipRes()), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
