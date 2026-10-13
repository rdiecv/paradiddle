// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rdiecv.paradiddle.core.Hand
import io.github.rdiecv.paradiddle.core.Rudiment
import io.github.rdiecv.paradiddle.ui.theme.PadColors

/** Horizontal layout of a pattern: x of each stroke (in dp) and total width. */
internal class NotationLayout(rudiment: Rudiment, slotWidth: Float, graceSpace: Float, margin: Float) {
    val xs: FloatArray
    val width: Float

    init {
        var x = margin
        xs = FloatArray(rudiment.strokes.size)
        rudiment.strokes.forEachIndexed { i, s ->
            x += s.graces.size * graceSpace
            xs[i] = x
            x += s.slots * slotWidth
        }
        width = x + margin
    }
}

/** Number of beams (or flags) for a note lasting [slots] grid slots. */
internal fun beamsFor(slots: Int, grid: Int): Int {
    val beats = slots.toDouble() / grid
    return when {
        beats >= 0.99 -> 0
        beats >= 0.33 -> 1
        beats >= 0.16 -> 2
        else -> 3
    }
}

/**
 * Single-line percussion notation with sticking letters underneath.
 * [highlight] is the stroke index to emphasise (e.g. while playing), or -1.
 */
@Composable
fun NotationView(
    rudiment: Rudiment,
    modifier: Modifier = Modifier,
    highlight: Int = -1,
    slotWidth: Dp = 22.dp,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val layout = remember(rudiment, slotWidth) { NotationLayout(rudiment, slotWidth.value, 12f, 14f) }
    val scroll = rememberScrollState()
    val ink = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(highlight) {
        if (highlight >= 0) {
            val x = with(density) { layout.xs[highlight].dp.toPx() }
            val target = (x - scroll.viewportSize / 3f).toInt().coerceIn(0, scroll.maxValue)
            if (target < scroll.value || x > scroll.value + scroll.viewportSize * 0.8f) scroll.animateScrollTo(target)
        }
    }

    Box(modifier.horizontalScroll(scroll).semantics { contentDescription = rudiment.sticking }) {
        Canvas(Modifier.width(layout.width.dp).height(118.dp)) {
            drawNotation(rudiment, layout, highlight, ink, accentColor, measurer)
        }
    }
}

private fun DrawScope.drawNotation(
    rudiment: Rudiment,
    layout: NotationLayout,
    highlight: Int,
    ink: Color,
    accent: Color,
    measurer: TextMeasurer,
) {
    val u = density
    val staffY = 64f * u
    val stemTop = staffY - 34f * u
    val headW = 9f * u
    val headH = 6.5f * u
    val beamGap = 5f * u
    val beamThick = 3f * u
    val strokes = rudiment.strokes
    val xs = layout.xs.map { it * u }

    drawLine(ink.copy(alpha = 0.5f), Offset(0f, staffY), Offset(size.width, staffY), 1f * u)

    // Beat starting slot of each stroke, to group beams per beat.
    val beatOf = IntArray(strokes.size)
    var slot = 0
    strokes.forEachIndexed { i, s -> beatOf[i] = slot / rudiment.grid; slot += s.slots }

    strokes.forEachIndexed { i, s ->
        val x = xs[i]
        if (i == highlight) {
            drawRoundRect(
                accent.copy(alpha = 0.18f),
                topLeft = Offset(x - 9f * u, 8f * u),
                size = Size(18f * u, 104f * u),
                cornerRadius = CornerRadius(6f * u),
            )
        }
        val hand = s.hand
        if (hand == null) {
            drawRect(ink, Offset(x - 3f * u, staffY - 8f * u), Size(6f * u, 3f * u))
            return@forEachIndexed
        }
        val color = if (i == highlight) accent else ink
        val beams = beamsFor(s.slots, rudiment.grid)
        val hollow = s.slots.toDouble() / rudiment.grid >= 1.99
        val head = Offset(x - headW / 2, staffY - headH / 2)
        if (hollow) drawOval(color, head, Size(headW, headH), style = DrawStroke(1.5f * u))
        else drawOval(color, head, Size(headW, headH))
        val stemX = x + headW / 2 - 0.75f * u
        drawLine(color, Offset(stemX, staffY), Offset(stemX, stemTop), 1.5f * u)
        if (s.accent) {
            val ay = stemTop - 9f * u
            drawLine(color, Offset(x - 5f * u, ay - 4f * u), Offset(x + 5f * u, ay), 1.5f * u)
            drawLine(color, Offset(x + 5f * u, ay), Offset(x - 5f * u, ay + 4f * u), 1.5f * u)
        }
        if (s.buzz) {
            drawText(measurer, "z", Offset(stemX - 4f * u, staffY - 26f * u), TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
        // Grace notes: small slashed notes before the main note.
        s.graces.forEachIndexed { g, gh ->
            val gx = x - (s.graces.size - g) * 12f * u + 2f * u
            drawOval(color, Offset(gx - 3f * u, staffY - 2.5f * u), Size(6f * u, 5f * u))
            drawLine(color, Offset(gx + 2.5f * u, staffY), Offset(gx + 2.5f * u, staffY - 18f * u), 1f * u)
            drawLine(color, Offset(gx - 1f * u, staffY - 8f * u), Offset(gx + 6f * u, staffY - 14f * u), 1f * u)
            drawText(measurer, gh.letter.lowercase(), Offset(gx - 3f * u, staffY + 30f * u), TextStyle(color = handColor(gh), fontSize = 10.sp))
        }
        // Sticking letter.
        drawText(
            measurer, hand.letter.toString(), Offset(x - 4f * u, staffY + 12f * u),
            TextStyle(color = handColor(hand), fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        // Beams: link to the next note in the same beat, else flags for a lone note.
        val prev = (i - 1).takeIf { it >= 0 && beatOf[it] == beatOf[i] && strokes[it].hand != null }
        val next = (i + 1).takeIf { it < strokes.size && beatOf[it] == beatOf[i] && strokes[it].hand != null }
        for (level in 0 until beams) {
            val y = stemTop + level * beamGap
            val nextBeams = next?.let { beamsFor(strokes[it].slots, rudiment.grid) } ?: 0
            val prevBeams = prev?.let { beamsFor(strokes[it].slots, rudiment.grid) } ?: 0
            when {
                next != null && nextBeams > level ->
                    drawRect(color, Offset(stemX, y), Size(xs[next] + headW / 2 - 0.75f * u - stemX + 0.75f * u, beamThick))
                prev != null && prevBeams > level -> Unit // drawn by the previous note
                prev != null -> drawRect(color, Offset(stemX - 7f * u, y), Size(7f * u, beamThick))
                next != null -> drawRect(color, Offset(stemX, y), Size(7f * u, beamThick))
                else -> drawLine(color, Offset(stemX, y), Offset(stemX + 6f * u, y + 9f * u), 2f * u)
            }
        }
    }

    // Tuplet numbers over each beat group for triplet/sextuplet grids.
    if (rudiment.grid == 3 || rudiment.grid == 6) {
        val groups = strokes.indices.filter { strokes[it].hand != null }.groupBy { beatOf[it] }
        groups.values.filter { it.size > 1 }.forEach { idx ->
            val label = if (rudiment.grid == 6 && idx.all { strokes[it].slots % 2 == 0 }) "3" else rudiment.grid.toString()
            val cx = (xs[idx.first()] + xs[idx.last()]) / 2 + headW / 2
            val ty = stemTop - (if (idx.any { strokes[it].accent }) 32f else 20f) * u
            drawText(measurer, label, Offset(cx - 3f * u, ty), TextStyle(color = ink, fontSize = 11.sp))
        }
    }
}

private fun handColor(hand: Hand) = if (hand == Hand.LEFT) PadColors.left else PadColors.right
