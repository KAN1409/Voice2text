package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.TranscriptionDao
import com.example.data.local.dao.UsageStatDao
import com.example.data.local.dao.VocabularyDao
import com.example.data.local.entity.TranscriptionEntity
import com.example.data.local.entity.UsageStatEntity
import com.example.data.local.entity.VocabularyEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TranscriptionEntity::class,
        VocabularyEntity::class,
        UsageStatEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun usageStatDao(): UsageStatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voice_transcriber_db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultVocab = listOf(
                                VocabularyEntity(term = "PR", category = "Business"),
                                VocabularyEntity(term = "BOQ", category = "Engineering"),
                                VocabularyEntity(term = "Variation Order", category = "Engineering"),
                                VocabularyEntity(term = "RFI", category = "Engineering"),
                                VocabularyEntity(term = "Shop Drawing", category = "Engineering"),
                                VocabularyEntity(term = "Mockup", category = "Design"),
                                VocabularyEntity(term = "Gypsum Board", category = "Construction"),
                                VocabularyEntity(term = "Piatra", category = "Materials"),
                                VocabularyEntity(term = "Galala", category = "Materials"),
                                VocabularyEntity(term = "Palmariva", category = "Locations"),
                                VocabularyEntity(term = "Negma", category = "Locations"),
                                VocabularyEntity(term = "New Cairo", category = "Locations"),
                                VocabularyEntity(term = "Quotation", category = "Business"),
                                VocabularyEntity(term = "Contractor", category = "Business"),
                                VocabularyEntity(term = "Meeting", category = "General"),
                                VocabularyEntity(term = "Approval", category = "Business")
                            )
                            getDatabase(context).vocabularyDao().insertTerms(defaultVocab)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
