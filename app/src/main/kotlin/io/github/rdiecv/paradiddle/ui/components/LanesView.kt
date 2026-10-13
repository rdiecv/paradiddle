// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import io.github.rdiecv.paradiddle.core.Hand
import io.github.rdiecv.paradiddle.core.Judgement
import io.github.rdiecv.paradiddle.core.NANOS_PER_MS
import io.github.rdiecv.paradiddle.core.Timeline
import io.github.rdiecv.paradiddle.ui.screens.Flash
import io.github.rdiecv.paradiddle.ui.theme.PadColors

private const val LOOKAHEAD_NANOS = 2_000L * NANOS_PER_MS
private const val FLASH_NANOS = 250L * NANOS_PER_MS

fun judgementColor(j: Judgement): Color = when (j) {
    Judgement.PERFECT, Judgement.GRACE_HIT -> PadColors.perfect
    Judgement.GOOD, Judgement.OK -> PadColors.good
    else -> PadColors.bad
}

/**
 * Two lanes (left hand, right hand) with notes falling toward a hit line near the bottom.
 * A note touches the hit line exactly at its session time [nowNanos].
 */
@Composable
fun LanesView(
    timeline: Timeline,
    nowNanos: Long,
    judgements: Map<Int, Judgement>,
    leftFlash: Flash?,
    rightFlash: Flash?,
    flashLabel: (Flash) -> String?,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(modifier) {
        val u = density
        val hitY = size.height - 18f * u
        val pxPerNano = (hitY - 8f * u) / LOOKAHEAD_NANOS
        val laneW = size.width / 2
        fun laneX(hand: Hand) = if (hand == Hand.LEFT) 0f else laneW
        fun yOf(t: Long) = hitY - (t - nowNanos) * pxPerNano

        // Lane backgrounds and divider.
        drawRect(PadColors.left.copy(alpha = 0.06f), Offset(0f, 0f), Size(laneW, size.height))
        drawRect(PadColors.right.copy(alpha = 0.06f), Offset(laneW, 0f), Size(laneW, size.height))
        drawLine(ink.copy(alpha = 0.15f), Offset(laneW, 0f), Offset(laneW, size.height), 1f * u)

        // Beat lines.
        timeline.clicks.forEach { c ->
            val y = yOf(c.timeNanos)
            if (y in 0f..hitY) drawLine(ink.copy(alpha = if (c.strong) 0.25f else 0.1f), Offset(0f, y), Offset(size.width, y), 1f * u)
        }

        // Flashes on the hit line.
        listOf(leftFlash, rightFlash).forEach { f ->
            if (f == null) return@forEach
            val age = nowNanos - f.atNanos
            if (age in 0..FLASH_NANOS) {
                val alpha = 1f - age.toFloat() / FLASH_NANOS
                val x = laneX(f.feedback.hand)
                drawRect(judgementColor(f.feedback.judgement).copy(alpha = 0.35f * alpha), Offset(x, hitY - 26f * u), Size(laneW, 34f * u))
                flashLabel(f)?.let { label ->
                    val style = TextStyle(color = judgementColor(f.feedback.judgement).copy(alpha = alpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val m = measurer.measure(label, style)
                    drawText(m, topLeft = Offset(x + (laneW - m.size.width) / 2, hitY - 54f * u))
                }
            }
        }

        drawLine(ink.copy(alpha = 0.7f), Offset(0f, hitY), Offset(size.width, hitY), 2.5f * u)

        // Notes from slightly past to the lookahead.
        val from = nowNanos - 300L * NANOS_PER_MS
        val to = nowNanos + LOOKAHEAD_NANOS
        timeline.notes.forEach { n ->
            if (n.timeNanos < from || n.timeNanos - 200 * NANOS_PER_MS > to) return@forEach
            val y = yOf(n.timeNanos)
            val j = judgements[n.index]
            val base = if (n.hand == Hand.LEFT) PadColors.left else PadColors.right
            val color = j?.let(::judgementColor) ?: base
            val h = (if (n.accent) 16f else 9f) * u
            val w = laneW * 0.56f
            val x = laneX(n.hand) + (laneW - w) / 2
            val alpha = if (n.timeNanos < nowNanos && j != null) 0.45f else 1f
            drawRoundRect(color.copy(alpha = alpha), Offset(x, y - h), Size(w, h), CornerRadius(4f * u))
            if (n.buzz) drawBuzz(x, y, w, h, measurer)
            n.graces.forEach { g ->
                val gy = yOf(g.timeNanos)
                drawCircle(
                    (if (g.hand == Hand.LEFT) PadColors.left else PadColors.right).copy(alpha = 0.8f),
                    radius = 4f * u,
                    center = Offset(laneX(g.hand) + laneW / 2, gy - 4f * u),
                )
            }
        }
    }
}

private fun DrawScope.drawBuzz(x: Float, y: Float, w: Float, h: Float, measurer: androidx.compose.ui.text.TextMeasurer) {
    val m = measurer.measure("z", TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold))
    drawText(m, topLeft = Offset(x + (w - m.size.width) / 2, y - h + (h - m.size.height) / 2))
}
