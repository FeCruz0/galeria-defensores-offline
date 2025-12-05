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
                phoneNumber = firebaseUser.email ?: "" // Using email as identifier now
            )
        } else {
            null
        }
    }

    fun login(email: String, password: String, onSuccess: (User) -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    getCurrentUser()?.let { onSuccess(it) }
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
