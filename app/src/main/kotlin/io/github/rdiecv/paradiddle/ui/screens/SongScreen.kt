// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.ui.components.DrumKitView
import kotlin.math.floor

@Composable
fun SongScreen(vm: SongViewModel, onBack: () -> Unit) {
    val lesson = vm.lesson
    val context = LocalContext.current
    DisposableEffect(vm) { onDispose { vm.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stop() }

    var step by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(vm.playing, vm.timeline) {
        if (!vm.playing) {
            step = null
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            step = vm.currentStep()
        }
    }
    val current = step
    val place = current?.let { if (it >= 0) vm.timeline.placeAt(floor(it).toInt()) else null }
    val shownSection = place?.section ?: vm.timeline.from
    val shownGroove = lesson.sections[shownSection].groove ?: lesson.sections.first { it.groove != null }.groove!!

    Scaffold(topBar = { BackTopBar(vm.song.title, onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(vm.song.artist, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.song_tempo, lesson.bpm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { context.listenTo(vm.song) }) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Text(stringResource(R.string.song_listen_short))
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val grooveStep = place?.let { it.grooveStep + (current - floor(current)) } ?: -1.0
                    DrumKitView(shownGroove, grooveStep, crash = place?.sectionStart == true && place.section > 0)
                    Text(
                        statusText(vm, current, place?.section, place?.bar),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }

            val slower = stringResource(R.string.slower)
            val faster = stringResource(R.string.faster)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedIconButton(
                    onClick = { vm.changeTempo(vm.bpm - 4) },
                    modifier = Modifier.semantics { contentDescription = slower },
                ) { Text("−4") }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bpm_value, vm.bpm), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (vm.bpm != lesson.bpm) {
                        Text(
                            stringResource(R.string.song_tempo_reset, lesson.bpm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { vm.changeTempo(lesson.bpm) },
                        )
                    }
                }
                OutlinedIconButton(
                    onClick = { vm.changeTempo(vm.bpm + 4) },
                    modifier = Modifier.semantics { contentDescription = faster },
                ) { Text("+4") }
            }
            Button(onClick = { if (vm.playing) vm.stop() else vm.play() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (vm.playing) R.string.stop else R.string.song_play_all))
            }
            if (vm.audioError) Text(stringResource(R.string.audio_error), color = MaterialTheme.colorScheme.error)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.song_path), style = MaterialTheme.typography.titleSmall)
                    lesson.steps.forEachIndexed { i, s ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(s.text(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.song_map),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        stringResource(R.string.song_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    lesson.sections.forEachIndexed { i, section ->
                        val active = vm.playing && place?.section == i
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    section.name.text() + " · " + pluralStringResource(R.plurals.song_bars, section.bars, section.bars),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    section.groove?.name?.text() ?: stringResource(R.string.song_no_drums),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                section.note?.let {
                                    Text(it.text(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { vm.play(from = i) }) {
                                Icon(Icons.Filled.PlayArrow, stringResource(R.string.song_play_from, section.name.text()))
                            }
                        }
                        if (i < lesson.sections.lastIndex) HorizontalDivider(Modifier.padding(start = 44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun statusText(vm: SongViewModel, step: Double?, section: Int?, bar: Int?): String {
    val sections = vm.lesson.sections
    return when {
        step == null -> stringResource(R.string.song_status_stopped)
        step < 0 -> stringResource(R.string.song_status_count_in, floor((step + Groove.STEPS_PER_BAR) / 4).toInt() + 1)
        section == null || bar == null -> ""
        else -> {
            val s = sections[section]
            val now = stringResource(R.string.song_status_now, s.name.text(), bar + 1, s.bars)
            if (section < sections.lastIndex) now + " · " + stringResource(R.string.song_status_next, sections[section + 1].name.text()) else now
        }
    }
}
