// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rdiecv.paradiddle.R
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun LogScreen(onBack: () -> Unit) {
    val app = app()
    val flow = remember { app.database.sessions().all() }
    val sessions by flow.collectAsStateWithLifecycle(emptyList())
    val names = remember { app.rudiments.associate { it.id to it.name } }
    val zone = ZoneId.systemDefault()
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.FULL) }
    val byDay = sessions.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.log), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (sessions.isEmpty()) {
                item { Text(stringResource(R.string.log_empty), Modifier.padding(vertical = 24.dp)) }
                return@LazyColumn
            }
            item {
                Text(
                    stringResource(R.string.total_time, formatDuration(sessions.sumOf { it.durationSec })),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            byDay.forEach { (day, list) ->
                item(key = day.toString()) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                        Text(
                            dateFormat.format(Date.from(day.atStartOfDay(zone).toInstant())),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatDuration(list.sumOf { it.durationSec }), style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(list, key = { it.id }) { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(names[s.rudimentId] ?: s.rudimentId, Modifier.weight(1f))
                        Text(
                            stringResource(R.string.session_line, s.bpm, s.accuracy.roundToInt()),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
