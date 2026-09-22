package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.local.entity.UsageStatEntity
import com.example.data.local.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTranscriptions(limit: Int): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    fun getTranscriptionById(id: Long): Flow<TranscriptionEntity?>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getTranscriptionByIdDirect(id: Long): TranscriptionEntity?

    @Query("""
        SELECT * FROM transcriptions 
        WHERE title LIKE '%' || :query || '%' 
           OR transcript LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(item: TranscriptionEntity): Long

    @Update
    suspend fun updateTranscription(item: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteTranscriptionById(id: Long)

    @Query("DELETE FROM transcriptions")
    suspend fun deleteAllTranscriptions()
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary ORDER BY term ASC")
    fun getAllVocabulary(): Flow<List<VocabularyEntity>>

    @Query("SELECT term FROM vocabulary")
    suspend fun getAllTermsDirect(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTerm(item: VocabularyEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTerms(items: List<VocabularyEntity>)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteTermById(id: Long)

    @Query("DELETE FROM vocabulary WHERE term = :term")
    suspend fun deleteTermByName(term: String)

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun getCount(): Int
}

@Dao
interface UsageStatDao {
    @Query("SELECT * FROM usage_stats WHERE dateString = :dateString")
    fun getStatsForDate(dateString: String): Flow<List<UsageStatEntity>>

    @Query("SELECT SUM(durationMs) FROM usage_stats WHERE dateString = :dateString AND providerName = :providerName")
    suspend fun getDurationForProviderToday(dateString: String, providerName: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordUsage(stat: UsageStatEntity)
}
