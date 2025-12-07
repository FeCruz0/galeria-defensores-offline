package com.galeria.defensores.data

import android.app.Activity
import com.galeria.defensores.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.concurrent.TimeUnit

object FirebaseAuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Usuário",
                email = firebaseUser.email ?: "",
                phoneNumber = firebaseUser.phoneNumber ?: "" 
            )
        } else {
            null
        }
    }

    fun login(email: String, password: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    getCurrentUser()?.let { 
                        // Update last login timestamp
                        // We use GlobalScope or similar mechanism if we can't be suspended, 
                        // but here we are in a callback. Ideally we launch a coroutine.
                        // For simplicity in this callback context (Java style), we might miss it if app closes.
                        // Better to rely on the UI/ViewModel to trigger it, OR fire and forget.
                        // However, we can't easily access CoroutineScope here without passing it.
                        // Let's defer to SessionManager.refreshUser which is called after login to handle it?
                        // User specifically asked for it. 
                        // Let's assume SessionManager.refreshUser handles it or we do it here.
                        // Since `onSuccess` is called, the UI will likely trigger a refresh.
                        // Let's add it there or here. 
                        // Adding simple fire-and-forget logic if possible? No, we don't have scope.
                        // Let's trust the Architecture: onSuccess -> UI -> SessionManager.refreshUser.
                        // We should add it to SessionManager.refreshUser then, as that's suspendable.
                        onSuccess(it) 
                    }
                } else {
                    onError(task.exception?.message ?: "Erro ao fazer login")
                }
            }
    }

    fun register(name: String, email: String, password: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                getCurrentUser()?.let { onSuccess(it) }
                            } else {
                                onSuccess(getCurrentUser()!!)
                            }
                        }
                } else {
                    onError(task.exception?.message ?: "Erro ao cadastrar")
                }
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Erro ao enviar e-mail de recuperação")
                }
            }
    }

    fun updateProfile(name: String, onSuccess: () -> Unit) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        auth.currentUser?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { 
                onSuccess() 
            }
    }

    fun logout() {
        auth.signOut()
    }

    // Google Sign-In
    fun getGoogleSignInClient(activity: Activity): com.google.android.gms.auth.api.signin.GoogleSignInClient {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(com.galeria.defensores.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso)
    }

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    getCurrentUser()?.let { onSuccess(it) }
                } else {
                    onError(task.exception?.message ?: "Erro no login com Google")
                }
            }
    }


}
