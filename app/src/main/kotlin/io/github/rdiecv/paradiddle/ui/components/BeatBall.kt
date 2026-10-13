// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.github.rdiecv.paradiddle.core.Timeline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToLong

/** Beats since the first click at session time [sessionNanos]; null before the first click. */
fun Timeline.beatsAt(sessionNanos: Long): Double? {
    val first = clicks.firstOrNull()?.timeNanos ?: return null
    return if (sessionNanos < first) null else (sessionNanos - first).toDouble() / beatNanos
}

/** How close to a beat (in beats) the ball and the end it reaches light up. */
private const val FLASH_BEATS = 0.12

/**
 * A visual metronome, like the swinging ball on the Alesis Nitro module: a ball swings from one
 * end to the other and touches an end exactly on each beat, slowing down there like a pendulum,
 * so the next beat can be seen coming. [beats] is the time in beats since the first beat
 * (2.5 = halfway between the third and fourth beat); null or negative leaves the ball at rest.
 */
@Composable
fun BeatBall(beats: Double?, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val r = 9.dp.toPx()
        val left = r
        val right = size.width - r
        val y = size.height / 2
        val b = beats?.takeIf { it >= 0 }
        // The beat being touched right now, if any. Even beats at the left end, odd at the right.
        val beat = b?.roundToLong()?.takeIf { abs(b - it) < FLASH_BEATS }
        val onBeat = beat != null
        val litLeft = beat?.rem(2) == 0L
        val litRight = beat?.rem(2) == 1L

        drawLine(ink.copy(alpha = 0.25f), Offset(left, y), Offset(right, y), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(if (litLeft) accent else ink.copy(alpha = 0.35f), radius = 3.dp.toPx() + if (litLeft) 2.dp.toPx() else 0f, center = Offset(left, y))
        drawCircle(if (litRight) accent else ink.copy(alpha = 0.35f), radius = 3.dp.toPx() + if (litRight) 2.dp.toPx() else 0f, center = Offset(right, y))

        // -cos(π·beats) is -1 on even beats, +1 on odd ones, and slows down near each end.
        val swing = b?.let { -cos(PI * it).toFloat() } ?: 0f
        val x = left + (swing + 1f) / 2f * (right - left)
        drawCircle(
            if (onBeat) accent else accent.copy(alpha = if (b != null) 0.75f else 0.3f),
            radius = if (onBeat) r else r * 0.8f,
            center = Offset(x, y),
        )
    }
}
