// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.ui.components.FormattedText

private val SECTIONS = listOf(
    R.string.kit_connect_title to R.string.kit_connect_body,
    R.string.kit_choose_title to R.string.kit_choose_body,
    R.string.kit_click_title to R.string.kit_click_body,
    R.string.kit_pads_title to R.string.kit_pads_body,
    R.string.kit_practice_title to R.string.kit_practice_body,
    R.string.kit_learning_title to R.string.kit_learning_body,
)

/** Read-only guide to setting up the Alesis Nitro Mesh kit for practice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitGuideScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.kit_title)) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.kit_intro), style = MaterialTheme.typography.bodyMedium)
            SECTIONS.forEach { (title, body) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                        FormattedText(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                stringResource(R.string.kit_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
