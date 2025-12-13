package com.galeria.defensores.models

import java.util.UUID

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "", // nome/descrição
    val quantity: String = "1" // quantidade
)
