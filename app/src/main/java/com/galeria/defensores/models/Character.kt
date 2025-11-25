package com.galeria.defensores.models

data class Character(
    val name: String,
    val age: Int,
    val race: String,
    val profession: String,
    val attributes: Map<String, Int>,
    val skills: List<String>,
    val equipment: List<String>
) {
    fun getCharacterSummary(): String {
        return "$name, the $age-year-old $race $profession"
    }
}