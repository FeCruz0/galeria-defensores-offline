package com.galeria.defensores.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class RuleSystem(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "3DeT Alpha",
    var description: String = "Sistema base do 3DeT Alpha",
    val isBaseSystem: Boolean = false, // If true, cannot be deleted
    var attributes: MutableList<AttributeDefinition> = defaultAttributes().toMutableList(),
    var resources: MutableList<ResourceDefinition> = defaultResources().toMutableList(),
    var advantages: MutableList<ItemDefinition> = mutableListOf(),
    var disadvantages: MutableList<ItemDefinition> = mutableListOf(),
    var skills: MutableList<ItemDefinition> = mutableListOf(),
    var damageTypes: MutableList<String> = mutableListOf(),
    var diceConfig: DiceConfig = DiceConfig()
)

@Serializable
data class ItemDefinition(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String,
    var cost: String = "1", // e.g. "1pt", "-1pt", "1-2pt"
    var type: String = "General", // Optional tag
    val isModular: Boolean = false,
    val modifiers: List<ModifierOption> = emptyList(),
    val selectedModifiers: List<String> = emptyList(),
    val baseCostPt: Int = 0
)

@Serializable
data class AttributeDefinition(
    val id: String = UUID.randomUUID().toString(),
    var key: String, // Internal key: forca, habilidade...
    var name: String, // Display Name
    var abbreviation: String,
    var color: String = "#000000",
    var displayOrder: Int = 0
)

@Serializable
data class ResourceDefinition(
    val id: String = UUID.randomUUID().toString(),
    var key: String, // pv, pm
    var name: String,
    var color: String = "#FF0000",
    var formula: String = "R * 5" // Formula string
)

@Serializable
data class DiceConfig(
    var count: Int = 1,
    var faces: Int = 6
)

fun defaultAttributes(): List<AttributeDefinition> {
    return listOf(
        AttributeDefinition(key = "forca", name = "Força", abbreviation = "F", color = "#EF4444", displayOrder = 0),
        AttributeDefinition(key = "habilidade", name = "Habilidade", abbreviation = "H", color = "#3B82F6", displayOrder = 1),
        AttributeDefinition(key = "resistencia", name = "Resistência", abbreviation = "R", color = "#10B981", displayOrder = 2),
        AttributeDefinition(key = "armadura", name = "Armadura", abbreviation = "A", color = "#6B7280", displayOrder = 3),
        AttributeDefinition(key = "poderFogo", name = "Poder de Fogo", abbreviation = "PdF", color = "#8B5CF6", displayOrder = 4)
    )
}

fun defaultResources(): List<ResourceDefinition> {
    return listOf(
        ResourceDefinition(key = "pv", name = "Pontos de Vida", color = "#EF4444", formula = "R * 5"),
        ResourceDefinition(key = "pm", name = "Pontos de Magia", color = "#3B82F6", formula = "R * 5")
    )
}

fun RuleSystem.validateUniqueNameAndKey(id: String, key: String, name: String) {
    val cleanKey = key.trim().lowercase()
    val cleanName = name.trim().lowercase()

    // Check attributes
    attributes.forEach { attr ->
        if (attr.id != id) {
            if (attr.key.trim().lowercase() == cleanKey) {
                throw IllegalArgumentException("A chave '$key' já está em uso por outro atributo.")
            }
            if (attr.name.trim().lowercase() == cleanName) {
                throw IllegalArgumentException("O nome '$name' já está em uso por outro atributo.")
            }
        }
    }

    // Check resources
    resources.forEach { res ->
        if (res.id != id) {
            if (res.key.trim().lowercase() == cleanKey) {
                throw IllegalArgumentException("A chave '$key' já está em uso por outro recurso.")
            }
            if (res.name.trim().lowercase() == cleanName) {
                throw IllegalArgumentException("O nome '$name' já está em uso por outro recurso.")
            }
        }
    }
}

