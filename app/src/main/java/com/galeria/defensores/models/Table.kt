package com.galeria.defensores.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Table(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    val masterId: String = "", 
    val players: MutableList<String> = mutableListOf(), 
    var isPrivate: Boolean = false, 
    val password: String? = null,
    val ruleSystemId: String = "3det_alpha_base",
    val rulesMod: Map<String, String> = emptyMap(), 
    val rollHistory: MutableList<RollResult> = mutableListOf(), 
    var customDamageTypes: MutableList<String> = mutableListOf(), 
    var customUniqueAdvantages: MutableList<UniqueAdvantage> = mutableListOf(),
    var lastVisualRoll: VisualRoll? = null,
    var combatState: CombatState? = null
)
