package com.galeria.defensores.data

import android.content.Context
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.AttributeConfig
import com.galeria.defensores.models.defaultAttributes
import com.galeria.defensores.models.defaultDerivedStats
import com.galeria.defensores.models.DiceConfig
import java.io.File

object RuleSystemRepository {
    private const val PREFIX = "system_"
    private const val BASE_SYSTEM_ID = "3det_alpha_base"
    
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

    private suspend fun ensureBaseSystemExists() {
        // Check if base system exists, if not create it
        val context = getContext() ?: return
        val baseFile = File(context.filesDir, "$PREFIX$BASE_SYSTEM_ID.json")
        if (!baseFile.exists()) {
            val baseSystem = RuleSystem(
                id = BASE_SYSTEM_ID,
                name = "3DeT Alpha",
                description = "Sistema padrão do 3DeT Alpha. Não pode ser excluído.",
                isBaseSystem = true,
                attributes = defaultAttributes(),
                derivedStats = defaultDerivedStats(),
                diceConfig = DiceConfig(1, 6)
            )
            LocalFileManager.saveJson(context, "$PREFIX$BASE_SYSTEM_ID.json", baseSystem)
        }
    }

    suspend fun getSystems(): List<RuleSystem> {
        val context = getContext() ?: return emptyList()
        val allFiles = LocalFileManager.listFiles(context, PREFIX)
        val systems = mutableListOf<RuleSystem>()
        
        // Always ensure base is loaded first or exists
        // ensureBaseSystemExists() // Already called in init, but safe to call? Sync vs suspend.
        
        for (file in allFiles) {
            val sys = LocalFileManager.readJson(context, file.name, RuleSystem::class.java)
            if (sys != null) {
                systems.add(sys)
            }
        }
        
        // Sort: Base first, then alphabetical
        return systems.sortedWith(compareBy({ !it.isBaseSystem }, { it.name }))
    }

    suspend fun getSystem(id: String): RuleSystem? {
        val context = getContext() ?: return null
        return LocalFileManager.readJson(context, "$PREFIX$id.json", RuleSystem::class.java)
    }
    
    // Fallback if ID is null (default to base)
    suspend fun getSystemOrDefault(id: String?): RuleSystem {
        if (id == null) return getBaseSystem()
        return getSystem(id) ?: getBaseSystem()
    }
    
    private suspend fun getBaseSystem(): RuleSystem {
        val context = getContext() ?: return RuleSystem() // fallback in worst case
        return LocalFileManager.readJson(context, "$PREFIX$BASE_SYSTEM_ID.json", RuleSystem::class.java) 
               ?: RuleSystem(id = BASE_SYSTEM_ID, isBaseSystem = true)
    }

    suspend fun saveSystem(system: RuleSystem) {
        val context = getContext() ?: return
        // Prevent overwriting base system properties if it were possible to edit them casually
        // But we might want to allow "resetting" base system? For now, just save.
        LocalFileManager.saveJson(context, "$PREFIX${system.id}.json", system)
    }

    suspend fun deleteSystem(id: String): Boolean {
        if (id == BASE_SYSTEM_ID) return false // Cannot delete base
        val context = getContext() ?: return false
        return LocalFileManager.deleteFile(context, "$PREFIX$id.json")
    }
}
