package com.galeria.defensores.data

import android.content.Context
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.defaultAttributes
import com.galeria.defensores.models.defaultResources
import com.galeria.defensores.models.DiceConfig
import java.io.File

object RuleSystemRepository {
    private const val PREFIX = "system_"
    private const val BASE_SYSTEM_ID = "3det_alpha_base"
    private const val GAIDEN_SYSTEM_ID = "3det_gaiden_base"
    
    private lateinit var appContext: Context
    private lateinit var database: com.galeria.defensores.data.database.AppDatabase

    fun init(context: Context) {
        appContext = context.applicationContext
        database = com.galeria.defensores.data.database.AppDatabase.getDatabase(appContext)
        
        kotlinx.coroutines.runBlocking {
            ensureBaseSystemExists()
        }
    }

    private fun getContext(): Context? {
        return if (::appContext.isInitialized) appContext else null
    }

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

    private suspend fun ensureBaseSystemExists() {
        if (!::database.isInitialized) return
        if (database.ruleSystemDao().getById(BASE_SYSTEM_ID) == null) {
            database.ruleSystemDao().insert(com.galeria.defensores.data.database.entities.RuleSystemEntity.fromRuleSystem(createBaseSystem()))
        }
    }

    suspend fun getSystems(): List<RuleSystem> {
        if (!::database.isInitialized) return listOf(createBaseSystem(), GaidenData.createSystem())
        
        val systems = mutableListOf<RuleSystem>()
        systems.add(createBaseSystem())
        systems.add(GaidenData.createSystem())

        val dbSystems = database.ruleSystemDao().getAll()
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
        
        if (!::database.isInitialized) return null
        return database.ruleSystemDao().getById(id)?.toRuleSystem()
    }
    
    suspend fun getSystemOrDefault(id: String?): RuleSystem {
        if (id == null || id == BASE_SYSTEM_ID) return createBaseSystem()
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem()
        return getSystem(id) ?: createBaseSystem()
    }

    suspend fun saveSystem(system: RuleSystem) {
        if (system.id == BASE_SYSTEM_ID) return
        if (system.id == GAIDEN_SYSTEM_ID) return
        if (!::database.isInitialized) return
        database.ruleSystemDao().insert(com.galeria.defensores.data.database.entities.RuleSystemEntity.fromRuleSystem(system))
    }

    suspend fun deleteSystem(id: String): Boolean {
        if (id == BASE_SYSTEM_ID) return false
        if (id == GAIDEN_SYSTEM_ID) return false
        if (!::database.isInitialized) return false
        database.ruleSystemDao().deleteById(id)
        return true
    }

    suspend fun resetBaseSystem() {
        val context = getContext() ?: return
        val baseSystem = RuleSystem(
            id = BASE_SYSTEM_ID,
            name = "3DeT Alpha",
            description = "Sistema padrão do 3DeT Alpha. Não pode ser excluído.",
            isBaseSystem = true,
            attributes = defaultAttributes().toMutableList(),
            resources = defaultResources().toMutableList(),
            diceConfig = DiceConfig(1, 6)
        )
        // Force overwrite
        LocalFileManager.saveJson(context, "$PREFIX$BASE_SYSTEM_ID.json", baseSystem)
    }
}
