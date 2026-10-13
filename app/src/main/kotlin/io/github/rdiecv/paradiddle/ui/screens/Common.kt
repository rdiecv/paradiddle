// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.rdiecv.paradiddle.ParadiddleApp
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Category
import io.github.rdiecv.paradiddle.core.GrooveFamily
import io.github.rdiecv.paradiddle.core.Instrument
import io.github.rdiecv.paradiddle.core.LocalizedText

@Composable
fun app(): ParadiddleApp = LocalContext.current.applicationContext as ParadiddleApp

@StringRes
fun Category.titleRes(): Int = when (this) {
    Category.ROLL -> R.string.category_roll
    Category.DIDDLE -> R.string.category_diddle
    Category.FLAM -> R.string.category_flam
    Category.DRAG -> R.string.category_drag
}

@StringRes
fun Category.tipRes(): Int = when (this) {
    Category.ROLL -> R.string.tip_roll
    Category.DIDDLE -> R.string.tip_diddle
    Category.FLAM -> R.string.tip_flam
    Category.DRAG -> R.string.tip_drag
}

@StringRes
fun GrooveFamily.titleRes(): Int = when (this) {
    GrooveFamily.ROCK -> R.string.family_rock
    GrooveFamily.LATIN -> R.string.family_latin
}

@StringRes
fun Instrument.shortRes(): Int = when (this) {
    Instrument.HIHAT -> R.string.inst_hihat_short
    Instrument.RIDE -> R.string.inst_ride_short
    Instrument.COWBELL -> R.string.inst_cowbell_short
    Instrument.SNARE -> R.string.inst_snare_short
    Instrument.CROSS_STICK -> R.string.inst_cross_stick_short
    Instrument.TOM -> R.string.inst_tom_short
    Instrument.KICK -> R.string.inst_kick_short
}

@StringRes
fun Instrument.nameRes(): Int = when (this) {
    Instrument.HIHAT -> R.string.inst_hihat
    Instrument.RIDE -> R.string.inst_ride
    Instrument.COWBELL -> R.string.inst_cowbell
    Instrument.SNARE -> R.string.inst_snare
    Instrument.CROSS_STICK -> R.string.inst_cross_stick
    Instrument.TOM -> R.string.inst_tom
    Instrument.KICK -> R.string.inst_kick
}

/** A text in the app's current language. */
@Composable
fun LocalizedText.text(): String = get(LocalConfiguration.current.locales[0].language)

@Composable
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> stringResource(R.string.duration_hm, h, m)
        m > 0 -> stringResource(R.string.duration_m, m)
        else -> stringResource(R.string.duration_s, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = { actions() },
    )
}
