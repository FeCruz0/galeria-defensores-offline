package com.galeria.defensores.data.database

import android.content.Context
import androidx.room.*
import com.galeria.defensores.data.database.daos.*
import com.galeria.defensores.data.database.entities.*

@Database(
    entities = [
        CharacterEntity::class,
        TableEntity::class,
        RuleSystemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun tableDao(): TableDao
    abstract fun ruleSystemDao(): RuleSystemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "defensores_database"
                )
                .fallbackToDestructiveMigration() // For development, simplify migrations
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
