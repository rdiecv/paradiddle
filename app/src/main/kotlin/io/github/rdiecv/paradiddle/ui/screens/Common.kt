// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
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
import io.github.rdiecv.paradiddle.core.Song

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
    Instrument.CRASH -> R.string.inst_crash_short
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
    Instrument.CRASH -> R.string.inst_crash
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

/**
 * Asks the phone's music apps to find and play [song] (Android's standard "play from search"),
 * falling back to a web search. No service is named, so whichever app the user has answers.
 */
fun Context.listenTo(song: Song) {
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
