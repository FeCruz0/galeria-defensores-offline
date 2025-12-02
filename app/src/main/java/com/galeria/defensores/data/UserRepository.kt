package com.galeria.defensores.data

import com.galeria.defensores.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun findUserByPhone(phone: String): User? {
        return try {
            val snapshot = usersCollection.whereEqualTo("phoneNumber", phone).get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun registerUser(user: User) {
        try {
            // Check if user exists first to avoid duplicates (optional, but good practice)
            val existing = findUserByPhone(user.phoneNumber)
            if (existing == null) {
                usersCollection.document(user.id).set(user).await()
            } else {
                // Update existing user if needed, or just skip
                usersCollection.document(existing.id).set(user).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getUser(id: String): User? {
        return try {
            val doc = usersCollection.document(id).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
