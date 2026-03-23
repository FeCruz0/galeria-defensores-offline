package com.galeria.defensores.data

import com.galeria.defensores.data.database.daos.RuleSystemDao
import com.galeria.defensores.data.database.entities.RuleSystemEntity
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.defaultAttributes
import com.galeria.defensores.models.defaultResources
import com.galeria.defensores.models.DiceConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

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
        if (ruleSystemDao.getById(BASE_SYSTEM_ID).first() == null) {
            ruleSystemDao.insert(RuleSystemEntity.fromRuleSystem(createBaseSystem()))
        }
    }

    fun getSystems(): Flow<List<RuleSystem>> {
        return ruleSystemDao.getAllReactive().map { dbSystems ->
            val systems = mutableListOf<RuleSystem>()
            systems.add(createBaseSystem())
            systems.add(GaidenData.createSystem())

            dbSystems.forEach { entity ->
                if (entity.id != BASE_SYSTEM_ID && entity.id != GAIDEN_SYSTEM_ID) {
                    systems.add(entity.toRuleSystem())
                }
            }
            systems.sortedWith(compareBy({ !it.isBaseSystem }, { it.name }))
        }
    }

    fun getSystem(id: String): Flow<RuleSystem?> {
        if (id == GAIDEN_SYSTEM_ID) return flow { emit(GaidenData.createSystem()) }
        return ruleSystemDao.getById(id).map { entity -> 
            if (entity == null && id == BASE_SYSTEM_ID) {
                createBaseSystem()
            } else {
                entity?.toRuleSystem()
            }
        }
    }

    suspend fun getSystemOnce(id: String): RuleSystem? {
        return getSystem(id).first()
    }

    suspend fun getSystemOrDefault(id: String?): RuleSystem {
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem()
        val targetId = if (id.isNullOrEmpty()) BASE_SYSTEM_ID else id
        return getSystemOnce(targetId) ?: createBaseSystem()
    }

    suspend fun saveSystem(system: RuleSystem) {
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
