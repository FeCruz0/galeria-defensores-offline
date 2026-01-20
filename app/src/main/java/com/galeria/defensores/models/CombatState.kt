package com.galeria.defensores.models

import java.util.UUID

data class CombatState(
    var isActive: Boolean = false,
    var round: Int = 1,
    var currentTurnIndex: Int = 0,
    var combatants: MutableList<Combatant> = mutableListOf(),
    
    // Pending Action (e.g., waiting for defense)
    var pendingAction: CombatAction? = null,
    
    // Combat Log
    var log: MutableList<String> = mutableListOf()
)

data class CombatAction(
    val type: String = "", // "ATTACK"
    val attackerId: String = "",
    val targetId: String = "",
    val attackRoll: Int = 0,
    val attackDetails: String = "",
    val damageType: String = "",
    val scaleFormatted: String = "" // e.g. "Sugoi (+5)"
)
