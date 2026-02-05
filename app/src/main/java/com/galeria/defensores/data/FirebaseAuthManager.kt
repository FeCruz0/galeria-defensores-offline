package com.galeria.defensores.data

import android.app.Activity
import com.galeria.defensores.models.User

object FirebaseAuthManager {
    // Mock for Offline Mode

    fun getCurrentUser(): User? {
        return SessionManager.currentUser
    }

    fun login(email: String, password: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
         onSuccess(SessionManager.currentUser!!)
    }

    fun register(name: String, email: String, password: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        onSuccess(SessionManager.currentUser!!)
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onSuccess()
    }

    fun updateProfile(name: String, onSuccess: () -> Unit) {
        onSuccess()
    }

    fun logout() {
        // No-op
    }

    // Google Sign-In - Disabled/Returning null/dummy
    // Google Sign-In - Disabled/Returning null/dummy
    fun getGoogleSignInClient(activity: Activity): Any? {
      return null
    }

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        onSuccess(SessionManager.currentUser!!)
    }


    suspend fun reauthenticate(password: String) {
        // No-op success
    }

    suspend fun deleteAccount(password: String) {
        // No-op
    }
}

