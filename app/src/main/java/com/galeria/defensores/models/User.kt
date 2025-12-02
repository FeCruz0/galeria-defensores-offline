package com.galeria.defensores.models

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "", // nome_exibicao
    val email: String = "",
    val phoneNumber: String = "", // Keeping for backward compatibility if needed, or remove if fully migrating
    val password: String = "" // senha_hash (managed by Auth, but kept for object structure if needed)
)
