package com.galeria.defensores.data

import com.galeria.defensores.models.Character
import kotlinx.coroutines.tasks.await

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
            val snapshot = charactersCollection.whereEqualTo("ownerId", userId).get().await()
            snapshot.toObjects(Character::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
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

    suspend fun deleteCharacter(id: String) {
        try {
            charactersCollection.document(id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
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
