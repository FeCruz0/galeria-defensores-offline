package com.galeria.defensores.models

import com.google.firebase.firestore.PropertyName

data class VisualRoll(
    @get:PropertyName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @get:PropertyName("senderId") val senderId: String = "",
    @get:PropertyName("senderName") val senderName: String = "",
    @get:PropertyName("diceCount") val diceCount: Int = 1,
    @get:PropertyName("diceValues") val diceValues: List<Int> = emptyList(),
    @get:PropertyName("diceProperties") val diceProperties: List<DieProperty> = emptyList(),
    @get:PropertyName("canCrit") val canCrit: Boolean = false,
    @get:PropertyName("isNegative") val isNegative: Boolean = false,
    @get:PropertyName("critRangeStart") val critRangeStart: Int = 6,
    @get:PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
