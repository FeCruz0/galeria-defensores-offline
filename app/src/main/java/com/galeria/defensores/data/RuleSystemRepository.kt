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

    fun init(context: Context) {
        appContext = context.applicationContext
        // Using GlobalScope or runBlocking here since this mimics a lightweight dependency injection initialization
        // For a proper architecture, this should be done in a coroutine scope aware of the lifecycle, 
        // but for now we use runBlocking to ensure base system exists before usage.
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
        // We no longer rely on the file for the base system logic, 
        // but we might save it for valid file listing consistency if needed.
        val context = getContext() ?: return
        val baseFile = File(context.filesDir, "$PREFIX$BASE_SYSTEM_ID.json")
        if (!baseFile.exists()) {
            LocalFileManager.saveJson(context, "$PREFIX$BASE_SYSTEM_ID.json", createBaseSystem())
        }
    }

    suspend fun getSystems(): List<RuleSystem> {
        val context = getContext() ?: return emptyList()
        val allFiles = LocalFileManager.listFiles(context, PREFIX)
        val systems = mutableListOf<RuleSystem>()
        
        // Add Base Systems explicitly (Hardcoded Source of Truth)
        systems.add(createBaseSystem())
        systems.add(GaidenData.createSystem())

        for (file in allFiles) {
            // Skip base system files if encountered (we added them manually)
            if (file.name == "$PREFIX$BASE_SYSTEM_ID.json") continue
            if (file.name == "$PREFIX$GAIDEN_SYSTEM_ID.json") continue

            val sys = LocalFileManager.readJson(context, file.name, RuleSystem::class.java)
            if (sys != null) {
                systems.add(sys)
            }
        }
        
        // Sort: Base first, then alphabetical
        return systems.sortedWith(compareBy({ !it.isBaseSystem }, { it.name }))
    }

    suspend fun getSystem(id: String): RuleSystem? {
        if (id == BASE_SYSTEM_ID) return createBaseSystem() // Always return fresh default
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem() // Always return fresh default
        
        val context = getContext() ?: return null
        return LocalFileManager.readJson(context, "$PREFIX$id.json", RuleSystem::class.java)
    }
    
    // Fallback if ID is null (default to base)
    suspend fun getSystemOrDefault(id: String?): RuleSystem {
        if (id == null || id == BASE_SYSTEM_ID) return createBaseSystem()
        if (id == GAIDEN_SYSTEM_ID) return GaidenData.createSystem()
        return getSystem(id) ?: createBaseSystem()
    }
    
    private suspend fun getBaseSystem(): RuleSystem {
        return createBaseSystem()
    }

    suspend fun saveSystem(system: RuleSystem) {
        if (system.id == BASE_SYSTEM_ID) return // PREVENT SAVING BASE SYSTEM
        if (system.id == GAIDEN_SYSTEM_ID) return // PREVENT SAVING GAIDEN SYSTEM
        val context = getContext() ?: return
        LocalFileManager.saveJson(context, "$PREFIX${system.id}.json", system)
    }

    suspend fun deleteSystem(id: String): Boolean {
        if (id == BASE_SYSTEM_ID) return false // Cannot delete base
        if (id == GAIDEN_SYSTEM_ID) return false // Cannot delete gaiden
        val context = getContext() ?: return false
        return LocalFileManager.deleteFile(context, "$PREFIX$id.json")
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
