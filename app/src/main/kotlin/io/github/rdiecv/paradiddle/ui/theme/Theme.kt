// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Navy = Color(0xFF1F2A44)
private val Amber = Color(0xFFF2C14E)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Color(0xFF8A5A00),
    tertiary = Color(0xFFB3261E),
    background = Color(0xFFFBF7F2),
    surface = Color(0xFFFBF7F2),
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Navy,
    secondary = Color(0xFFFFB95C),
    tertiary = Color(0xFFFFB4AB),
    background = Color(0xFF14161C),
    surface = Color(0xFF14161C),
)

/** Fixed colours with meaning; they don't follow the dynamic palette. */
object PadColors {
    val left = Color(0xFF3D7BD9)
    val right = Color(0xFFE0703A)
    val perfect = Color(0xFF2E9E5B)
    val good = Color(0xFFE2B33C)
    val bad = Color(0xFFD64545)
}

@Composable
fun ParadiddleTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
