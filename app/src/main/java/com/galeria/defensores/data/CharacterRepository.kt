package com.galeria.defensores.data

import com.galeria.defensores.models.Character
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.Source

object CharacterRepository {
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val charactersCollection = db.collection("characters")

    suspend fun getCharacters(tableId: String? = null): List<Character> {
        return try {
            val query = if (tableId != null) {
                charactersCollection.whereEqualTo("tableId", tableId)
            } else {
                charactersCollection
            }
            val snapshot = query.get().await()
            snapshot.toObjects(Character::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCharactersForUser(userId: String): List<Character> {
        return try {
            // FORCE SERVER FETCH to avoid stale cache issues
            val snapshot = charactersCollection.whereEqualTo("ownerId", userId).get(Source.SERVER).await()
            snapshot.toObjects(Character::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to cache if server fails? Or just return empty? 
            // Better empty to indicate error or maybe fallback
            try {
                 val snapshot = charactersCollection.whereEqualTo("ownerId", userId).get().await()
                 snapshot.toObjects(Character::class.java)
            } catch (ex: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getCharacter(id: String): Character? {
        return try {
            val doc = charactersCollection.document(id).get().await()
            doc.toObject(Character::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveCharacter(character: Character) {
        try {
            charactersCollection.document(character.id).set(character).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCharacter(id: String): Boolean {
        android.util.Log.d("RepoDebug", "Attempting to delete character ID: $id")
        return try {
            charactersCollection.document(id).delete().await()
            android.util.Log.d("RepoDebug", "Delete successful for ID: $id")
            true
        } catch (e: Exception) {
            android.util.Log.e("RepoDebug", "Delete FAILED for ID: $id", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun unlinkCharactersFromTable(tableId: String) {
        try {
            val snapshot = charactersCollection.whereEqualTo("tableId", tableId).get().await()
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.update(document.reference, "tableId", "")
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
