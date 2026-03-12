package com.galeria.defensores.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.models.RollType
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.models.RuleSystem
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val _character = MutableLiveData<Character>()
    val character: LiveData<Character> = _character

    private val _isRolling = MutableLiveData<Boolean>()
    val isRolling: LiveData<Boolean> = _isRolling

    private val _lastRoll = MutableLiveData<RollResult>()
    val lastRoll: LiveData<RollResult> = _lastRoll

    private val _rollEvent = MutableLiveData<com.galeria.defensores.utils.Event<RollResult>>()

    val rollEvent: LiveData<com.galeria.defensores.utils.Event<RollResult>> = _rollEvent

    // Rule System
    private val _ruleSystem = MutableLiveData<RuleSystem>(RuleSystem()) // Default
    val ruleSystem: LiveData<RuleSystem> = _ruleSystem
    
    // Helper property for internal access, derived from LiveData value or separate tracking
    // We can just use _ruleSystem.value!! since we initialized it.
    private val currentRuleSystem: RuleSystem
        get() = _ruleSystem.value!!

    // Domain Layer (Use Cases)
    private val getResourceMaxUseCase = com.galeria.defensores.domain.usecases.GetResourceMaxUseCase()
    private val calculateStandardRollUseCase = com.galeria.defensores.domain.usecases.CalculateStandardRollUseCase()
    private val calculateCustomRollUseCase = com.galeria.defensores.domain.usecases.CalculateCustomRollUseCase()
    private val loadCharacterUseCase = com.galeria.defensores.domain.usecases.LoadCharacterUseCase()

    fun getAttributeName(key: String): String {
        return currentRuleSystem.attributes.find { it.key == key }?.name ?: key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    
    fun getDerivedStatName(key: String): String {
         return currentRuleSystem.resources.find { it.key == key }?.name ?: key.uppercase()
    }

    // Settings
    var isAnimationEnabled = true

    fun loadCharacter(id: String?, tableId: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("CharacterDebug", "Loading character: id=$id, tableId=$tableId")
            
            when (val result = loadCharacterUseCase(id, tableId)) {
                is com.galeria.defensores.domain.usecases.CharacterResult.Success -> {
                    val systemToLoad = result.ruleSystem
                    val loadedChar = result.character
                    
                    android.util.Log.d("SystemDebug", "Loaded RuleSystem: ${systemToLoad.id} (${systemToLoad.name})")
                    _ruleSystem.value = systemToLoad

                    val effectiveTableId = if (loadedChar.tableId.isNotEmpty()) loadedChar.tableId else tableId
                    loadDamageTypes(effectiveTableId)

                    _character.value = loadedChar
                }
                is com.galeria.defensores.domain.usecases.CharacterResult.Error -> {
                    android.util.Log.e("CharacterDebug", "Error loading character", result.throwable)
                    // Create minimal fallback character if error occurs to avoid crashing UI
                    val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                    _character.value = Character(
                        tableId = tableId ?: "",
                        ownerId = currentUser?.id ?: ""
                    )
                }
            }
        }
    }

    fun saveCharacter() {
        _character.value?.let { char ->
            viewModelScope.launch {
                CharacterRepository.saveCharacter(char)
            }
        }
    }

    /**
     * Update an attribute (forca, habilidade, resistencia, armadura, poderFogo).
     * Now also updates the dynamic attributeValues map.
     * When resistencia changes, also update current PV and PM to the new maximums.
     */
    fun updateAttribute(attribute: String, value: Int) {
        val currentChar = _character.value ?: return
        val newValue = value.coerceIn(0, 99)
        
        // Update dynamic map
        currentChar.attributeValues[attribute] = newValue
        
        // Sync with legacy fields for now
        when (attribute) {
            "forca" -> currentChar.forca = newValue
            "habilidade" -> currentChar.habilidade = newValue
            "resistencia" -> {
                currentChar.resistencia = newValue
                // Recalculate max PV/PM and set current values to the new max
                currentChar.currentPv = currentChar.getMaxPv()
                currentChar.currentPm = currentChar.getMaxPm()
            }
            "armadura" -> currentChar.armadura = newValue
            "poderFogo" -> currentChar.poderFogo = newValue
        }
        _character.value = currentChar // Trigger LiveData update
        saveCharacter()
    }

    /**
     * Update status bars (PV or PM) by a delta, respecting the calculated maximums.
     */
    fun updateStatus(type: String, delta: Int) {
        val currentChar = _character.value ?: return
        when (type) {
            "pv" -> {
                val maxPv = currentChar.getMaxPv()
                currentChar.currentPv = (currentChar.currentPv + delta).coerceIn(0, maxPv)
            }
            "pm" -> {
                val maxPm = currentChar.getMaxPm()
                currentChar.currentPm = (currentChar.currentPm + delta).coerceIn(0, maxPm)
            }
        }
        _character.value = currentChar
        saveCharacter()
    }

    fun setStatus(type: String, value: Int) {
        val currentChar = _character.value ?: return
        when (type) {
            "pv" -> {
                val maxPv = currentChar.getMaxPv()
                currentChar.currentPv = value.coerceIn(0, maxPv)
            }
            "pm" -> {
                val maxPm = currentChar.getMaxPm()
                currentChar.currentPm = value.coerceIn(0, maxPm)
            }
        }
        _character.value = currentChar
        saveCharacter()
    }

    /**
     * Update any resource (including custom ones) by a specific value.
     */
    /**
     * Update any resource (including custom ones) by a delta value.
     */
    fun updateResource(key: String, delta: Int) {
        // Check if it's a legacy resource first
        if (key.equals("pv", ignoreCase = true)) {
            updateStatus("pv", delta)
            return
        }
        if (key.equals("pm", ignoreCase = true)) {
            updateStatus("pm", delta)
            return
        }

        val currentChar = _character.value ?: return

        // Custom Resource Logic
        val deficiency = currentRuleSystem.resources.find { it.key == key }
        val max = if (deficiency != null) calculateResourceMax(deficiency, currentChar) else 999
        
        // Get current value, default to max if not set (or 0? Defaulting to max seems safer for initial state)
        val current = currentChar.resourceValues[key] ?: max
        
        // Update generic map
        currentChar.resourceValues[key] = (current + delta).coerceIn(0, max)
        
        _character.value = currentChar
        saveCharacter()
    }

    /**
     * Calculates the maximum value for a given resource based on its formula.
     * key: Attribute keys (F, H, R, A, PdF) or numbers.
     * Supported operators: +, -, *, /
     */
    fun calculateResourceMax(res: com.galeria.defensores.models.ResourceDefinition, char: com.galeria.defensores.models.Character): Int {
        return getResourceMaxUseCase(res, char, currentRuleSystem)
    }

    fun updateName(name: String) {
        val currentChar = _character.value ?: return
        currentChar.name = name
        _character.value = currentChar
        saveCharacter()
    }

    fun updateScale(scale: Int) {
        val currentChar = _character.value ?: return
        currentChar.scale = scale
        _character.value = currentChar
        saveCharacter()
    }



    fun updateSavedPoints(points: Int) {
        val currentChar = _character.value ?: return
        currentChar.savedPoints = points.coerceAtLeast(0)
        _character.value = currentChar
        saveCharacter()
    }

    fun updateExperience(xp: Int) {
        val currentChar = _character.value ?: return
        var newXp = xp.coerceAtLeast(0)
        var newSaved = currentChar.savedPoints

        if (newXp >= 10) {
            val pointsToAdd = newXp / 10
            newXp = newXp % 10
            newSaved += pointsToAdd
        }

        if (newXp != currentChar.experience || newSaved != currentChar.savedPoints) {
            currentChar.experience = newXp
            currentChar.savedPoints = newSaved
            _character.value = currentChar
            saveCharacter()
        }
    }

    // --- Damage Types Logic ---
    private val defaultDamageTypes = listOf(
        "Corte", "Perfuração", "Esmagamento", 
        "Fogo", "Frio", "Elétrico", "Químico", "Sônico"
    )

    private val _availableDamageTypes = MutableLiveData<List<String>>()
    val availableDamageTypes: LiveData<List<String>> = _availableDamageTypes

    private var currentTableId: String? = null

    // Call this when loading character or table
    fun loadDamageTypes(tableId: String?) {
        this.currentTableId = tableId
        viewModelScope.launch {
            val customTypes = if (!tableId.isNullOrEmpty()) {
                val table = TableRepository.getTable(tableId)
                table?.customDamageTypes ?: emptyList()
            } else {
                emptyList()
            }
            val allTypes = (defaultDamageTypes + customTypes).distinct().sorted()
            _availableDamageTypes.value = allTypes
        }
    }

    fun updateDamageType(type: String, isPdf: Boolean) {
        val currentChar = _character.value ?: return
        if (isPdf) {
            currentChar.damageTypePdf = type
        } else {
            currentChar.damageTypeForca = type
        }
        _character.value = currentChar
        persistCharacter()
    }

    fun addCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        if (type.isBlank()) return
        
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                if (!table.customDamageTypes.contains(type)) {
                    table.customDamageTypes.add(type)
                    TableRepository.updateTable(table) // Assuming updateTable exists or creating helper
                }
                loadDamageTypes(tableId) // Reload
            }
        }
    }

    fun removeCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                if (table.customDamageTypes.remove(type)) {
                    TableRepository.updateTable(table)
                }
                loadDamageTypes(tableId) // Reload
            }
        }
    }

    // --- Rule System Editing (Attributes) ---
    fun addAttributeDefinition(attr: com.galeria.defensores.models.AttributeDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        android.util.Log.d("SystemDebug", "Adding attribute '${attr.name}' to system ID: ${currentSystem.id}")
        
        val newAttributes = currentSystem.attributes.toMutableList()
        newAttributes.add(attr)
        
        // Use copy to ensure LiveData triggers update
        val newSystem = currentSystem.copy(attributes = newAttributes)
        
        saveRuleSystem(newSystem)
    }

    private fun saveRuleSystem(system: com.galeria.defensores.models.RuleSystem) {
        _ruleSystem.value = system
        viewModelScope.launch {
            withContext(NonCancellable) {
                android.util.Log.d("SystemDebug", "Saving system ID: ${system.id} with ${system.attributes.size} attributes")
                // In offline mode, we allow editing the base system directly
                if (system.id == "3det_alpha_base" || system.isBaseSystem) {
                    android.util.Log.d("SystemDebug", "Overwriting Base System configuration for Offline Mode.")
                }
                com.galeria.defensores.data.RuleSystemRepository.saveSystem(system)
            }
        }
    }

    fun exportSystemJson(): String {
        return try {
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            gson.toJson(_ruleSystem.value ?: com.galeria.defensores.models.RuleSystem())
        } catch (e: Exception) {
            android.util.Log.e("SystemExport", "Error exporting system", e)
            "{ \"error\": \"Failed to export system\" }"
        }
    }

    fun importSystemJson(json: String): Boolean {
        return try {
            val gson = com.google.gson.Gson()
            val newSystem = gson.fromJson(json, com.galeria.defensores.models.RuleSystem::class.java)
            
            if (newSystem == null || (newSystem.attributes.isEmpty() && newSystem.resources.isEmpty())) {
                android.util.Log.e("SystemImport", "Invalid system JSON or empty system")
                return false
            }

            val current = _ruleSystem.value ?: com.galeria.defensores.models.RuleSystem()
            // Retain original ID to avoid breaking references in existing characters if strict checks exist?
            // Actually, if we import a system, we likely WANT it to define the rules.
            // But if we change the ID, the Character's ruleSystemId pointer might become invalid 
            // if we are not also updating the Character or Table.
            // In our current offline flow, the Character loads the system from the Table, 
            // or falls back to "null" (default).
            // If we overwrite the CURRENT object in memory and save it, we are effectively editing the loaded system.
            // If the loaded system was the "Default" one (which might be shared), 
            // editing it affects ALL characters using the default. Use caution.
            // For now, in Offline Single Player mode, this is acceptable feature behavior: "Edit the Rules".
            
            // We'll update the content but KEEP the ID if it's the Base System to ensuring we don't spawn infinite files?
            // Actually, let's trust the user or the Import.
            // If we import a system with a different ID, we should probably save it AS a new file 
            // and Switch to it? Or just overwrite the fields of the current system?
            // User request: "Importing a system will OVERWRITE the current system's definition."
            // So we copy fields into the CURRENT ID.
            
            val updatedSystem = current.copy(
                name = newSystem.name,
                description = newSystem.description,
                attributes = newSystem.attributes,
                resources = newSystem.resources,
                diceConfig = newSystem.diceConfig
                // Keep ID, isBaseSystem to prevent breakage of current session
            )
            
            saveRuleSystem(updatedSystem)
            true
        } catch (e: Exception) {
            android.util.Log.e("SystemImport", "Error parsing JSON", e)
            false
        }
    }

    fun saveSystemAs(newName: String, onComplete: (Boolean) -> Unit) {
        val current = _ruleSystem.value ?: return
        
        // Create a copy with a NEW ID
        val newSystem = current.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName,
            isBaseSystem = false // modifications are never base
        )
        
        viewModelScope.launch {
            try {
                com.galeria.defensores.data.RuleSystemRepository.saveSystem(newSystem)
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("SystemSaveAs", "Error saving new system", e)
                onComplete(false)
            }
        }
    }


    fun resetBaseSystem(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                com.galeria.defensores.data.RuleSystemRepository.resetBaseSystem()
                // If current is base, reload it
                val current = _ruleSystem.value
                if (current?.id == "3det_alpha_base") {
                    _ruleSystem.value = com.galeria.defensores.data.RuleSystemRepository.getSystem("3det_alpha_base")
                }
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("SystemReset", "Error resetting base system", e)
                onComplete(false)
            }
        }
    }

    fun updateAttributeDefinition(attr: com.galeria.defensores.models.AttributeDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        val newAttributes = currentSystem.attributes.toMutableList()
        val index = newAttributes.indexOfFirst { it.id == attr.id }
        
        if (index != -1) {
             val oldAttr = newAttributes[index]
             val oldKey = oldAttr.key
             val newKey = attr.key
             
             newAttributes[index] = attr
             
             // Cascading Update: If key changed, update formulas in resources
             var newResources = currentSystem.resources.toMutableList()
             if (oldKey != newKey) {
                 var modifiedResources = false
                 newResources = newResources.map { res ->
                     // Simple replace. Ideally regex \bOLD\b but simple replace is consistent with our parser
                     if (res.formula.contains(oldKey, ignoreCase = true)) {
                         modifiedResources = true
                         // Use regex to replace whole words only?
                         // "INT" -> "INTE" should not replace "INTELIGENCIA" -> "INTELIGENCIAELIGENCIA"
                         // But our attributes are simple keys.
                         // Let's use simple replace for now to match current parser logic, 
                         // or maybe regex with word boundaries for safety.
                         // Regex: \bOLD\b with IGNORE_CASE
                         val pattern = "(?i)\\b$oldKey\\b".toRegex()
                         val newFormula = res.formula.replace(pattern, newKey)
                         res.copy(formula = newFormula)
                     } else {
                         res
                     }
                 }.toMutableList()
                 
                 if (modifiedResources) {
                     android.util.Log.d("SystemDebug", "Updated resource formulas due to attribute rename: $oldKey -> $newKey")
                 }
                 
                 // Also migrate Character values!
                 // If we strictly follow the rule that the system defines the key, we must update the character data.
                 _character.value?.let { char ->
                     if (char.attributeValues.containsKey(oldKey)) {
                         val oldVal = char.attributeValues[oldKey] ?: 0
                         char.attributeValues.remove(oldKey)
                         char.attributeValues[newKey] = oldVal
                         _character.value = char // Trigger update
                         saveCharacter()
                         android.util.Log.d("SystemDebug", "Migrated character attribute value: $oldKey -> $newKey = $oldVal")
                     }
                 }
             }

             val newSystem = currentSystem.copy(attributes = newAttributes, resources = newResources)
             saveRuleSystem(newSystem)
        }
    }

    fun removeAttributeDefinition(attr: com.galeria.defensores.models.AttributeDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        val newAttributes = currentSystem.attributes.toMutableList()
        newAttributes.removeAll { it.id == attr.id }
        currentSystem.attributes = newAttributes
        saveRuleSystem(currentSystem)
        
        // Also clean up character values? 
        // For now, keep them as "orphans" which is safer than deleting data.
    }

    // --- Rule System Editing (Resources) ---
    fun addResourceDefinition(res: com.galeria.defensores.models.ResourceDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        val newList = currentSystem.resources.toMutableList()
        newList.add(res)
        currentSystem.resources = newList
        saveRuleSystem(currentSystem)
    }

    fun updateResourceDefinition(res: com.galeria.defensores.models.ResourceDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        val newList = currentSystem.resources.toMutableList()
        val index = newList.indexOfFirst { it.id == res.id }
        if (index != -1) {
             newList[index] = res
             currentSystem.resources = newList
             saveRuleSystem(currentSystem)
        }
    }

    fun removeResourceDefinition(res: com.galeria.defensores.models.ResourceDefinition) {
        val currentSystem = _ruleSystem.value ?: return
        val newList = currentSystem.resources.toMutableList()
        newList.removeAll { it.id == res.id }
        currentSystem.resources = newList
        saveRuleSystem(currentSystem)
    }

    private val _virtualRollRequest = MutableLiveData<com.galeria.defensores.utils.Event<com.galeria.defensores.models.RollRequest>>()
    val virtualRollRequest: LiveData<com.galeria.defensores.utils.Event<com.galeria.defensores.models.RollRequest>> = _virtualRollRequest

    // Toggle for Virtual Rolls (Default true for testing)
    var isVirtualRollEnabled = true 

    fun finalizeVirtualRoll(diceValues: List<Int>) {
        val requestEvent = _virtualRollRequest.value
        val request = requestEvent?.peekContent()
        val char = _character.value

        if (request == null || char == null) {
            // Error handling or logging
            return
        }

        val result = if (request.type == com.galeria.defensores.models.RollRequestType.CUSTOM && request.customRoll != null) {
            calculateCustomRollUseCase(char, request.customRoll, request.diceOverride ?: diceValues)
        } else {
            // Standard Roll
            val dieVal = (request.diceOverride ?: diceValues).firstOrNull() ?: 1
            
            val rollType = when(request.type) {
                com.galeria.defensores.models.RollRequestType.ATTACK_F -> com.galeria.defensores.models.RollType.ATTACK_F
                com.galeria.defensores.models.RollRequestType.ATTACK_PDF -> com.galeria.defensores.models.RollType.ATTACK_PDF
                com.galeria.defensores.models.RollRequestType.DEFENSE -> com.galeria.defensores.models.RollType.DEFENSE
                com.galeria.defensores.models.RollRequestType.INITIATIVE -> com.galeria.defensores.models.RollType.INITIATIVE
                com.galeria.defensores.models.RollRequestType.ATTRIBUTE -> com.galeria.defensores.models.RollType.ATTRIBUTE
                else -> com.galeria.defensores.models.RollType.ATTRIBUTE
            }

            calculateStandardRollUseCase(
                char, 
                rollType, 
                request.bonus, 
                request.attributeValue, 
                request.skillValue, 
                dieVal
            ) { getAttributeName(it) }
        }

        _lastRoll.value = result
        _rollEvent.value = com.galeria.defensores.utils.Event(result)
        sendRollToTable(result)
    }

    private fun sendRollToTable(result: RollResult) {
        // Logic extracted from existing code if needed, or just let the observer handle it.
        // The existing logic doesn't have a "sendRollToTable" method, it posts to _rollEvent 
        // and the Fragment sends it via ChatViewModel. 
        // So just updating _lastRoll and _rollEvent is sufficient.
        saveRollToHistory(result)
    }
    
    // Helper to save to history (duplicated/extracted logic)
    private fun saveRollToHistory(result: RollResult) {
         val currentChar = _character.value ?: return
         if (currentChar.tableId.isNotEmpty()) {
             viewModelScope.launch {
                com.galeria.defensores.data.TableRepository.addRollToHistory(currentChar.tableId, result)
             }
        }
    }

    fun rollDice(type: RollType) {
        val char = _character.value ?: return
        
        viewModelScope.launch {
            _isRolling.value = true
            var bonus = 0
            var isSpecial = false
            
            // Special PM deduction logic
            if (type == RollType.SPECIAL_F || type == RollType.SPECIAL_PDF) {
                if (char.currentPm < 1) {
                    _isRolling.value = false
                    return@launch
                }
                char.currentPm -= 1
                bonus = 2
                isSpecial = true
                _character.value = char
            }

            var attrVal = 0
            var displayAttr = ""
            var reqType = com.galeria.defensores.models.RollRequestType.ATTACK_F // Default

            when (type) {
                RollType.ATTACK_F, RollType.SPECIAL_F -> {
                    attrVal = char.forca
                    displayAttr = getAttributeName("forca")
                    reqType = com.galeria.defensores.models.RollRequestType.ATTACK_F
                }
                RollType.ATTACK_PDF, RollType.SPECIAL_PDF -> {
                    attrVal = char.poderFogo
                    displayAttr = getAttributeName("poderFogo")
                    reqType = com.galeria.defensores.models.RollRequestType.ATTACK_PDF
                }
                RollType.DEFENSE -> {
                    attrVal = char.armadura
                    displayAttr = getAttributeName("armadura")
                    reqType = com.galeria.defensores.models.RollRequestType.DEFENSE
                }
                RollType.INITIATIVE -> {
                    attrVal = 0
                    displayAttr = "Iniciativa" // Usually standard, but could be customizable?
                    reqType = com.galeria.defensores.models.RollRequestType.INITIATIVE
                }
                RollType.ATTRIBUTE -> {
                     attrVal = 0
                     displayAttr = "Atributo"
                     reqType = com.galeria.defensores.models.RollRequestType.ATTRIBUTE
                }
            }

            if (isVirtualRollEnabled) {
                // Pre-roll die for visual synchronization
                val diceValues = listOf(Random.nextInt(6) + 1)
                val canCrit = (type == RollType.ATTACK_F || type == RollType.ATTACK_PDF || type == RollType.SPECIAL_F || type == RollType.SPECIAL_PDF || type == RollType.DEFENSE)
                val isNegativeRoll = bonus < 0
                val diceProps = listOf(com.galeria.defensores.models.DieProperty(canCrit, isNegativeRoll, 6))

                // Broadcast to table
                if (char.tableId.isNotEmpty()) {
                    val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id ?: char.ownerId
                    val visualRoll = com.galeria.defensores.models.VisualRoll(
                        senderId = currentUserId,
                        senderName = char.name,
                        diceCount = 1,
                        diceValues = diceValues,
                        diceProperties = diceProps,
                        canCrit = canCrit,
                        isNegative = isNegativeRoll,
                        critRangeStart = 6
                    )
                    android.util.Log.d("VisualRollDebug", "Broadcasting standard roll: sender=$currentUserId, table=${char.tableId}")
                    TableRepository.broadcastVisualRoll(char.tableId, visualRoll)
                }

                // Intercept and send request
                val request = com.galeria.defensores.models.RollRequest(
                    type = reqType,
                    diceCount = 1, // Standard 1d6
                    bonus = bonus,
                    attributeValue = attrVal,
                    skillValue = char.habilidade,
                    attributeName = displayAttr,
                    diceOverride = diceValues,
                    diceProperties = diceProps,
                    isNegative = isNegativeRoll,
                    canCrit = canCrit,
                    critRangeStart = 6
                )
                _virtualRollRequest.value = com.galeria.defensores.utils.Event(request)
                // We do NOT update _lastRoll yet.
                _isRolling.value = false // Stop "processing" state
                return@launch
            }

            // ... Existing Logic for non-virtual ...
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val animationEnabled = prefs.getBoolean("animation_enabled", true)

            if (animationEnabled) {
                val animationDuration = 2500L
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < animationDuration) {
                    val fakeDie = Random.nextInt(6) + 1
                    val fakeTotal = attrVal + char.habilidade + fakeDie + bonus
                    val fakeResult = RollResult(
                        total = fakeTotal,
                        die = fakeDie,
                        attributeUsed = displayAttr,
                        attributeValue = attrVal,
                        skillValue = char.habilidade,
                        bonus = bonus,
                        isCritical = fakeDie == 6,
                        timestamp = System.currentTimeMillis(),
                        name = "Rolando...",
                        characterId = char.id
                    )
                    _lastRoll.value = fakeResult
                    delay(100)
                }
            }

            val result = calculateStandardRollUseCase(char, type, bonus, attrVal, char.habilidade, null) { getAttributeName(it) }
            
            _lastRoll.value = result
            _rollEvent.value = com.galeria.defensores.utils.Event(result)
            _isRolling.value = false

            // Save to Table History
            if (char.tableId.isNotEmpty()) {
                com.galeria.defensores.data.TableRepository.addRollToHistory(char.tableId, result)
            }
        }
    }

    fun addAdvantage(advantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        // Create a copy of the list to trigger LiveData update
        val newAdvantages = currentChar.vantagens.toMutableList()
        newAdvantages.add(advantage)
        currentChar.vantagens = newAdvantages
        
        _character.value = currentChar
        saveCharacter()
    }

    fun updateAdvantage(advantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newAdvantages = currentChar.vantagens.toMutableList()
        val index = newAdvantages.indexOfFirst { it.id == advantage.id }
        if (index != -1) {
            newAdvantages[index] = advantage
            currentChar.vantagens = newAdvantages
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun addDisadvantage(disadvantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newDisadvantages = currentChar.desvantagens.toMutableList()
        newDisadvantages.add(disadvantage)
        currentChar.desvantagens = newDisadvantages
        
        _character.value = currentChar
        saveCharacter()
    }

    fun updateDisadvantage(disadvantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newDisadvantages = currentChar.desvantagens.toMutableList()
        val index = newDisadvantages.indexOfFirst { it.id == disadvantage.id }
        if (index != -1) {
            newDisadvantages[index] = disadvantage
            currentChar.desvantagens = newDisadvantages
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeAdvantage(advantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newAdvantages = currentChar.vantagens.toMutableList()
        newAdvantages.removeAll { it.id == advantage.id }
        currentChar.vantagens = newAdvantages
        _character.value = currentChar
        saveCharacter()
    }

    fun removeDisadvantage(disadvantage: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newDisadvantages = currentChar.desvantagens.toMutableList()
        newDisadvantages.removeAll { it.id == disadvantage.id }
        currentChar.desvantagens = newDisadvantages
        _character.value = currentChar
        saveCharacter()
    }

    fun addSkill(skill: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newSkills = currentChar.pericias.toMutableList()
        newSkills.add(skill)
        currentChar.pericias = newSkills
        _character.value = currentChar
        saveCharacter()
    }

    fun updateSkill(skill: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newSkills = currentChar.pericias.toMutableList()
        val index = newSkills.indexOfFirst { it.id == skill.id }
        if (index != -1) {
            newSkills[index] = skill
            currentChar.pericias = newSkills
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeSkill(skill: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newSkills = currentChar.pericias.toMutableList()
        newSkills.removeAll { it.id == skill.id }
        currentChar.pericias = newSkills
        _character.value = currentChar
        saveCharacter()
    }

    fun addSpecializations(specializations: List<com.galeria.defensores.models.AdvantageItem>) {
        val currentChar = _character.value ?: return
        val newSpecs = currentChar.especializacoes.toMutableList()
        newSpecs.addAll(specializations)
        currentChar.especializacoes = newSpecs
        _character.value = currentChar
        saveCharacter()
    }

    fun updateSpecialization(specialization: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newSpecs = currentChar.especializacoes.toMutableList()
        val index = newSpecs.indexOfFirst { it.id == specialization.id }
        if (index != -1) {
            newSpecs[index] = specialization
            currentChar.especializacoes = newSpecs
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeSpecialization(specialization: com.galeria.defensores.models.AdvantageItem) {
        val currentChar = _character.value ?: return
        val newSpecs = currentChar.especializacoes.toMutableList()
        newSpecs.removeAll { it.id == specialization.id }
        currentChar.especializacoes = newSpecs
        _character.value = currentChar
        saveCharacter()
    }

    fun addInventoryItem(item: com.galeria.defensores.models.InventoryItem) {
        val currentChar = _character.value ?: return
        val newList = currentChar.inventario.toMutableList()
        newList.add(item)
        currentChar.inventario = newList
        _character.value = currentChar
        saveCharacter()
    }

    fun updateInventoryItem(item: com.galeria.defensores.models.InventoryItem) {
        val currentChar = _character.value ?: return
        val newList = currentChar.inventario.toMutableList()
        val index = newList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            newList[index] = item
            currentChar.inventario = newList
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeInventoryItem(item: com.galeria.defensores.models.InventoryItem) {
        val currentChar = _character.value ?: return
        val newList = currentChar.inventario.toMutableList()
        newList.removeAll { it.id == item.id }
        currentChar.inventario = newList
        _character.value = currentChar
        saveCharacter()
    }

    fun adjustInventoryQuantity(item: com.galeria.defensores.models.InventoryItem, delta: Int) {
        val currentQty = item.quantity.toIntOrNull()
        if (currentQty != null) {
            val newQty = (currentQty + delta).coerceAtLeast(0)
            val newItem = item.copy(quantity = newQty.toString())
            updateInventoryItem(newItem)
        }
    }

    fun updateNotes(htmlInfo: String) {
        val currentChar = _character.value ?: return
        currentChar.anotacoes = htmlInfo
        _character.value = currentChar
        saveCharacter()
    }

    fun deleteCharacter(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val charId = _character.value?.id ?: return
        viewModelScope.launch {
            val success = CharacterRepository.deleteCharacter(charId)
            if (success) {
                _character.value = null
                onSuccess()
            } else {
                onError("Erro ao excluir. Verifique sua conexão.")
            }
        }
    }
    fun addSpell(spell: com.galeria.defensores.models.Spell) {
        val currentChar = _character.value ?: return
        val newSpells = currentChar.magias.toMutableList()
        newSpells.add(spell)
        currentChar.magias = newSpells
        _character.value = currentChar
        saveCharacter()
    }

    fun updateSpell(spell: com.galeria.defensores.models.Spell) {
        val currentChar = _character.value ?: return
        val newSpells = currentChar.magias.toMutableList()
        val index = newSpells.indexOfFirst { it.id == spell.id }
        if (index != -1) {
            newSpells[index] = spell
            currentChar.magias = newSpells
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeSpell(spell: com.galeria.defensores.models.Spell) {
        val currentChar = _character.value ?: return
        val newSpells = currentChar.magias.toMutableList()
        newSpells.removeAll { it.id == spell.id }
        currentChar.magias = newSpells
        _character.value = currentChar
        saveCharacter()
    }



    // --- Unique Advantage Logic ---
    
    private val _availableUniqueAdvantages = MutableLiveData<List<com.galeria.defensores.models.UniqueAdvantage>>()
    val availableUniqueAdvantages: LiveData<List<com.galeria.defensores.models.UniqueAdvantage>> = _availableUniqueAdvantages

    fun loadUniqueAdvantages(tableId: String?) {
        this.currentTableId = tableId
        viewModelScope.launch {
            val defaults = com.galeria.defensores.data.UniqueAdvantagesData.defaults
            val customUAs = if (!tableId.isNullOrEmpty()) {
                val table = TableRepository.getTable(tableId)
                table?.customUniqueAdvantages ?: emptyList()
            } else {
                emptyList()
            }
            // Merge defaults and customs. We can sort them if we want.
            // Sorting by name seems reasonable.
            val allTypes = (defaults + customUAs).sortedBy { it.name }
            _availableUniqueAdvantages.value = allTypes
        }
    }

    fun setUniqueAdvantage(ua: com.galeria.defensores.models.UniqueAdvantage?) {
        val currentChar = _character.value ?: return
        currentChar.uniqueAdvantage = ua
        _character.value = currentChar
        saveCharacter()
    }

    fun addCustomUniqueAdvantage(ua: com.galeria.defensores.models.UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                // Remove if exists (update) or just add
               val existingIndex = table.customUniqueAdvantages.indexOfFirst { it.name == ua.name } // Check by name uniqueness for simplicity or just allow duplicates? 
               // Better to not allow duplicates with same name to avoid confusion.
               if (existingIndex == -1) {
                   table.customUniqueAdvantages.add(ua)
               } else {
                   // Update if exists?
                   table.customUniqueAdvantages[existingIndex] = ua
               }
                if (TableRepository.updateTable(table)) {
                    loadUniqueAdvantages(tableId)
                }
            }
        }
    }
    
    fun removeCustomUniqueAdvantage(ua: com.galeria.defensores.models.UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                // Logic to remove. Since Custom UAs are saved in the list, we remove by object equality or name.
                // Assuming UA is from the list.
                val removed = table.customUniqueAdvantages.removeIf { it.name == ua.name && it.group == ua.group }
                if (removed) {
                    if (TableRepository.updateTable(table)) {
                        loadUniqueAdvantages(tableId)
                    }
                }
            }
        }
    }
    
     fun updateCustomUniqueAdvantage(oldUA: com.galeria.defensores.models.UniqueAdvantage, newUA: com.galeria.defensores.models.UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                 val index = table.customUniqueAdvantages.indexOfFirst { it.name == oldUA.name && it.group == oldUA.group }
                 if (index != -1) {
                     table.customUniqueAdvantages[index] = newUA
                     if (TableRepository.updateTable(table)) {
                         loadUniqueAdvantages(tableId)
                     }
                 } else {
                     // Maybe it was a default one being "edited" into a custom one?
                     // Requirements say "edit... custom ones".
                     // If user tries to edit a default one, it should probably create a custom one copy?
                     // For now, let's assume we only edit custom ones via this method.
                 }
            }
        }
    }

    // --- Custom Rolls Logic ---
    fun addCustomRoll(roll: com.galeria.defensores.models.CustomRoll) {
        val currentChar = _character.value ?: return
        val newRolls = currentChar.customRolls.toMutableList()
        newRolls.add(roll)
        currentChar.customRolls = newRolls
        _character.value = currentChar
        saveCharacter()
    }

    fun updateCustomRoll(roll: com.galeria.defensores.models.CustomRoll) {
        val currentChar = _character.value ?: return
        val newRolls = currentChar.customRolls.toMutableList()
        val index = newRolls.indexOfFirst { it.id == roll.id }
        if (index != -1) {
            newRolls[index] = roll
            currentChar.customRolls = newRolls
            _character.value = currentChar
            saveCharacter()
        }
    }

    fun removeCustomRoll(roll: com.galeria.defensores.models.CustomRoll) {
        val currentChar = _character.value ?: return
        val newRolls = currentChar.customRolls.toMutableList()
        newRolls.removeAll { it.id == roll.id }
        currentChar.customRolls = newRolls
        _character.value = currentChar
        saveCharacter()
    }

    fun rollCustom(roll: com.galeria.defensores.models.CustomRoll) {
        val char = _character.value ?: return
        
        viewModelScope.launch {
            _isRolling.value = true

            if (isVirtualRollEnabled) {
                // Determine dice count (sum of all dice components)
                // For simplified virtual roll, we might just spawn X generic dice.
                var diceCount = 0
                roll.components.forEach { diceCount += it.count }
                
                // Pre-calculate dice values and determine crit range
                val diceValues = mutableListOf<Int>()
                val diceProps = mutableListOf<com.galeria.defensores.models.DieProperty>()
                val combatNameCheck = roll.name.contains("Ataque", ignoreCase = true) || 
                                     roll.name.contains("Defesa", ignoreCase = true) || 
                                     roll.name.contains("PdF", ignoreCase = true) ||
                                     roll.name.contains(" F ", ignoreCase = true)

                var minCritRange = 6
                roll.components.forEach { comp ->
                    if (comp.canCrit && (comp.critRangeStart ?: comp.faces) < minCritRange) {
                        minCritRange = comp.critRangeStart ?: comp.faces
                    }
                    repeat(comp.count) {
                        diceValues.add(Random.nextInt(comp.faces) + 1)
                        diceProps.add(com.galeria.defensores.models.DieProperty(
                            canCrit = comp.canCrit || combatNameCheck,
                            isNegative = comp.isNegative,
                            critRangeStart = comp.critRangeStart ?: comp.faces
                        ))
                    }
                }

                val isNegativeRoll = roll.globalModifier < 0
                val canCrit = roll.components.any { it.canCrit } || combatNameCheck

                // Broadcast
                if (char.tableId.isNotEmpty()) {
                    val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id ?: char.ownerId
                    val visualRoll = com.galeria.defensores.models.VisualRoll(
                        senderId = currentUserId,
                        senderName = char.name,
                        diceCount = diceValues.size,
                        diceValues = diceValues,
                        diceProperties = diceProps,
                        canCrit = canCrit,
                        isNegative = isNegativeRoll,
                        critRangeStart = minCritRange
                    )
                    android.util.Log.d("VisualRollDebug", "Broadcasting custom roll: sender=$currentUserId, table=${char.tableId}")
                    TableRepository.broadcastVisualRoll(char.tableId, visualRoll)
                }

                // Intercept and send request
                val totalDice = roll.components.sumOf { it.count }
                val request = com.galeria.defensores.models.RollRequest(
                    type = com.galeria.defensores.models.RollRequestType.CUSTOM,
                    diceCount = totalDice,
                    bonus = roll.globalModifier,
                    attributeValue = 0,
                    skillValue = char.habilidade,
                    attributeName = roll.name,
                    customRoll = roll,
                    diceOverride = diceValues,
                    diceProperties = diceProps,
                    isNegative = isNegativeRoll,
                    canCrit = canCrit,
                    critRangeStart = minCritRange
                )
                _virtualRollRequest.value = com.galeria.defensores.utils.Event(request)
                _isRolling.value = false
                return@launch
            }

            // ... Existing Logic ...
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val animationEnabled = prefs.getBoolean("animation_enabled", true)
            
            if (animationEnabled) {
                 delay(1500)
            }


            val result = calculateCustomRollUseCase(char, roll, null)
            _lastRoll.value = result
            _rollEvent.value = com.galeria.defensores.utils.Event(result)
            _isRolling.value = false
            
             if (char.tableId.isNotEmpty()) {
                com.galeria.defensores.data.TableRepository.addRollToHistory(char.tableId, result)
            }
        }
       }
    fun uploadCharacterAvatar(context: Context, uri: android.net.Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val charId = _character.value?.id ?: return
        android.util.Log.d("AvatarUpdate", "Starting Base64 update for charId: $charId")
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Compress Image (Max 300px, 70% Quality)
                val compressedBytes = com.galeria.defensores.utils.ImageUtils.compressImage(context, uri)
                
                if (compressedBytes != null) {
                    // 2. Encode to Base64
                    val base64String = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
                    // Prefix for Glide/Web usage
                    val dataUri = "data:image/jpeg;base64,$base64String"
                    
                    android.util.Log.d("AvatarUpdate", "Image encoded. Length: ${dataUri.length} chars")

                    // 3. Update Character Object Directly
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        updateAvatarUrl(dataUri)
                        onSuccess()
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("Falha ao processar imagem (compressão falhou).")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AvatarUpdate", "Error during avatar Base64 conversion", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Erro ao salvar imagem: ${e.message}")
                }
            }
        }
    }

    private fun persistCharacter() {
        val currentChar = _character.value ?: null
        if (currentChar != null) {
             viewModelScope.launch {
                 try {
                     com.galeria.defensores.data.CharacterRepository.saveCharacter(currentChar)
                 } catch (e: Exception) {
                     android.util.Log.e("ViewModel", "Error saving character", e)
                 }
             }
        }
    }

    private fun updateAvatarUrl(url: String) {
        val currentChar = _character.value ?: return
        currentChar.imageUrl = url
        _character.postValue(currentChar) // Update UI immediately
        persistCharacter() // Save big string to local file
    }
}
