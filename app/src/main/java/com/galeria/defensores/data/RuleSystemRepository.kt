package com.galeria.defensores.data

import com.galeria.defensores.data.database.daos.RuleSystemDao
import com.galeria.defensores.data.database.entities.RuleSystemEntity
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.defaultAttributes
import com.galeria.defensores.models.defaultResources
import com.galeria.defensores.models.DiceConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleSystemRepository @Inject constructor(
    private val ruleSystemDao: RuleSystemDao
) {
    private val BASE_SYSTEM_ID = "3det_alpha_base"
    private val GAIDEN_SYSTEM_ID = "3det_gaiden_base"

    private fun createBaseSystem(): RuleSystem {
        return RuleSystem(
            id = BASE_SYSTEM_ID,
            name = "3DeT Alpha",
            description = "Sistema padrão do 3DeT Alpha. Não pode ser excluído.",
            isBaseSystem = true,
            attributes = defaultAttributes().toMutableList(),
            resources = defaultResources().toMutableList(),
            diceConfig = DiceConfig(1, 6)
        )
    }

    suspend fun ensureBaseSystemExists() {
        if (ruleSystemDao.getById(BASE_SYSTEM_ID) == null) {
            ruleSystemDao.insert(RuleSystemEntity.fromRuleSystem(createBaseSystem()))
        }
    }

    suspend fun getSystems(): List<RuleSystem> {
        val systems = mutableListOf<RuleSystem>()
        systems.add(createBaseSystem())
        systems.add(GaidenData.createSystem())

        val dbSystems = ruleSystemDao.getAll()
        dbSystems.forEach { entity ->
            if (entity.id != BASE_SYSTEM_ID && entity.id != GAIDEN_SYSTEM_ID) {
                systems.add(entity.toRuleSystem())
            }
        }

        return systems.sortedWith(compareBy({ !it.isBaseSystem }, { it.name }))
    }

    suspend fun getSystem(id: String): RuleSystem? {
        if (id == BASE_SYSTEM_ID) return createBaseSystem()
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem()
        return ruleSystemDao.getById(id)?.toRuleSystem()
    }

    suspend fun getSystemOrDefault(id: String?): RuleSystem {
        if (id == null || id == BASE_SYSTEM_ID) return createBaseSystem()
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem()
        return getSystem(id) ?: createBaseSystem()
    }

    suspend fun saveSystem(system: RuleSystem) {
        if (system.id == BASE_SYSTEM_ID) return
        if (system.id == GAIDEN_SYSTEM_ID) return
        ruleSystemDao.insert(RuleSystemEntity.fromRuleSystem(system))
    }

    suspend fun deleteSystem(id: String): Boolean {
        if (id == BASE_SYSTEM_ID) return false
        if (id == GAIDEN_SYSTEM_ID) return false
        ruleSystemDao.deleteById(id)
        return true
    }

    suspend fun resetBaseSystem() {
        ruleSystemDao.insert(RuleSystemEntity.fromRuleSystem(createBaseSystem()))
    }
}
