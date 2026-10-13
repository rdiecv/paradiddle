// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import io.github.rdiecv.paradiddle.ui.components.DrumKitView
import io.github.rdiecv.paradiddle.ui.components.GrooveGrid
import kotlin.math.roundToInt

@Composable
fun StyleDetailScreen(vm: StyleViewModel, onBack: () -> Unit) {
    val style = vm.style
    val groove = vm.groove
    // Stop when the screen goes away (other tab, back) or the app goes to the background.
    DisposableEffect(vm) { onDispose { vm.stop() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.stop() }

    var position by remember { mutableDoubleStateOf(-1.0) }
    LaunchedEffect(vm.playing, vm.selected) {
        if (!vm.playing) {
            position = -1.0
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            position = vm.currentPosition()
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
                    text = { Text(stringResource(R.string.kit_legend_body)) },
                )
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

            Card(Modifier.fillMaxWidth()) {
                GrooveGrid(groove, if (position < 0) -1 else position.toInt(), Modifier.padding(12.dp))
            }

            InfoCard(stringResource(R.string.how_to_play), groove.how.text())
            SongsCard(groove.songs)
            InfoCard(
                stringResource(R.string.nitro_setup),
                stringResource(R.string.kit_select, style.kit, NitroMesh.kitName(style.kit)) + "\n\n" + style.setup.text(),
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

@Composable
private fun SongsCard(songs: List<Song>) {
    var showGuide by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.songs_title), style = MaterialTheme.typography.titleSmall)
            if (songs.isEmpty()) {
                Text(stringResource(R.string.songs_none), style = MaterialTheme.typography.bodyMedium)
            } else {
                val context = LocalContext.current
                songs.forEach { song ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                song.artist + (song.note?.let { " · " + it.text() } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalIconButton(onClick = { context.listenTo(song) }) {
                            Icon(Icons.Filled.PlayArrow, stringResource(R.string.song_listen, song.title))
                        }
                    }
                }
                Text(
                    stringResource(R.string.songs_caveat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().clickable { showGuide = !showGuide },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.learn_song_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(
                    if (showGuide) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(if (showGuide) R.string.learn_song_hide else R.string.learn_song_show),
                )
            }
            if (showGuide) {
                Text(stringResource(R.string.learn_song_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Asks the phone's music apps to find and play [song] (Android's standard "play from search"),
 * falling back to a web search. No service is named, so whichever app the user has answers.
 */
private fun Context.listenTo(song: Song) {
    val query = "${song.title} ${song.artist}"
    val play = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
        .putExtra(MediaStore.EXTRA_MEDIA_TITLE, song.title)
        .putExtra(MediaStore.EXTRA_MEDIA_ARTIST, song.artist)
        .putExtra(SearchManager.QUERY, query)
    val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
    for (intent in listOf(play, search)) {
        try {
            startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            // Try the next way.
        }
    }
    Toast.makeText(this, R.string.song_no_app, Toast.LENGTH_LONG).show()
}

