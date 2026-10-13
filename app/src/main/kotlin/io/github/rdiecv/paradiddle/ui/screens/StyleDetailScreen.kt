// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.github.rdiecv.paradiddle.core.Song
import io.github.rdiecv.paradiddle.ui.components.BulletList
import io.github.rdiecv.paradiddle.ui.components.DrumKitView
import io.github.rdiecv.paradiddle.ui.components.FormattedText
import io.github.rdiecv.paradiddle.ui.rememberFrameDelayNanos
import io.github.rdiecv.paradiddle.ui.components.GrooveGrid
import io.github.rdiecv.paradiddle.ui.components.BeatBall
import kotlin.math.roundToInt

@Composable
fun StyleDetailScreen(
    vm: StyleViewModel,
    onBack: () -> Unit,
    onLearnSong: (String) -> Unit,
    /** Practise the selected beat (its index) at a tempo on real drums. */
    onPractise: (Int, Int) -> Unit,
) {
    val style = vm.style
    val groove = vm.groove
    // Stop when the screen goes away (other tab, back) or the app goes to the background.
    DisposableEffect(vm) { onDispose { vm.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stop() }

    var position by remember { mutableDoubleStateOf(-1.0) }
    val frameDelay by rememberFrameDelayNanos(active = vm.playing)
    LaunchedEffect(vm.playing, vm.selected) {
        if (!vm.playing) {
            position = -1.0
            return@LaunchedEffect
        }
        while (true) {
            position = withFrameNanos { vm.positionAt(it + frameDelay) }
        }
    }

    Scaffold(topBar = { BackTopBar(style.name, onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (style.grooves.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    style.grooves.forEachIndexed { i, g ->
                        FilterChip(selected = vm.selected == i, onClick = { vm.select(i) }, label = { Text(g.name.text()) })
                    }
                }
            } else {
                Text(groove.name.text(), style = MaterialTheme.typography.titleMedium)
            }
            var showLegend by rememberSaveable { mutableStateOf(false) }
            BeatBall(if (position < 0) null else position / (Groove.STEPS_PER_BAR / 4))
            Card(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(12.dp)) {
                    DrumKitView(groove, position)
                    // The kit's bottom-right corner is empty, so the legend button sits there.
                    IconButton(onClick = { showLegend = true }, modifier = Modifier.align(Alignment.BottomEnd)) {
                        Icon(Icons.Outlined.Info, stringResource(R.string.kit_legend_button), tint = Color(0xFFB8BCC6))
                    }
                }
            }
            if (showLegend) {
                AlertDialog(
                    onDismissRequest = { showLegend = false },
                    confirmButton = { TextButton(onClick = { showLegend = false }) { Text(stringResource(R.string.kit_legend_close)) } },
                    title = { Text(stringResource(R.string.kit_legend_title)) },
                    text = { FormattedText(stringResource(R.string.kit_legend_body)) },
                )
            }
            // The grid right under the kit, so both can be followed while the beat plays.
            Card(Modifier.fillMaxWidth()) {
                GrooveGrid(groove, if (position < 0) -1 else position.toInt(), Modifier.padding(12.dp))
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
            OutlinedButton(onClick = { vm.stop(); onPractise(vm.selected, vm.bpm) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.practise_on_drums))
            }
            if (vm.audioError) {
                Text(stringResource(R.string.audio_error), color = MaterialTheme.colorScheme.error)
            }


            InfoCard(stringResource(R.string.how_to_play), groove.how.text())
            SongsCard(groove.songs, onLearnSong)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.nitro_setup), style = MaterialTheme.typography.titleSmall)
                    BulletList(
                        listOf(stringResource(R.string.kit_select, style.kit, NitroMesh.kitName(style.kit))) +
                            style.setup.text().lines().filter { it.isNotBlank() },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            FormattedText(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SongsCard(songs: List<Song>, onLearn: (String) -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.songs_title), style = MaterialTheme.typography.titleSmall)
            if (songs.isEmpty()) {
                Text(stringResource(R.string.songs_none), style = MaterialTheme.typography.bodyMedium)
            }
            songs.forEach { song ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                song.artist + (song.note?.let { " · " + it.text() } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { context.listenTo(song) }) {
                            Icon(Icons.Filled.PlayArrow, stringResource(R.string.song_listen, song.title))
                        }
                    }
                    if (song.lesson != null) {
                        FilledTonalButton(onClick = { onLearn(song.id) }) { Text(stringResource(R.string.learn_this_song)) }
                    }
                }
            }
        }
    }
}
