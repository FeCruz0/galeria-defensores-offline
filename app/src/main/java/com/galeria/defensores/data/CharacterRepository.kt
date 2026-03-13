package com.galeria.defensores.data

import com.galeria.defensores.models.Character
import com.galeria.defensores.data.database.daos.CharacterDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepository @Inject constructor(
    private val characterDao: CharacterDao
) {
    // Legacy init removed as Hilt handles injection
    fun init(context: android.content.Context) {}

    suspend fun getCharacters(tableId: String? = null): List<Character> {
        val entities = if (tableId == null) {
            characterDao.getAll()
        } else {
            characterDao.getByTable(tableId)
        }
        return entities.map { it.toCharacter() }
    }

    suspend fun getCharactersForUser(userId: String): List<Character> {
        val entities = characterDao.getByOwner(userId)
        return entities.map { it.toCharacter() }
    }

    suspend fun getCharacter(id: String): Character? {
        return characterDao.getById(id)?.toCharacter()
    }

    suspend fun saveCharacter(character: Character) {
        characterDao.insert(com.galeria.defensores.data.database.entities.CharacterEntity.fromCharacter(character))
    }

    suspend fun deleteCharacter(id: String): Boolean {
        characterDao.deleteById(id)
        return true
    }

    suspend fun unlinkCharactersFromTable(tableId: String) {
        val characters = getCharacters(tableId)
        for (char in characters) {
            val updatedChar = char.copy(tableId = "")
            saveCharacter(updatedChar)
        }
    }
}
