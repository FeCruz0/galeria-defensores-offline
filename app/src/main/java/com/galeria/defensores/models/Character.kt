package com.galeria.defensores.models

import java.util.UUID

data class Character(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Defensor", // nome_personagem
    var tableId: String = "", // mesa_id
    var ownerId: String = "", // proprietario_id
    var ownerName: String = "", // nome_do_proprietario
    
    // Attributes
    var forca: Int = 0,
    var habilidade: Int = 0,
    var resistencia: Int = 0,
    var armadura: Int = 0,
    var poderFogo: Int = 0, // poder_de_fogo
    
    // Status
    var currentPv: Int = 1, // pv_atual
    var currentPm: Int = 1, // pm_atual
    
    // Lists
    var vantagens: MutableList<AdvantageItem> = mutableListOf(),
    var desvantagens: MutableList<AdvantageItem> = mutableListOf(),
    var pericias: MutableList<AdvantageItem> = mutableListOf(),
    var especializacoes: MutableList<AdvantageItem> = mutableListOf(),
    var magias: MutableList<SimpleItem> = mutableListOf(),
    var inventario: MutableList<InventoryItem> = mutableListOf(),
    val anotacoesRich: String = "", // Para futuro suporte a rich text se precisar persistir html/markdown
    
    var anotacoes: String = "",
    var isHidden: Boolean = false
) {
    fun getMaxPv(): Int = (resistencia * 5).coerceAtLeast(1)
    fun getMaxPm(): Int = (resistencia * 5).coerceAtLeast(1)
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