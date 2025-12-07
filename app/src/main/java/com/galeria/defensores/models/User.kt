package com.galeria.defensores.models

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "", // nome_exibicao
    val email: String = "",
    val phoneNumber: String = "", // Keeping for backward compatibility if needed, or remove if fully migrating
    val password: String = "", // senha_hash (managed by Auth, but kept for object structure if needed)
    
    // Profile Info
    val about: String = "",
    val cep: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    
    // System Fields
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0,
    val emailVerifiedAt: Long? = null,
    val lastUsernameChangeAt: Long = 0
)
