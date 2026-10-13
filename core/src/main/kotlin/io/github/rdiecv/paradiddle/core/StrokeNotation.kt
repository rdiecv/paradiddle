// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

/**
 * Compact sticking notation, one token per stroke, separated by spaces:
 * `R`/`L` stroke, `>` accent, `z` buzz, lowercase `r`/`l` prefix = grace notes,
 * `:n` = lasts n grid slots, `-` = rest. Example: `lR> R rL> L`.
 */
object StrokeNotation {
    private val strokeToken = Regex("""^([rl]*)([RL])([>z]*)(?::(\d+))?$""")
    private val restToken = Regex("""^-(?::(\d+))?$""")

    fun parse(text: String): List<Stroke> {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        require(tokens.isNotEmpty()) { "Empty pattern" }
        return tokens.map(::parseToken)
    }

    private fun parseToken(token: String): Stroke {
        restToken.matchEntire(token)?.let { m ->
            return Stroke(hand = null, slots = slotsOf(m.groupValues[1], token))
        }
        val m = strokeToken.matchEntire(token) ?: throw IllegalArgumentException("Bad token '$token'")
        val (graces, hand, marks, slots) = m.destructured
        require(marks.count { it == '>' } <= 1 && marks.count { it == 'z' } <= 1) { "Bad marks in '$token'" }
        return Stroke(
            hand = handOf(hand[0]),
            slots = slotsOf(slots, token),
            accent = '>' in marks,
            buzz = 'z' in marks,
            graces = graces.map(::handOf),
        )
    }

    private fun slotsOf(value: String, token: String): Int {
        if (value.isEmpty()) return 1
        val n = value.toInt()
        require(n in 1..32) { "Bad duration in '$token'" }
        return n
    }

    private fun handOf(c: Char): Hand = if (c.uppercaseChar() == 'L') Hand.LEFT else Hand.RIGHT

    fun format(strokes: List<Stroke>): String = strokes.joinToString(" ") { s ->
        buildString {
            if (s.hand == null) {
                append('-')
            } else {
                s.graces.forEach { append(it.letter.lowercaseChar()) }
                append(s.hand.letter)
                if (s.accent) append('>')
                if (s.buzz) append('z')
            }
            if (s.slots != 1) append(':').append(s.slots)
        }
    }
}
