package com.galeria.defensores.models

data class RollResult(
    val total: Int = 0,
    val die: Int = 0,
    val attributeUsed: String = "",
    val attributeValue: Int = 0,
    val skillValue: Int = 0,
    val bonus: Int = 0,
    val isCritical: Boolean = false,
    val timestamp: Long = 0L,
    val name: String = "",
    val isHidden: Boolean = false,
    val characterId: String = ""
)

enum class RollType(val displayName: String) {
    ATTACK_F("Força"),
    ATTACK_PDF("Poder de Fogo"),
    DEFENSE("Armadura"),
    SPECIAL_F("Especial (F)"),
    SPECIAL_PDF("Especial (PdF)")
}
