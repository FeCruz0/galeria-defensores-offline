package com.galeria.defensores.models

import java.util.UUID

data class Table(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    val masterId: String = "", // mestre_id
    val players: MutableList<String> = mutableListOf(), // lista_jogadores_id
    val isPrivate: Boolean = false, // privativa
    val password: String? = null,
    val rulesMod: Map<String, Any> = emptyMap(), // regras_mod
    val rollHistory: MutableList<RollResult> = mutableListOf() // historico_rolagens
)
