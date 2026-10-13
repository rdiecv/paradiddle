// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private const val BULLET = "• "

/**
 * Text written as lines: a line starting with "• " is a list item, any other line a paragraph.
 * A wrapped list item stays aligned with its own text instead of running under the bullet.
 */
@Composable
fun FormattedText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (line.startsWith(BULLET)) BulletItem(line.removePrefix(BULLET), style) else Text(line, style = style)
        }
    }
}

/** [items] as a bulleted list. */
@Composable
fun BulletList(items: List<String>, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { BulletItem(it, style) }
    }
}

@Composable
private fun BulletItem(text: String, style: TextStyle) {
    Row {
        Text("•", style = style, modifier = Modifier.width(16.dp))
        Text(text, style = style, modifier = Modifier.weight(1f))
    }
}
