package com.galeria.defensores.data

import android.content.Context
import android.content.SharedPreferences
import com.galeria.defensores.models.User

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHONE = "user_phone"

    fun init(context: Context) {
        // Firebase Auth handles persistence automatically
    }

    private var _currentUser: User? = null
    
    val currentUser: User?
        get() = _currentUser

    suspend fun refreshUser() {
        val firebaseUser = FirebaseAuthManager.getCurrentUser()
        if (firebaseUser != null) {
            _currentUser = UserRepository.getUser(firebaseUser.id)
            // If user not found in Firestore (e.g. new auth), create basic profile
            if (_currentUser == null) {
                val newUser = User(
                    id = firebaseUser.id,
                    name = firebaseUser.name.ifBlank { "Usuário" },
                    email = firebaseUser.email,
                    phoneNumber = firebaseUser.phoneNumber
                )
                UserRepository.registerUser(newUser)
                _currentUser = newUser
            }
        } else {
            _currentUser = null
        }
    }

    fun logout() {
        FirebaseAuthManager.logout()
        _currentUser = null
    }
    
    fun isLoggedIn(): Boolean = FirebaseAuthManager.getCurrentUser() != null
}
