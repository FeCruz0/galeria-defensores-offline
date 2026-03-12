package com.galeria.defensores.data.database

import android.content.Context
import com.galeria.defensores.data.*
import com.galeria.defensores.data.database.entities.*
import com.galeria.defensores.models.*
import kotlinx.coroutines.*

object JsonToRoomMigrator {
    private const val CHAR_PREFIX = "char_"
    private const val TABLE_PREFIX = "table_"
    private const val RULE_PREFIX = "rulesystem_"

    suspend fun migrateIfNeeded(context: Context) {
        val db = AppDatabase.getDatabase(context)
        
        // Use a background scope for migration
        withContext(Dispatchers.IO) {
            migrateCharacters(context, db)
            migrateTables(context, db)
            migrateRuleSystems(context, db)
        }
    }

    private suspend fun migrateCharacters(context: Context, db: AppDatabase) {
        val files = LocalFileManager.listFiles(context, CHAR_PREFIX)
        files.forEach { file ->
            val char = LocalFileManager.readJson(context, file.name, Character::class.java)
            if (char != null) {
                // Check if already in DB to avoid duplicates if migration runs twice
                if (db.characterDao().getById(char.id) == null) {
                    db.characterDao().insert(CharacterEntity.fromCharacter(char))
                    android.util.Log.d("Migration", "Migrated Character: ${char.name}")
                }
            }
        }
    }

    private suspend fun migrateTables(context: Context, db: AppDatabase) {
        val files = LocalFileManager.listFiles(context, TABLE_PREFIX)
        files.forEach { file ->
            val table = LocalFileManager.readJson(context, file.name, Table::class.java)
            if (table != null) {
                if (db.tableDao().getById(table.id) == null) {
                    db.tableDao().insert(TableEntity.fromTable(table))
                    android.util.Log.d("Migration", "Migrated Table: ${table.name}")
                }
            }
        }
    }

    private suspend fun migrateRuleSystems(context: Context, db: AppDatabase) {
        val files = LocalFileManager.listFiles(context, RULE_PREFIX)
        files.forEach { file ->
            val rs = LocalFileManager.readJson(context, file.name, RuleSystem::class.java)
            if (rs != null) {
                if (db.ruleSystemDao().getById(rs.id) == null) {
                    db.ruleSystemDao().insert(RuleSystemEntity.fromRuleSystem(rs))
                    android.util.Log.d("Migration", "Migrated RuleSystem: ${rs.name}")
                }
            }
        }
    }
}
