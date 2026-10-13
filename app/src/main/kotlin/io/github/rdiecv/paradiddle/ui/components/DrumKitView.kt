// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.rdiecv.paradiddle.R
import io.github.rdiecv.paradiddle.core.Groove
import io.github.rdiecv.paradiddle.core.Hit
import io.github.rdiecv.paradiddle.core.Instrument
import kotlin.math.floor

/** Pads of the Alesis Nitro Mesh kit. */
private enum class Pad { CRASH, RIDE, HIHAT, TOM1, TOM2, TOM3, SNARE, KICK }

private enum class Limb { RIGHT_HAND, LEFT_HAND, RIGHT_FOOT }

/** Round pads in a 100 × 72 plan of the kit, seen from the drummer's seat. */
private class Round(val x: Float, val y: Float, val r: Float, val cymbal: Boolean)

private val ROUND = mapOf(
    Pad.CRASH to Round(20f, 15f, 12f, cymbal = true),
    Pad.RIDE to Round(81f, 17f, 13f, cymbal = true),
    Pad.HIHAT to Round(13f, 40f, 9.5f, cymbal = true),
    Pad.TOM1 to Round(40f, 21f, 8f, cymbal = false),
    Pad.TOM2 to Round(60f, 21f, 8f, cymbal = false),
    Pad.TOM3 to Round(74f, 45f, 9f, cymbal = false),
    Pad.SNARE to Round(33.5f, 44f, 9.5f, cymbal = false),
)
private const val PLAN_WIDTH = 100f
private const val PLAN_HEIGHT = 72f

// The kit's own colours, the same in both themes: black mesh and rubber pads on a dark stage,
// lit in the app's amber.
private val STAGE = Color(0xFF16191F)
private val PAD = Color(0xFF2A2D35)
private val PAD_RIM = Color(0xFF5B606C)
private val PAD_LINE = Color(0xFF41454F)
private val LIGHT = Color(0xFFF2C14E)
private val LABEL = Color(0xFFE8E9EE)
private val LABEL_ON_LIGHT = Color(0xFF1F2A44)

private fun Instrument.pad(): Pad = when (this) {
    Instrument.CRASH -> Pad.CRASH
    Instrument.HIHAT -> Pad.HIHAT
    Instrument.RIDE, Instrument.COWBELL -> Pad.RIDE // the setup tips put the cowbell on the ride pad
    Instrument.SNARE, Instrument.CROSS_STICK -> Pad.SNARE
    Instrument.TOM -> Pad.TOM3
    Instrument.KICK -> Pad.KICK
}

/** Right-handed kit: right hand on the cymbals, left hand on snare and toms. */
private fun Instrument.limb(): Limb = when (this) {
    Instrument.CRASH, Instrument.HIHAT, Instrument.RIDE, Instrument.COWBELL -> Limb.RIGHT_HAND
    Instrument.SNARE, Instrument.CROSS_STICK, Instrument.TOM -> Limb.LEFT_HAND
    Instrument.KICK -> Limb.RIGHT_FOOT
}

private fun Hit.strength(): Float = when (this) {
    Hit.ACCENT -> 1f
    Hit.NORMAL, Hit.OPEN -> 0.8f
    Hit.GHOST -> 0.45f
}

/** What one pad shows at this moment. */
private class Light(val strength: Float, val limb: Limb, val rimOnly: Boolean, val open: Boolean)

/**
 * Top view of the Alesis Nitro Mesh kit. Pads used by [groove] are outlined; while it plays,
 * [position] (step plus fraction, -1 when stopped) lights the pads to hit right now. [crash]
 * also lights the crash (the cue at the start of a song section).
 */
@Composable
fun DrumKitView(groove: Groove, position: Double, modifier: Modifier = Modifier, crash: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val labels = mapOf(
        Pad.CRASH to stringResource(R.string.kit_pad_crash),
        Pad.RIDE to stringResource(
            if (Instrument.COWBELL in groove.tracks) R.string.kit_pad_cowbell else R.string.kit_pad_ride,
        ),
        Pad.HIHAT to stringResource(R.string.kit_pad_hihat),
        Pad.TOM1 to stringResource(R.string.kit_pad_tom1),
        Pad.TOM2 to stringResource(R.string.kit_pad_tom2),
        Pad.TOM3 to stringResource(R.string.kit_pad_tom3),
        Pad.SNARE to stringResource(R.string.kit_pad_snare),
        Pad.KICK to stringResource(R.string.kit_pad_kick),
    )
    val limbs = mapOf(
        Limb.RIGHT_HAND to stringResource(R.string.pad_right),
        Limb.LEFT_HAND to stringResource(R.string.pad_left),
        Limb.RIGHT_FOOT to stringResource(R.string.foot_right),
    )
    val description = stringResource(R.string.kit_view_desc)
    val used = groove.tracks.keys.map { it.pad() }.toSet() + if (crash) setOf(Pad.CRASH) else emptySet()

    // Pads hit at the current step, fading out over the step.
    val lights = HashMap<Pad, Light>()
    var hiHatOpen = false
    if (position >= 0) {
        val step = floor(position).toInt().coerceIn(0, groove.steps - 1)
        val fade = 1f - 0.6f * (position - step).toFloat()
        groove.tracks.forEach { (instrument, hits) ->
            val hit = hits[step] ?: return@forEach
            if (hit == Hit.OPEN) hiHatOpen = true
            val pad = instrument.pad()
            val light = Light(hit.strength() * fade, instrument.limb(), instrument == Instrument.CROSS_STICK, hit == Hit.OPEN)
            if ((lights[pad]?.strength ?: 0f) < light.strength) lights[pad] = light
        }
        if (crash) lights[Pad.CRASH] = Light(fade, Limb.RIGHT_HAND, rimOnly = false, open = false)
    }

    Canvas(
        modifier.fillMaxWidth().aspectRatio(PLAN_WIDTH / PLAN_HEIGHT).semantics { contentDescription = description },
    ) {
        val s = size.width / PLAN_WIDTH
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        drawRoundRect(STAGE, cornerRadius = CornerRadius(3f * s))

        // Hi-hat pedal: held down (closed hi-hat) whenever the groove uses the hi-hat.
        val hiHatHeld = Pad.HIHAT in used && !hiHatOpen
        pedal(o(16f, 60f), s, alpha = if (Pad.HIHAT in used) 1f else 0.35f, lit = if (hiHatHeld) 0.3f else 0f)

        // Kick tower and its pedal: the pedal is what you press.
        val kick = lights[Pad.KICK]
        val kickAlpha = if (Pad.KICK in used) 1f else 0.35f
        drawRoundRect(
            lerp(PAD, LIGHT, (kick?.strength ?: 0f) * 0.6f).copy(alpha = kickAlpha),
            topLeft = o(44.5f, 44.5f), size = Size(15f * s, 9f * s), cornerRadius = CornerRadius(2f * s),
        )
        if (Pad.KICK in used) {
            drawRoundRect(
                LIGHT.copy(alpha = 0.5f), topLeft = o(44.5f, 44.5f), size = Size(15f * s, 9f * s),
                cornerRadius = CornerRadius(2f * s), style = Stroke(0.6f * s),
            )
        }
        label(measurer, labels.getValue(Pad.KICK), o(52f, 49f), s, (kick?.strength ?: 0f) * 0.6f, kickAlpha)
        pedal(o(52f, 63f), s, alpha = kickAlpha, lit = kick?.strength ?: 0f)
        if (kick != null) badge(measurer, limbs.getValue(kick.limb), o(57f, 58f), s)

        ROUND.forEach { (pad, shape) ->
            val light = lights[pad]
            val alpha = if (pad in used) 1f else 0.35f
            val center = o(shape.x, shape.y)
            val r = shape.r * s
            val head = if (light != null && !light.rimOnly) light.strength else 0f
            if (light != null) drawCircle(LIGHT.copy(alpha = 0.3f * light.strength), r + 2.2f * s, center)
            drawCircle(lerp(PAD, LIGHT, head).copy(alpha = alpha), r, center)
            if (shape.cymbal) {
                drawCircle(PAD_LINE.copy(alpha = alpha * (1f - head)), r * 0.62f, center, style = Stroke(0.4f * s))
                drawCircle(lerp(PAD_LINE, LIGHT, head).copy(alpha = alpha), r * 0.24f, center)
            }
            val rim = when {
                light?.rimOnly == true -> LIGHT
                pad in used -> LIGHT.copy(alpha = 0.5f)
                else -> PAD_RIM.copy(alpha = alpha)
            }
            drawCircle(rim, r, center, style = Stroke((if (light?.rimOnly == true) 1.6f else 0.7f) * s))
            if (light?.open == true) {
                // Open hi-hat: the top cymbal lifts off the bottom one.
                drawCircle(LIGHT, r + 1.2f * s, center - Offset(0f, 1.6f * s), style = Stroke(0.7f * s))
            }
            label(measurer, labels.getValue(pad), center + Offset(0f, if (shape.cymbal) r * 0.55f else 0f), s, head, alpha)
            if (light != null) badge(measurer, limbs.getValue(light.limb), center + Offset(r * 0.78f, -r * 0.78f), s)
        }
    }
}

private fun DrawScope.pedal(center: Offset, s: Float, alpha: Float, lit: Float) {
    val size = Size(7f * s, 11f * s)
    val topLeft = center - Offset(size.width / 2, size.height / 2)
    drawRoundRect(lerp(PAD, LIGHT, lit).copy(alpha = alpha), topLeft, size, CornerRadius(1.5f * s))
    drawRoundRect(PAD_RIM.copy(alpha = alpha), topLeft, size, CornerRadius(1.5f * s), style = Stroke(0.5f * s))
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, center: Offset, s: Float, lit: Float, alpha: Float) {
    val color = if (lit > 0.5f) LABEL_ON_LIGHT else LABEL.copy(alpha = alpha)
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = (3.1f * s).toSp(), fontWeight = FontWeight.Medium))
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

/** Which limb plays the lit pad: a small amber-ringed disc with R, L or RF. */
private fun DrawScope.badge(measurer: TextMeasurer, text: String, center: Offset, s: Float) {
    drawCircle(LABEL_ON_LIGHT, 3.6f * s, center)
    drawCircle(LIGHT, 3.6f * s, center, style = Stroke(0.5f * s))
    val layout = measurer.measure(text, TextStyle(color = LIGHT, fontSize = (2.9f * s).toSp(), fontWeight = FontWeight.Bold))
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}
