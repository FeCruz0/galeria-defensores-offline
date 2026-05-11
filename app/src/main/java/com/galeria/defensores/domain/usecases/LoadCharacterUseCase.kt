package com.galeria.defensores.domain.usecases

import com.galeria.defensores.data.*
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.RuleSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Use Case to load a character and its associated rule system.
 * Also performs data migrations and upgrades (e.g., modular advantages).
 */
class LoadCharacterUseCase @javax.inject.Inject constructor(
    private val characterRepository: com.galeria.defensores.data.CharacterRepository,
    private val tableRepository: com.galeria.defensores.data.TableRepository,
    private val ruleSystemRepository: com.galeria.defensores.data.RuleSystemRepository
) {

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    operator fun invoke(id: String?, tableId: String?): Flow<CharacterResult> {
        val currentUser = com.galeria.defensores.data.SessionManager.currentUser
        
        if (id == null) {
            return flow {
                val fallback = Character(
                    tableId = tableId ?: "",
                    ownerId = currentUser?.id ?: ""
                )
                val system = resolveRuleSystemSync(tableId)
                emit(CharacterResult.Success(performMigrations(fallback, system), system))
            }.flowOn(Dispatchers.IO)
        }

        return flow {
            val loadedChar = characterRepository.getCharacterOnce(id)
            val char = loadedChar ?: Character(
                tableId = tableId ?: "",
                ownerId = currentUser?.id ?: ""
            )
            
            val effectiveTableId = if (char.tableId.isNotEmpty()) char.tableId else tableId
            
            val sysId = if (!effectiveTableId.isNullOrEmpty()) {
                val table = tableRepository.getTableOnce(effectiveTableId)
                table?.ruleSystemId ?: RuleSystemRepository.BASE_SYSTEM_ID
            } else {
                RuleSystemRepository.BASE_SYSTEM_ID
            }
            
            val sys = ruleSystemRepository.getSystemOnce(sysId)
            
            try {
                val finalSys = sys ?: ruleSystemRepository.getSystemOnce(RuleSystemRepository.BASE_SYSTEM_ID) ?: RuleSystem(id = RuleSystemRepository.BASE_SYSTEM_ID, name = "3D&T ALPHA")
                
                AdvantagesRepository.loadSystem(finalSys)
                DisadvantagesRepository.loadSystem(finalSys)
                SkillsRepository.loadSystem(finalSys)
                
                val migratedChar = performMigrations(char, finalSys)
                emit(CharacterResult.Success(migratedChar, finalSys))
            } catch (e: Exception) {
                emit(CharacterResult.Error(e))
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun resolveRuleSystemSync(tableId: String?): RuleSystem {
        return if (!tableId.isNullOrEmpty()) {
            val table = tableRepository.getTableOnce(tableId)
            if (table != null) {
                ruleSystemRepository.getSystemOrDefault(table.ruleSystemId)
            } else {
                ruleSystemRepository.getSystemOrDefault(null)
            }
        } else {
            ruleSystemRepository.getSystemOrDefault(null)
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
