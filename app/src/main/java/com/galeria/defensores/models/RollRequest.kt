package com.galeria.defensores.models

enum class RollRequestType {
    ATTACK_F,
    ATTACK_PDF,
    DEFENSE,
    INITIATIVE,
    CUSTOM,
    ATTRIBUTE
}

data class RollRequest(
    val type: RollRequestType,
    val diceCount: Int,
    val bonus: Int,
    val attributeValue: Int,
    val skillValue: Int,
    val attributeName: String,
    // For Custom Rolls
    val customRoll: CustomRoll? = null,
    val diceOverride: List<Int>? = null,
    val diceProperties: List<DieProperty>? = null,
    val isNegative: Boolean = false,
    val canCrit: Boolean = false,
    val critRangeStart: Int = 6
)
