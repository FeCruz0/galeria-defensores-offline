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
            val docRef = usersCollection.document(user.id)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val newUser = user.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                docRef.set(newUser).await()
            }
            // If user exists, do nothing. We do not want to overwrite profile data with Auth data on login.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun updateUser(user: User) {
        try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            usersCollection.document(user.id).set(updatedUser).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateLastLogin(userId: String) {
        try {
            usersCollection.document(userId).update("lastLoginAt", System.currentTimeMillis()).await()
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

    suspend fun isUsernameTaken(name: String): Boolean {
        return try {
            val snapshot = usersCollection.whereEqualTo("name", name).get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false // Assume not taken on error to avoid blocking, or handle differently
        }
    }
    suspend fun deleteUser(userId: String) {
        try {
            usersCollection.document(userId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
