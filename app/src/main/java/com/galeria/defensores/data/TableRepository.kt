package com.galeria.defensores.data

import com.galeria.defensores.models.Table
import kotlinx.coroutines.tasks.await

object TableRepository {
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val tablesCollection = db.collection("tables")

    suspend fun getTables(): List<Table> {
        return try {
            val snapshot = tablesCollection.get().await()
            snapshot.toObjects(Table::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTable(id: String): Table? {
        return try {
            val doc = tablesCollection.document(id).get().await()
            doc.toObject(Table::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addTable(table: Table): Boolean {
        return try {
            tablesCollection.document(table.id).set(table).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateTable(table: Table): Boolean {
        return try {
            tablesCollection.document(table.id).set(table).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    suspend fun deleteTable(id: String): Boolean {
        return try {
            // 1. Delete pending notifications for this table
            com.galeria.defensores.data.NotificationRepository.deleteNotificationsForTable(id)
            
            // 2. Unlink characters
            com.galeria.defensores.data.CharacterRepository.unlinkCharactersFromTable(id)
            
            // 3. Delete the table
            tablesCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun checkUserHasActiveTables(userId: String): Boolean {
        return try {
            val snapshot = tablesCollection
                .whereEqualTo("masterId", userId)
                .get()
                .await()
            // Check if any table has players (active)
            // Or simpler: does user own ANY table? The requirement mentions "mesas com outros jogadores".
            // Let's implement strict check: Table owned by user AND players list is NOT empty.
            // But 'players' includes the master usually? No, masterId is separate field. players array is list of OTHER users.
            
            // Assuming 'players' field. Let's check schema. Table model has `val players: MutableList<String> = mutableListOf()`.
            // Does it include master? Usually implementation dependent.
            // If I look at create logic:
            // Table(masterId = user.id, ...)
            // It doesn't auto-add master to players list in constructor usually.
            // So:
            
            snapshot.documents.any { doc ->
                val table = doc.toObject(Table::class.java)
                table != null && table.players.isNotEmpty()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true // Fail safe: block deletion if we can't verify
        }
    }

    suspend fun addPlayerToTable(tableId: String, playerId: String) {
        try {
            val tableRef = tablesCollection.document(tableId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(tableRef)
                val table = snapshot.toObject(Table::class.java)
                if (table != null && !table.players.contains(playerId)) {
                    table.players.add(playerId)
                    transaction.set(tableRef, table)
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addRollToHistory(tableId: String, roll: com.galeria.defensores.models.RollResult) {
        try {
            android.util.Log.d("TableRepoDebug", "Adding roll to history: tableId=$tableId, roll=${roll.total}")
            tablesCollection.document(tableId)
                .update("rollHistory", com.google.firebase.firestore.FieldValue.arrayUnion(roll))
                .await()
            android.util.Log.d("TableRepoDebug", "Roll added successfully")
        } catch (e: Exception) {
            android.util.Log.e("TableRepoDebug", "Error adding roll", e)
            e.printStackTrace()
        }
    }

    suspend fun clearRollHistory(tableId: String): Boolean {
        return try {
            val emptyList = emptyList<com.galeria.defensores.models.RollResult>()
            tablesCollection.document(tableId)
                .update("rollHistory", emptyList)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
