// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.Hit
import io.github.rdiecv.paradiddle.ui.screens.nameRes
import io.github.rdiecv.paradiddle.ui.screens.shortRes

private val LABEL_WIDTH = 32.dp

/**
 * Drum grid of a groove: one block per bar, one row per instrument and one column per 16th
 * note. [currentStep] (0-based over the whole groove, -1 for none) is highlighted.
 */
@Composable
fun GrooveGrid(groove: Groove, currentStep: Int, modifier: Modifier = Modifier) {
    val counts = stringResource(R.string.grid_counts).split(' ')
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(groove.bars) { bar ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (groove.bars > 1) {
                    Text(stringResource(R.string.bar_n, bar + 1), style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(LABEL_WIDTH))
                    counts.forEachIndexed { i, c ->
                        Text(
                            c,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (i % 4 == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                groove.tracks.forEach { (instrument, hits) ->
                    val name = stringResource(instrument.nameRes())
                    Row(Modifier.fillMaxWidth().semantics { contentDescription = name }, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(instrument.shortRes()),
                            Modifier.width(LABEL_WIDTH),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        BarRow(
                            hits.subList(bar * Groove.STEPS_PER_BAR, (bar + 1) * Groove.STEPS_PER_BAR),
                            current = currentStep - bar * Groove.STEPS_PER_BAR,
                            modifier = Modifier.weight(1f).height(26.dp),
                        )
                    }
                }
            }
        }
        val legend = groove.tracks.keys.map { "${stringResource(it.shortRes())} ${stringResource(it.nameRes())}" }.joinToString(" · ")
        Text(legend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.grid_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One bar of one instrument; [current] is the highlighted step within this bar (may be outside it). */
@Composable
private fun BarRow(hits: List<Hit?>, current: Int, modifier: Modifier) {
    val beatShade = MaterialTheme.colorScheme.surfaceVariant
    val playhead = MaterialTheme.colorScheme.primaryContainer
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val cell = size.width / hits.size
        val r = minOf(cell, size.height) / 2
        for (beat in 0 until hits.size / 4) {
            if (beat % 2 == 0) {
                drawRect(beatShade, Offset(beat * 4 * cell, 0f), Size(4 * cell, size.height))
            }
        }
        if (current in hits.indices) {
            drawRect(playhead, Offset(current * cell, 0f), Size(cell, size.height))
        }
        hits.forEachIndexed { i, hit ->
            val center = Offset(i * cell + cell / 2, size.height / 2)
            when (hit) {
                null -> Unit
                Hit.ACCENT -> drawCircle(accent, r * 0.8f, center)
                Hit.NORMAL -> drawCircle(ink, r * 0.55f, center)
                Hit.GHOST -> drawCircle(ink.copy(alpha = 0.55f), r * 0.3f, center)
                Hit.OPEN -> drawCircle(ink, r * 0.55f, center, style = Stroke(width = r * 0.22f))
            }
        }
    }
}
