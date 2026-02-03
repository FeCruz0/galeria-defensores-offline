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
    fun getGoogleSignInClient(activity: Activity): com.google.android.gms.auth.api.signin.GoogleSignInClient {
      // Return a dummy or throw exception if used? 
      // Ideally returning real client requires google-services.json which we might still have, 
      // but let's just create a basic one or error out if not used. 
      // Given we removed login UI, this shouldn't be called.
      // But for compilation safety:
      val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
      return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
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

