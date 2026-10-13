// SPDX-FileCopyrightText: 2026 The Paradiddle authors
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.rdiecv.paradiddle.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "practice_session")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rudimentId: String,
    /** Epoch milliseconds. */
    val startedAt: Long,
    val bpm: Int,
    val repetitions: Int,
    val accuracy: Double,
    val meanOffsetMs: Double,
    val spreadMs: Double,
    val perfect: Int,
    val good: Int,
    val ok: Int,
    val wrongHand: Int,
    val missed: Int,
    val extra: Int,
    val durationSec: Int,
)

data class RudimentBest(val rudimentId: String, val bpm: Int)

@Dao
interface PracticeSessionDao {
    @Insert
    suspend fun insert(session: PracticeSession): Long

    @Query("SELECT rudimentId, MAX(bpm) AS bpm FROM practice_session WHERE accuracy >= :clean GROUP BY rudimentId")
    fun bestCleanBpm(clean: Double): Flow<List<RudimentBest>>

    @Query("SELECT * FROM practice_session WHERE rudimentId = :rudimentId ORDER BY startedAt DESC")
    fun sessionsFor(rudimentId: String): Flow<List<PracticeSession>>

    @Query("SELECT * FROM practice_session ORDER BY startedAt DESC")
    fun all(): Flow<List<PracticeSession>>
}

@Database(entities = [PracticeSession::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): PracticeSessionDao
}
