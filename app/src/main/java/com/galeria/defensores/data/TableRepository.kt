package com.galeria.defensores.data

import com.galeria.defensores.models.Table
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object TableRepository {
    private const val PREFIX = "table_"
    
    // Similar Context strategy as CharacterRepository
    private lateinit var appContext: android.content.Context

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }
    
    private fun getContext(): android.content.Context? {
        return if (::appContext.isInitialized) appContext else null
    }

    suspend fun getTables(): List<Table> {
        val context = getContext() ?: return emptyList()
        val allFiles = LocalFileManager.listFiles(context, PREFIX)
        val tables = mutableListOf<Table>()
        
        for (file in allFiles) {
            val table = LocalFileManager.readJson(context, file.name, Table::class.java)
            if (table != null) {
                 tables.add(table)
            }
        }
        return tables
    }

    suspend fun getTable(id: String): Table? {
         val context = getContext() ?: return null
         return LocalFileManager.readJson(context, "$PREFIX$id.json", Table::class.java)
    }

    suspend fun addTable(table: Table): Boolean {
        // Identical to save/update
        return updateTable(table)
    }

    suspend fun updateTable(table: Table): Boolean {
        val context = getContext() ?: return false
        LocalFileManager.saveJson(context, "$PREFIX${table.id}.json", table)
        return true
    }

    suspend fun deleteTable(id: String): Boolean {
        val context = getContext() ?: return false
        
        // 1. Delete pending notifications (Mocked/Local)
        com.galeria.defensores.data.NotificationRepository.deleteNotificationsForTable(id)
        
        // 2. Unlink characters
        com.galeria.defensores.data.CharacterRepository.unlinkCharactersFromTable(id)
        
        // 3. Delete file
        return LocalFileManager.deleteFile(context, "$PREFIX$id.json")
    }

    suspend fun checkUserHasActiveTables(userId: String): Boolean {
        // "Active" tables offline = any table the user created that has other players? 
        // Since players are just strings/IDs, we can check.
        // But in offline single player mode, "players" might just be empty or contain non-existent IDs.
        // Let's iterate.
        
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
        // Limit history size to prevent massive JSONs
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
         // Just setting it in the object, next poll/refresh would see it 
         // (though polling isn't implemented for files, UI needs to refresh manually or observe something else)
         val updatedTable = table.copy(lastVisualRoll = visualRoll)
         updateTable(updatedTable)
    }

    fun getTableFlow(id: String): Flow<Table?> = callbackFlow {
        // File system doesn't support real-time listeners easily.
        // We can emit the current value once.
        // If we wanted updates, we'd need a custom observable or polling.
        // For now, emit once and close, or just emit once.
        // Flow allows emitting multiple times, but without a file watcher, we can't know when to emit next.
        // UI might rely on this flow staying open? 
        // Let's emit once.
        val context = getContext()
        val table = if (context != null) LocalFileManager.readJson(context, "$PREFIX$id.json", Table::class.java) else null
        trySend(table)
        
        // We could implement a pseudo-polling here if crucial:
        // while(isActive) { delay(2000); trySend(readJson...); }
        // But that's heavy.
        // Let's assume offline usage is mostly synchronous user actions updating UI.
        
        close() 
        awaitClose { }
    }
}
