package com.galeria.defensores.models

data class VisualRoll(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val diceCount: Int = 1,
    val diceValues: List<Int> = emptyList(),
    val diceProperties: List<DieProperty> = emptyList(),
    val canCrit: Boolean = false,
    val isNegative: Boolean = false,
    val critRangeStart: Int = 6,
    val timestamp: Long = System.currentTimeMillis()
)
