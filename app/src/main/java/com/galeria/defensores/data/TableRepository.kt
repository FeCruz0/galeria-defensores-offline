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
            tablesCollection.document(id).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
}
