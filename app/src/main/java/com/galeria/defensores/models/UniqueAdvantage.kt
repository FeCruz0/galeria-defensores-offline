package com.galeria.defensores.models

import java.io.Serializable

data class UniqueAdvantage(
    var name: String = "",
    var group: String = "",
    var cost: Int = 0,
    var benefits: String = "",
    var weaknesses: String = ""
) : Serializable
