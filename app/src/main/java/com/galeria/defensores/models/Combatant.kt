package com.galeria.defensores.models

import java.util.UUID

data class Combatant(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String = "",
    val name: String = "",
    var initiative: Int = 0,
    var currentPv: Int = 1,
    var maxPv: Int = 1,
    var avatarUrl: String = "",
    var isNpc: Boolean = false,
    var isDefeated: Boolean = false,
    
    // For storing last roll in combat for display
    var lastRollTotal: Int = 0,
    var lastRollType: String = ""
)
