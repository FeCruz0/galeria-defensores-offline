package com.galeria.defensores.data.database.entities

import androidx.room.*
import com.galeria.defensores.models.*

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String,
    val tableId: String,
    val imageUrl: String,
    val scale: Int,
    
    // Attributes
    val forca: Int,
    val habilidade: Int,
    val resistencia: Int,
    val armadura: Int,
    val poderFogo: Int,
    val damageTypeForca: String,
    val damageTypePdf: String,
    
    // Status
    val currentPv: Int,
    val currentPm: Int,
    
    // Experience
    val savedPoints: Int,
    val experience: Int,

    // Complex types stored as JSON via Converters
    val vantagens: List<AdvantageItem>,
    val desvantagens: List<AdvantageItem>,
    val pericias: List<AdvantageItem>,
    val especializacoes: List<AdvantageItem>,
    val magias: List<Spell>,
    val inventario: List<InventoryItem>,
    val uniqueAdvantage: UniqueAdvantage?,
    val anotacoes: String,
    val customRolls: List<CustomRoll>,
    val systemOverride: RuleSystem?,
    val attributeValues: Map<String, Int>,
    val resourceValues: Map<String, Int>
) {
    fun toCharacter(): Character {
        return Character(
            id = id,
            name = name,
            ownerId = ownerId,
            ownerName = ownerName,
            tableId = tableId,
            imageUrl = imageUrl,
            scale = scale,
            forca = forca,
            habilidade = habilidade,
            resistencia = resistencia,
            armadura = armadura,
            poderFogo = poderFogo,
            damageTypeForca = damageTypeForca,
            damageTypePdf = damageTypePdf,
            currentPv = currentPv,
            currentPm = currentPm,
            savedPoints = savedPoints,
            experience = experience,
            vantagens = vantagens.toMutableList(),
            desvantagens = desvantagens.toMutableList(),
            pericias = pericias.toMutableList(),
            especializacoes = especializacoes.toMutableList(),
            magias = magias.toMutableList(),
            inventario = inventario.toMutableList(),
            uniqueAdvantage = uniqueAdvantage,
            anotacoes = anotacoes,
            customRolls = customRolls.toMutableList(),
            systemOverride = systemOverride,
            attributeValues = attributeValues.toMutableMap(),
            resourceValues = resourceValues.toMutableMap()
        )
    }

    companion object {
        fun fromCharacter(char: Character): CharacterEntity {
            return CharacterEntity(
                id = char.id,
                name = char.name,
                ownerId = char.ownerId,
                ownerName = char.ownerName,
                tableId = char.tableId,
                imageUrl = char.imageUrl,
                scale = char.scale,
                forca = char.forca,
                habilidade = char.habilidade,
                resistencia = char.resistencia,
                armadura = char.armadura,
                poderFogo = char.poderFogo,
                damageTypeForca = char.damageTypeForca,
                damageTypePdf = char.damageTypePdf,
                currentPv = char.currentPv,
                currentPm = char.currentPm,
                savedPoints = char.savedPoints,
                experience = char.experience,
                vantagens = char.vantagens,
                desvantagens = char.desvantagens,
                pericias = char.pericias,
                especializacoes = char.especializacoes,
                magias = char.magias,
                inventario = char.inventario,
                uniqueAdvantage = char.uniqueAdvantage,
                anotacoes = char.anotacoes,
                customRolls = char.customRolls,
                systemOverride = char.systemOverride,
                attributeValues = char.attributeValues,
                resourceValues = char.resourceValues
            )
        }
    }
}
