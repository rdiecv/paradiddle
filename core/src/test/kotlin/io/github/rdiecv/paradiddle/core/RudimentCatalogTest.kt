// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RudimentCatalogTest {
    private fun realFile(): String = File(System.getProperty("rudimentsJson")).readText()

    @Test
    fun realFileHasAllFortyPasRudiments() {
        val all = RudimentCatalog.parse(realFile())
        assertEquals((1..40).toList(), all.map { it.pas })
        assertEquals(40, all.map { it.id }.toSet().size)
    }

    @Test
    fun realFileCategoriesFollowPasGroups() {
        val all = RudimentCatalog.parse(realFile()).associateBy { it.pas }
        (1..15).forEach { assertEquals(Category.ROLL, all.getValue(it).category) }
        (16..19).forEach { assertEquals(Category.DIDDLE, all.getValue(it).category) }
        (20..30).forEach { assertEquals(Category.FLAM, all.getValue(it).category) }
        (31..40).forEach { assertEquals(Category.DRAG, all.getValue(it).category) }
    }

    @Test
    fun flamRudimentsHaveGraceNotes() {
        val all = RudimentCatalog.parse(realFile())
        all.filter { it.category == Category.FLAM }.forEach { r ->
            assertTrue(r.id, r.strokes.any { it.graces.isNotEmpty() })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPartialBeats() {
        RudimentCatalog.parse("""[{"id":"x","pas":1,"name":"X","category":"roll","grid":4,"strokes":"R L R"}]""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateIds() {
        RudimentCatalog.parse(
            """[{"id":"x","pas":1,"name":"X","category":"roll","grid":1,"strokes":"R"},
               {"id":"x","pas":2,"name":"Y","category":"roll","grid":1,"strokes":"L"}]"""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownCategory() {
        RudimentCatalog.parse("""[{"id":"x","pas":1,"name":"X","category":"cymbal","grid":1,"strokes":"R"}]""")
    }
}
