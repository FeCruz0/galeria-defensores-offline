package com.galeria.defensores.domain.usecases

import com.galeria.defensores.data.*
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.RuleSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case to load a character and its associated rule system.
 * Also performs data migrations and upgrades (e.g., modular advantages).
 */
class LoadCharacterUseCase {

    suspend operator fun invoke(id: String?, tableId: String?): CharacterResult = withContext(Dispatchers.IO) {
        try {
            // 1. Load Character from repository or create default
            var loadedChar: Character? = null
            if (id != null) {
                loadedChar = CharacterRepository.getCharacter(id)
            }
            
            if (loadedChar == null) {
                val currentUser = SessionManager.currentUser
                loadedChar = Character(
                    tableId = tableId ?: "",
                    ownerId = currentUser?.id ?: ""
                )
            }
            
            // 2. Resolve Rule System to load
            val effectiveTableId = if (loadedChar.tableId.isNotEmpty()) loadedChar.tableId else tableId
            val systemToLoad = if (!effectiveTableId.isNullOrEmpty()) {
                val table = TableRepository.getTable(effectiveTableId)
                if (table != null) {
                    RuleSystemRepository.getSystemOrDefault(table.ruleSystemId)
                } else {
                    RuleSystemRepository.getSystemOrDefault(null)
                }
            } else {
                RuleSystemRepository.getSystemOrDefault(null)
            }
            
            // 3. Update active data in repositories
            AdvantagesRepository.loadSystem(systemToLoad)
            DisadvantagesRepository.loadSystem(systemToLoad)
            SkillsRepository.loadSystem(systemToLoad)
            
            // 4. Perform migrations and data upgrades
            val character = performMigrations(loadedChar, systemToLoad)
            
            CharacterResult.Success(character, systemToLoad)
        } catch (e: Exception) {
            CharacterResult.Error(e)
        }
    }
    
    private fun performMigrations(char: Character, system: RuleSystem): Character {
        // Sync legacy fields for attributes if map is empty
        if (char.attributeValues.isEmpty()) {
            char.attributeValues["forca"] = char.forca
            char.attributeValues["habilidade"] = char.habilidade
            char.attributeValues["resistencia"] = char.resistencia
            char.attributeValues["armadura"] = char.armadura
            char.attributeValues["poderFogo"] = char.poderFogo
        }
        
        // Ensure standard resource values are initialized in the map
        char.resourceValues["pv"] = char.currentPv
        char.resourceValues["pm"] = char.currentPm
        
        // Upgrade legacy advantages to Modular if a match is found in current data sources
        char.vantagens = char.vantagens.map { adv ->
            if (!adv.isModular) {
                val defaultMatch = AdvantagesData.defaultAdvantages.find { it.name.trim().equals(adv.name.trim(), ignoreCase = true) }
                if (defaultMatch != null && defaultMatch.isModular) {
                    adv.copy(isModular = true, baseCostPt = defaultMatch.baseCostPt, modifiers = defaultMatch.modifiers)
                } else {
                    val gaidenMatch = GaidenData.createSystem().advantages.find { it.name.trim().equals(adv.name.trim(), ignoreCase = true) }
                    if (gaidenMatch != null && gaidenMatch.isModular) {
                        adv.copy(isModular = true, baseCostPt = gaidenMatch.baseCostPt, modifiers = gaidenMatch.modifiers)
                    } else {
                        adv
                    }
                }
            } else {
                adv
            }
        }.toMutableList()

        // Upgrade legacy disadvantages to Modular
        char.desvantagens = char.desvantagens.map { disadv ->
            if (!disadv.isModular) {
                val defaultMatch = DisadvantagesData.defaultDisadvantages.find { it.name.trim().equals(disadv.name.trim(), ignoreCase = true) }
                if (defaultMatch != null && defaultMatch.isModular) {
                    disadv.copy(isModular = true, baseCostPt = defaultMatch.baseCostPt, modifiers = defaultMatch.modifiers)
                } else {
                    disadv
                }
            } else {
                disadv
            }
        }.toMutableList()
        
        return char
    }
}

/**
 * Result wrapper for the LoadCharacterUseCase.
 */
sealed class CharacterResult {
    data class Success(val character: Character, val ruleSystem: RuleSystem) : CharacterResult()
    data class Error(val throwable: Throwable) : CharacterResult()
}
