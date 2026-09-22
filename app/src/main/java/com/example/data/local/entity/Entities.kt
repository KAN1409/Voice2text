package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val transcript: String,
    val audioFilePath: String,
    val originalFileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val provider: String,
    val model: String,
    val detectedLanguages: String,
    val processingTimeMs: Long,
    val isFavorite: Boolean = false,
    val wordCount: Int = 0
)

@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val term: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage_stats")
data class UsageStatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // e.g. "2026-09-22"
    val providerName: String,
    val durationMs: Long
)
