package com.galeria.defensores.data

import com.galeria.defensores.models.Table
import com.galeria.defensores.data.database.daos.TableDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class TableRepository @Inject constructor(
    private val tableDao: TableDao,
    private val characterRepository: CharacterRepository
) {
    // Legacy init removed as Hilt handles injection
    fun init(context: android.content.Context) {}

    fun getTables(): Flow<List<Table>> {
        return tableDao.getAll().map { entities -> entities.map { it.toTable() } }
    }

    fun getTable(id: String): Flow<Table?> {
        return tableDao.getById(id).map { it?.toTable() }
    }

    suspend fun getTableOnce(id: String): Table? {
        return getTable(id).first()
    }

    suspend fun addTable(table: Table): Boolean {
        if (table.name.isNotBlank()) {
            val existing = getTables().first()
            if (existing.any { it.name.equals(table.name, ignoreCase = true) && it.id != table.id }) {
                throw IllegalArgumentException("Já existe uma mesa com o nome '${table.name}'.")
            }
        }
        return updateTable(table)
    }

    suspend fun updateTable(table: Table): Boolean {
        if (table.name.isNotBlank()) {
            val existing = getTables().first()
            if (existing.any { it.name.equals(table.name, ignoreCase = true) && it.id != table.id }) {
                throw IllegalArgumentException("Já existe uma mesa com o nome '${table.name}'.")
            }
        }
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
        val tables = getTables().first()
        return tables.any { it.masterId == userId && it.players.isNotEmpty() }
    }
 
    suspend fun addPlayerToTable(tableId: String, playerId: String) {
        val table = getTableOnce(tableId) ?: return
        if (!table.players.contains(playerId)) {
            val updatedTable = table.copy(players = (table.players + playerId).toMutableList())
            updateTable(updatedTable)
        }
    }
 
    suspend fun addRollToHistory(tableId: String, roll: com.galeria.defensores.models.RollResult) {
        val table = getTableOnce(tableId) ?: return
        val updatedHistory = table.rollHistory.toMutableList()
        updatedHistory.add(roll)
        if (updatedHistory.size > 50) {
            updatedHistory.removeAt(0)
        }
        val updatedTable = table.copy(rollHistory = updatedHistory)
        updateTable(updatedTable)
    }
 
    suspend fun clearRollHistory(tableId: String): Boolean {
         val table = getTableOnce(tableId) ?: return false
         val updatedTable = table.copy(rollHistory = mutableListOf())
         updateTable(updatedTable)
         return true
    }
 
    suspend fun broadcastVisualRoll(tableId: String, visualRoll: com.galeria.defensores.models.VisualRoll) {
         val table = getTableOnce(tableId) ?: return
         val updatedTable = table.copy(lastVisualRoll = visualRoll)
         updateTable(updatedTable)
    }

    fun getTableFlow(id: String): Flow<Table?> = getTable(id)
}
