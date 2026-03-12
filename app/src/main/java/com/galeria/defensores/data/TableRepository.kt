package com.galeria.defensores.data

import com.galeria.defensores.models.Table

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object TableRepository {
    private const val PREFIX = "table_"
    
    // Similar Context strategy as CharacterRepository
    private lateinit var appContext: android.content.Context
    private lateinit var database: com.galeria.defensores.data.database.AppDatabase

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        database = com.galeria.defensores.data.database.AppDatabase.getDatabase(appContext)
    }
    
    private fun getContext(): android.content.Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    suspend fun getTables(): List<Table> {
        if (!::database.isInitialized) return emptyList()
        return database.tableDao().getAll().map { it.toTable() }
    }

    suspend fun getTable(id: String): Table? {
        if (!::database.isInitialized) return null
        return database.tableDao().getById(id)?.toTable()
    }

    suspend fun addTable(table: Table): Boolean {
        return updateTable(table)
    }

    suspend fun updateTable(table: Table): Boolean {
        if (!::database.isInitialized) return false
        database.tableDao().insert(com.galeria.defensores.data.database.entities.TableEntity.fromTable(table))
        return true
    }

    suspend fun deleteTable(id: String): Boolean {
        if (!::database.isInitialized) return false
        
        // 1. Delete pending notifications (Mocked/Local)
        com.galeria.defensores.data.NotificationRepository.deleteNotificationsForTable(id)
        
        // 2. Unlink characters
        com.galeria.defensores.data.CharacterRepository.unlinkCharactersFromTable(id)
        
        // 3. Delete from DB
        database.tableDao().deleteById(id)
        return true
    }

    suspend fun checkUserHasActiveTables(userId: String): Boolean {
        val tables = getTables()
        return tables.any { it.masterId == userId && it.players.isNotEmpty() }
    }

    suspend fun addPlayerToTable(tableId: String, playerId: String) {
        val table = getTable(tableId) ?: return
        if (!table.players.contains(playerId)) {
            val updatedTable = table.copy(players = (table.players + playerId).toMutableList())
            updateTable(updatedTable)
        }
    }

    suspend fun addRollToHistory(tableId: String, roll: com.galeria.defensores.models.RollResult) {
        val table = getTable(tableId) ?: return
        val updatedHistory = table.rollHistory.toMutableList()
        updatedHistory.add(roll)
        if (updatedHistory.size > 50) {
            updatedHistory.removeAt(0)
        }
        val updatedTable = table.copy(rollHistory = updatedHistory)
        updateTable(updatedTable)
    }

    suspend fun clearRollHistory(tableId: String): Boolean {
         val table = getTable(tableId) ?: return false
         val updatedTable = table.copy(rollHistory = mutableListOf())
         updateTable(updatedTable)
         return true
    }

    suspend fun broadcastVisualRoll(tableId: String, visualRoll: com.galeria.defensores.models.VisualRoll) {
         val table = getTable(tableId) ?: return
         val updatedTable = table.copy(lastVisualRoll = visualRoll)
         updateTable(updatedTable)
    }

    fun getTableFlow(id: String): Flow<Table?> = callbackFlow {
        // Since we are not using StateFlow/Live-Query yet for simplicity in this migration,
        // we'll emit the current value. Full Flow integration with Room 'Query as Flow' 
        // can be a follow-up optimization.
        val table = getTable(id)
        trySend(table)
        close() 
        awaitClose { }
    }
}
