package com.galeria.defensores.data

import com.galeria.defensores.models.User


object UserRepository {
    // In Offline Mode, we only really have the "Device Owner" user.
    // 'findUserByPhone' for invites doesn't make sense unless we scan contacts or something, 
    // but for now we'll just mock it.

    suspend fun findUserByPhone(phone: String): User? {
        // Mock: If phone matches current user, return it.
        val current = SessionManager.currentUser
        return if (current?.phoneNumber == phone) current else null
    }

    suspend fun registerUser(user: User) {
        // Save to Session or local file
        // For simplicity, just update SessionManager which is strictly in-memory or SharedPreferences in the real implementation
        // But here SessionManager is mocked.
        // We can save to a file `user_profile.json` using LocalFileManager if we want persistence.
        com.galeria.defensores.data.LocalFileManager.saveJson(SessionManager.context, "user_profile.json", user)
    }
    
    suspend fun updateUser(user: User) {
        // Same as register
        com.galeria.defensores.data.LocalFileManager.saveJson(SessionManager.context, "user_profile.json", user)
    }

    suspend fun updateLastLogin(userId: String) {
        // No-op or update local timestamp
    }
    
    suspend fun getUser(id: String): User? {
         // Return current user if ID matches, or read from "contacts" if we had them.
         val current = SessionManager.currentUser
         return if (current?.id == id) current else null
    }

    suspend fun isUsernameTaken(name: String): Boolean {
        return false // Offline mode, no collision check needed with others
    }
    
    suspend fun deleteUser(userId: String) {
        com.galeria.defensores.data.LocalFileManager.deleteFile(SessionManager.context, "user_profile.json")
    }
}
