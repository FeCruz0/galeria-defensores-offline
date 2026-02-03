package com.galeria.defensores.models

import java.util.UUID

data class Combatant(
    val id: String = UUID.randomUUID().toString(),
    val characterId: String = "",
    val name: String = "",
    var initiative: Int = 0,
    var currentPv: Int = 1,
    var maxPv: Int = 1,
    var currentPm: Int = 1,
    var maxPm: Int = 1,
    var avatarUrl: String = "",
    var isNpc: Boolean = false,
    var isDefeated: Boolean = false,
    
    // Attributes Snapshot
    var forca: Int = 0,
    var habilidade: Int = 0,
    var resistencia: Int = 0,
    var armadura: Int = 0,
    var poderFogo: Int = 0,
    var scale: Int = 0, // 0=Ningen...
    
    // For storing last roll in combat for display
    var lastRollTotal: Int = 0,
    var lastRollType: String = ""
)
