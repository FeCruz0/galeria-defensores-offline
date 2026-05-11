package com.galeria.defensores.data

import kotlinx.coroutines.flow.*
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

    fun getCharacters(tableId: String? = null): Flow<List<Character>> {
        val flow = if (tableId == null) {
            characterDao.getAllReactive()
        } else {
            characterDao.getByTable(tableId)
        }
        return flow.map { entities -> entities.map { it.toCharacter() } }
    }

    fun getCharactersForUser(userId: String): Flow<List<Character>> {
        return characterDao.getByOwner(userId).map { entities -> 
            entities.map { it.toCharacter() } 
        }
    }

    fun getCharacter(id: String): Flow<Character?> {
        return characterDao.getById(id).map { it?.toCharacter() }
    }

    suspend fun getCharacterOnce(id: String): Character? {
        return characterDao.getByIdOnce(id)?.toCharacter()
    }

    suspend fun saveCharacter(character: Character) {
        if (character.tableId.isNotEmpty() && character.name.isNotBlank()) {
            val existing = getCharacters(character.tableId).first()
            if (existing.any { it.name.equals(character.name, ignoreCase = true) && it.id != character.id }) {
                throw IllegalArgumentException("Já existe um personagem com o nome '${character.name}' nesta mesa.")
            }
        }
        characterDao.insert(com.galeria.defensores.data.database.entities.CharacterEntity.fromCharacter(character))
    }

    suspend fun deleteCharacter(id: String): Boolean {
        characterDao.deleteById(id)
        return true
    }

    suspend fun unlinkCharactersFromTable(tableId: String) {
        val characters = getCharacters(tableId).first()
        for (char in characters) {
            val updatedChar = char.copy(tableId = "")
            saveCharacter(updatedChar)
        }
    }
}
