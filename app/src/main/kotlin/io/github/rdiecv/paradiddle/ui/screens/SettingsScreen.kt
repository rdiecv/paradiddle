// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rdiecv.paradiddle.BuildConfig
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.data.InputSource
import io.github.rdiecv.paradiddle.data.Settings
import io.github.rdiecv.paradiddle.input.DrumInputs
import io.github.rdiecv.paradiddle.input.MidiDrumInput
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onCalibrate: () -> Unit) {
    val app = app()
    val scope = rememberCoroutineScope()
    val s by app.settings.settings.collectAsStateWithLifecycle(Settings())
    val context = LocalContext.current
    var micDenied by remember { mutableStateOf(false) }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) scope.launch { app.settings.setInputSource(InputSource.MICROPHONE) }
    }
    fun choose(source: InputSource) {
        micDenied = false
        if (source == InputSource.MICROPHONE && !DrumInputs.hasMicPermission(context)) {
            askMic.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            scope.launch { app.settings.setInputSource(source) }
        }
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(stringResource(R.string.input_title)) {
                InputSource.entries.forEach { source ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = s.inputSource == source, role = Role.RadioButton) { choose(source) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = s.inputSource == source, onClick = null)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(stringResource(source.label()), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(source.description()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (micDenied) Text(stringResource(R.string.input_problem_mic_permission), color = MaterialTheme.colorScheme.error)
                if (s.inputSource == InputSource.MIDI) MidiStatus()
            }
            HorizontalDivider()
            Section(stringResource(R.string.settings_calibration)) {
                Text(stringResource(R.string.calibration_for, stringResource(s.inputSource.label())), style = MaterialTheme.typography.bodySmall)
                Text(
                    s.offsetMs()?.let { stringResource(R.string.calibration_value, it) }
                        ?: stringResource(R.string.calibration_none),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCalibrate) { Text(stringResource(R.string.calibrate)) }
                    if (s.offsetMs() != null) {
                        OutlinedButton(onClick = { scope.launch { app.settings.setCalibration(null, s.inputSource) } }) {
                            Text(stringResource(R.string.reset))
                        }
                    }
                }
            }
            HorizontalDivider()
            Section(stringResource(R.string.click_volume)) {
                Slider(value = s.clickVolume, onValueChange = { v -> scope.launch { app.settings.setClickVolume(v) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.guide_sound), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.guide_sound_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = s.guideSound, onCheckedChange = { on -> scope.launch { app.settings.setGuideSound(on) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.separate_hands), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.separate_hands_desc), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = s.separateHands, onCheckedChange = { on -> scope.launch { app.settings.setSeparateHands(on) } })
            }
            Section(stringResource(R.string.default_reps)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 8, 16).forEach { n ->
                        FilterChip(
                            selected = s.defaultRepetitions == n,
                            onClick = { scope.launch { app.settings.setDefaultRepetitions(n) } },
                            label = { Text("$n") },
                        )
                    }
                }
            }
            HorizontalDivider()
            Section(stringResource(R.string.about)) {
                Text(stringResource(R.string.about_text, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun InputSource.label(): Int = when (this) {
    InputSource.SCREEN -> R.string.input_screen
    InputSource.MICROPHONE -> R.string.input_mic
    InputSource.MIDI -> R.string.input_midi
}

private fun InputSource.description(): Int = when (this) {
    InputSource.SCREEN -> R.string.input_screen_desc
    InputSource.MICROPHONE -> R.string.input_mic_desc
    InputSource.MIDI -> R.string.input_midi_desc
}

/** Which kit is plugged in, kept up to date as cables come and go. */
@Composable
private fun MidiStatus() {
    val context = LocalContext.current
    var name by remember { mutableStateOf(MidiDrumInput.deviceName(context)) }
    DisposableEffect(Unit) {
        val unwatch = MidiDrumInput.watch(context) { name = MidiDrumInput.deviceName(context) }
        onDispose { unwatch() }
    }
    Text(
        name?.let { stringResource(R.string.midi_connected, it) } ?: stringResource(R.string.midi_none),
        style = MaterialTheme.typography.bodyMedium,
        color = if (name == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}
