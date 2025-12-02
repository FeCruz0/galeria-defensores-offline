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
}
