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
        android.util.Log.d("SessionDebug", "Refreshing user. FirebaseUser: ${firebaseUser?.id}")
        if (firebaseUser != null) {
            _currentUser = UserRepository.getUser(firebaseUser.id)
            android.util.Log.d("SessionDebug", "Fetched user from Firestore: ${_currentUser?.name}, ID: ${_currentUser?.id}")
            
            // Auto-heal: If user exists but has no email in Firestore (legacy/error), sync with Auth email
            if (_currentUser != null && _currentUser!!.email.isBlank() && firebaseUser.email.isNotBlank()) {
                 android.util.Log.d("SessionDebug", "Auto-healing user email. Syncing from Auth: ${firebaseUser.email}")
                 val healedUser = _currentUser!!.copy(email = firebaseUser.email)
                 UserRepository.updateUser(healedUser)
                 _currentUser = healedUser
            }

            // If user not found in Firestore (e.g. new auth), create basic profile
            if (_currentUser == null) {
                val newUser = User(
                    id = firebaseUser.id,
                    name = firebaseUser.name.ifBlank { "Usuário" },
                    email = firebaseUser.email,
                    phoneNumber = firebaseUser.phoneNumber
                )
                android.util.Log.d("SessionDebug", "User not found in Firestore. Registering new user: ${newUser.name}")
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
