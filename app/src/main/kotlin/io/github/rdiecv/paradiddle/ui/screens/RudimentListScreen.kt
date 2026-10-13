// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Scoring
import io.github.rdiecv.paradiddle.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RudimentListScreen(
    onOpen: (String) -> Unit,
    onLog: () -> Unit,
    onSettings: () -> Unit,
    onCalibrate: () -> Unit,
) {
    val app = app()
    val bestFlow = remember { app.database.sessions().bestCleanBpm(Scoring.CLEAN_ACCURACY) }
    val best by bestFlow.collectAsStateWithLifecycle(emptyList())
    val settings by app.settings.settings.collectAsStateWithLifecycle(Settings(calibrationOffsetMs = 0))
    val bestById = best.associate { it.rudimentId to it.bpm }
    val groups = app.rudiments.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onLog) { Icon(Icons.Filled.DateRange, stringResource(R.string.log)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (settings.calibrationOffsetMs == null) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.calibration_banner))
                            TextButton(onClick = onCalibrate, modifier = Modifier.align(Alignment.End)) {
                                Text(stringResource(R.string.calibrate))
                            }
                        }
                    }
                }
            }
            groups.forEach { (category, rudiments) ->
                item(key = category.name) {
                    Text(
                        stringResource(category.titleRes()),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                items(rudiments, key = { it.id }) { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpen(r.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "${r.pas}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                r.sticking,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        val b = bestById[r.id]
                        Text(
                            if (b != null) stringResource(R.string.bpm_value, b) else stringResource(R.string.none),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (b != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}
