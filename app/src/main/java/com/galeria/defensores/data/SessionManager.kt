package com.galeria.defensores.data

import android.content.Context
import com.galeria.defensores.models.User

object SessionManager {
    
    // Default Offline User
    private val offlineUser = User(
        id = "offline_user_id",
        name = "Jogador Offline",
        email = "offline@local.com",
        phoneNumber = ""
    )

    private var _currentUser: User? = offlineUser
    
    val currentUser: User?
        get() = _currentUser

    lateinit var context: Context
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
        _currentUser = offlineUser
    }

    suspend fun refreshUser() {
        // Always offline user
        _currentUser = offlineUser
    }

    fun logout() {
        // Cannot logout in offline mode
    }
    
    fun isLoggedIn(): Boolean = true
}

