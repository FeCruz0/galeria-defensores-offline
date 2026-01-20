package com.galeria.defensores.models

import java.util.UUID

import com.google.firebase.firestore.PropertyName

data class Table(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    @get:PropertyName("masterId") val masterId: String = "", // mestre_id
    @get:PropertyName("players") val players: MutableList<String> = mutableListOf(), // lista_jogadores_id
    @get:PropertyName("isPrivate") @set:PropertyName("isPrivate") var isPrivate: Boolean = false, // privativa
    val password: String? = null,
    val rulesMod: Map<String, Any> = emptyMap(), // regras_mod
    val rollHistory: MutableList<RollResult> = mutableListOf(), // historico_rolagens
    var customDamageTypes: MutableList<String> = mutableListOf(), // tipos_dano_customizados
    @get:PropertyName("customUniqueAdvantages") var customUniqueAdvantages: MutableList<UniqueAdvantage> = mutableListOf(),
    @get:PropertyName("lastVisualRoll") var lastVisualRoll: VisualRoll? = null,
    @get:PropertyName("combatState") var combatState: CombatState? = null
)
