package com.galeria.defensores.models

import java.util.UUID

data class RuleSystem(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "3DeT Alpha",
    var description: String = "Sistema base do 3DeT Alpha",
    val isBaseSystem: Boolean = false, // If true, cannot be deleted
    var attributes: List<AttributeConfig> = defaultAttributes(),
    var derivedStats: List<DerivedStatConfig> = defaultDerivedStats(),
    var diceConfig: DiceConfig = DiceConfig()
)

data class AttributeConfig(
    val key: String, // Internal key: forca, habilidade, resistencia, armadura, poderFogo
    var name: String, // Display Name
    var abbreviation: String,
    var color: String = "#000000"
)

data class DerivedStatConfig(
    val key: String, // pv, pm
    var name: String,
    var formulaMultiplier: Int = 5 // e.g., Rx5
)

data class DiceConfig(
    var count: Int = 1,
    var faces: Int = 6
)

fun defaultAttributes(): List<AttributeConfig> {
    return listOf(
        AttributeConfig("forca", "Força", "F", "#D32F2F"),
        AttributeConfig("habilidade", "Habilidade", "H", "#1976D2"),
        AttributeConfig("resistencia", "Resistência", "R", "#388E3C"),
        AttributeConfig("armadura", "Armadura", "A", "#7B1FA2"),
        AttributeConfig("poderFogo", "Poder de Fogo", "PdF", "#FBC02D")
    )
}

fun defaultDerivedStats(): List<DerivedStatConfig> {
    return listOf(
        DerivedStatConfig("pv", "Pontos de Vida", 5),
        DerivedStatConfig("pm", "Pontos de Magia", 5)
    )
}
