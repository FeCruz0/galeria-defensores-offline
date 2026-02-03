package com.galeria.defensores.models

import java.util.UUID
import com.galeria.defensores.models.InventoryItem

data class Character(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Defensor", // nome_personagem
    var ownerId: String = "", // proprietario_id
    var ownerName: String = "", // nome_do_proprietario
    var tableId: String = "", // id_da_mesa
    var imageUrl: String = "", // avatar_url
    var scale: Int = 0, // escala: 0=Ningen, 1=Sugoi, 2=Kiodai, 3=Kami
    
    // Attributes
    var forca: Int = 0,
    var habilidade: Int = 0,
    var resistencia: Int = 0,
    var armadura: Int = 0,
    var poderFogo: Int = 0, // poder_de_fogo
    var damageTypeForca: String = "Corte", // tipo_dano_forca
    var damageTypePdf: String = "Corte", // tipo_dano_pdf
    
    // Status
    var currentPv: Int = 1, // pv_atual
    var currentPm: Int = 1, // pm_atual
    
    // Experience & Points
    var savedPoints: Int = 0, // pontos_guardados
    var experience: Int = 0, // experiencia

    // Lists
    var vantagens: MutableList<AdvantageItem> = mutableListOf(),
    var desvantagens: MutableList<AdvantageItem> = mutableListOf(),
    var pericias: MutableList<AdvantageItem> = mutableListOf(),
    var especializacoes: MutableList<AdvantageItem> = mutableListOf(),
    var magias: MutableList<Spell> = mutableListOf(),
    var inventario: MutableList<InventoryItem> = mutableListOf(),
    
    // Unique Advantage (Race)
    var uniqueAdvantage: UniqueAdvantage? = null,
    val anotacoesRich: String = "", // Para futuro suporte a rich text se precisar persistir html/markdown
    
    var anotacoes: String = "",
    var isHidden: Boolean = false,
    
    // Custom Rolls
    var customRolls: MutableList<CustomRoll> = mutableListOf(),

    // Persistent Buffs
    var buffs: MutableList<Buff> = mutableListOf()
) {
    fun getMaxPv(): Int = (resistencia * 5).coerceAtLeast(1)
    fun getMaxPm(): Int = (resistencia * 5).coerceAtLeast(1)

    fun calculateScore(): Int {
        val attrSum = forca + habilidade + resistencia + armadura + poderFogo
        val advantagesSum = vantagens.sumOf { it.cost.toIntOrNull() ?: 0 }
        val skillsSum = pericias.sumOf { it.cost.toIntOrNull() ?: 0 }
        val specsSum = (especializacoes.size / 3) // 1 ponto a cada 3 especializações
        val uniqueAdvantageCost = uniqueAdvantage?.cost ?: 0
        
        return attrSum + advantagesSum + skillsSum + specsSum + uniqueAdvantageCost + savedPoints
    }
    
}

data class AdvantageItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val cost: String = ""
)

data class SimpleItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = ""
)

data class Spell(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var school: String = "",
    var requirements: String = "",
    var cost: String = "",
    var range: String = "",
    var duration: String = "",
    var description: String = ""
)

data class CustomRoll(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Nova Rolagem",
    var description: String = "",
    var components: MutableList<RollComponent> = mutableListOf(RollComponent()),
    var globalModifier: Int = 0,
    var primaryAttribute: String = "none",
    var secondaryAttribute: String = "none",
    var accumulateCrit: Boolean = false, // false: Single Crit (x2), true: +1 Mult per Crit
    var type: String = "OTHER", // ATTACK, DEFENSE, MAGIC, INITIATIVE, TEST, OTHER
    var pmCost: Int = 0
)

data class RollComponent(
    val id: String = UUID.randomUUID().toString(),
    var count: Int = 1,
    var faces: Int = 6,
    var bonus: Int = 0,
    var isNegative: Boolean = false,
    var canCrit: Boolean = false,
    var critRangeStart: Int? = null, // null means ONLY max value
    var critMultiplier: Int = 2
)

data class Buff(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var value: Int = 0, // e.g., +2 or -2
    var targetType: String = "ALL" // ATTACK, DEFENSE, HAB, FOR, ETC.
)