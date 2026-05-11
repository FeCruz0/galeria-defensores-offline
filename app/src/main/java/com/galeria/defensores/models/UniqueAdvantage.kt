package com.galeria.defensores.models

import java.io.Serializable as JavaSerializable
import kotlinx.serialization.Serializable

@Serializable
data class UniqueAdvantage(
    var name: String = "",
    var group: String = "",
    var cost: Int = 0,
    var benefits: String = "",
    var weaknesses: String = ""
) : JavaSerializable
