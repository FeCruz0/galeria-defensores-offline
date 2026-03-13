package com.galeria.defensores.data

import com.galeria.defensores.models.Table
import com.galeria.defensores.data.database.daos.TableDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class TableRepository @Inject constructor(
    private val tableDao: TableDao,
    private val characterRepository: CharacterRepository
) {
    // Legacy init removed as Hilt handles injection
    fun init(context: android.content.Context) {}

    suspend fun getTables(): List<Table> {
        return tableDao.getAll().map { it.toTable() }
    }

    suspend fun getTable(id: String): Table? {
        return tableDao.getById(id)?.toTable()
    }

    suspend fun addTable(table: Table): Boolean {
        return updateTable(table)
    }

    suspend fun updateTable(table: Table): Boolean {
        tableDao.insert(com.galeria.defensores.data.database.entities.TableEntity.fromTable(table))
        return true
    }

    suspend fun deleteTable(id: String): Boolean {
        // 1. Delete pending notifications (Mocked/Local)
        com.galeria.defensores.data.NotificationRepository.deleteNotificationsForTable(id)
        
        // 2. Unlink characters
        characterRepository.unlinkCharactersFromTable(id)
        
        // 3. Delete from DB
        tableDao.deleteById(id)
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
