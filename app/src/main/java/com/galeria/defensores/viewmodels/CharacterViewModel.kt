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

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val _character = MutableLiveData<Character>()
    val character: LiveData<Character> = _character

    private val _isRolling = MutableLiveData<Boolean>()
    val isRolling: LiveData<Boolean> = _isRolling

    private val _lastRoll = MutableLiveData<RollResult>()
    val lastRoll: LiveData<RollResult> = _lastRoll

    private val _rollEvent = MutableLiveData<com.galeria.defensores.utils.Event<RollResult>>()
    val rollEvent: LiveData<com.galeria.defensores.utils.Event<RollResult>> = _rollEvent

    // Settings
    var isAnimationEnabled = true

    fun loadCharacter(id: String?, tableId: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("CharacterDebug", "Loading character: id=$id, tableId=$tableId")
            if (id != null) {
                val char = CharacterRepository.getCharacter(id)
                if (char != null) {
                    android.util.Log.d("CharacterDebug", "Character found: ${char.id}, owner=${char.ownerId}")
                    _character.value = char
                    return@launch
                } else {
                    android.util.Log.e("CharacterDebug", "Character NOT found for id=$id")
                }
            }
            // Default new character if ID not found or null
            android.util.Log.d("CharacterDebug", "Creating default character fallback")
            val currentUser = com.galeria.defensores.data.SessionManager.currentUser
            _character.value = Character(
                tableId = tableId ?: "",
                ownerId = currentUser?.id ?: "" // Try to set owner if falling back
            )
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
     * When resistencia changes, also update current PV and PM to the new maximums.
     */
    fun updateAttribute(attribute: String, value: Int) {
        val currentChar = _character.value ?: return
        val newValue = value.coerceIn(0, 99)
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

    fun updateName(name: String) {
        val currentChar = _character.value ?: return
        currentChar.name = name
        _character.value = currentChar
        saveCharacter()
    }

    fun updateHidden(isHidden: Boolean) {
        val currentChar = _character.value ?: return
        currentChar.isHidden = isHidden
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
        saveCharacter()
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
            calculateCustomRollResult(char, request.customRoll, diceValues)
        } else {
            // Standard Roll: Expects 1 die usually, but if diceValues has more, we take first or sum?
            // Standard Virtual Roll spawns 1 die based on our logic.
            val dieVal = if (diceValues.isNotEmpty()) diceValues[0] else 1
            
            val rollType = when(request.type) {
                com.galeria.defensores.models.RollRequestType.ATTACK_F -> com.galeria.defensores.models.RollType.ATTACK_F
                com.galeria.defensores.models.RollRequestType.ATTACK_PDF -> com.galeria.defensores.models.RollType.ATTACK_PDF
                com.galeria.defensores.models.RollRequestType.DEFENSE -> com.galeria.defensores.models.RollType.DEFENSE
                com.galeria.defensores.models.RollRequestType.INITIATIVE -> com.galeria.defensores.models.RollType.INITIATIVE
                else -> com.galeria.defensores.models.RollType.ATTRIBUTE
            }

            calculateStandardRollResult(
                char, 
                rollType, 
                request.bonus, 
                request.attributeValue, 
                request.skillValue, 
                dieVal
            )
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
                    displayAttr = "Força"
                    reqType = com.galeria.defensores.models.RollRequestType.ATTACK_F
                }
                RollType.ATTACK_PDF, RollType.SPECIAL_PDF -> {
                    attrVal = char.poderFogo
                    displayAttr = "Poder de Fogo"
                    reqType = com.galeria.defensores.models.RollRequestType.ATTACK_PDF
                }
                RollType.DEFENSE -> {
                    attrVal = char.armadura
                    displayAttr = "Armadura"
                    reqType = com.galeria.defensores.models.RollRequestType.DEFENSE
                }
                RollType.INITIATIVE -> {
                    attrVal = 0
                    displayAttr = "Iniciativa"
                    reqType = com.galeria.defensores.models.RollRequestType.INITIATIVE
                }
                RollType.ATTRIBUTE -> {
                     attrVal = 0
                     displayAttr = "Atributo"
                     reqType = com.galeria.defensores.models.RollRequestType.ATTACK_F // Fallback
                }
            }

            if (isVirtualRollEnabled) {
                // Intercept and send request
                val request = com.galeria.defensores.models.RollRequest(
                    type = reqType,
                    diceCount = 1, // Standard 1d6
                    bonus = bonus,
                    attributeValue = attrVal,
                    skillValue = char.habilidade,
                    attributeName = displayAttr
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
                        isHidden = char.isHidden,
                        characterId = char.id
                    )
                    _lastRoll.value = fakeResult
                    delay(100)
                }
            }

            val result = calculateStandardRollResult(char, type, bonus, attrVal, char.habilidade, null)
            
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
                
                val request = com.galeria.defensores.models.RollRequest(
                    type = com.galeria.defensores.models.RollRequestType.CUSTOM,
                    diceCount = diceCount,
                    bonus = 0, // Calculated in final result or UI? 
                    // For custom rolls, bonus is built-in to components/logic. 
                    // We pass 0 here and let finalize logic handle the full calc?
                    // actually, the UI might just return "dice results" and we re-run the calculation logic with those fixed results?
                    // OR we just trigger the animation phase and then run the standard logic?
                    // Let's stick to "Post Request -> UI does visual thing -> calls finalize -> finalized uses standard logic but maybe mocked dice?"
                    // Actually, `finalizeVirtualRoll` takes a `RollResult`.
                    // So the UI needs to CALCULATE the result for custom rolls?
                    // That implies duplicating the complex custom roll logic in the UI or Helper.
                    // BETTER APPROACH: 
                    // 1. VM calculates the result (RNG) *first*.
                    // 2. VM sends "Here is the result (e.g. 3, 5, 6), please animate this virtual roll".
                    // 3. UI animates.
                    // 4. UI callback "Done".
                    // 5. VM posts the result to chat.
                    // This "Deterministic" approach is easier for logic reuse.
                    // BUT the user wants to "interact" with the dice (physics).
                    // If physics determine the result, the VM cannot pre-calculate.
                    // So `DiceBoardView` MUST generate the random numbers based on physics/position.
                    // Challenge: How to feed those numbers back into the complex `rollCustom` logic?
                    // The `rollCustom` logic has `Random.nextInt`.
                    // Refactor `rollCustom` to accept *optional* pre-determined dice values?
                    attributeValue = 0,
                    skillValue = char.habilidade,
                    attributeName = roll.name,
                    customRoll = roll
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


            val result = calculateCustomRollResult(char, roll, null)
            _lastRoll.value = result
            _rollEvent.value = com.galeria.defensores.utils.Event(result)
            _isRolling.value = false
            
             if (char.tableId.isNotEmpty()) {
                com.galeria.defensores.data.TableRepository.addRollToHistory(char.tableId, result)
            }
        }
    }

    private fun calculateStandardRollResult(
        char: com.galeria.defensores.models.Character,
        type: RollType,
        bonus: Int,
        attrVal: Int,
        skillVal: Int,
        diceOverride: Int? = null
    ): RollResult {
        // Determine Die Result
        val die = diceOverride ?: (Random.nextInt(6) + 1)
        val isCritical = die == 6
        val effectiveAttr = if (isCritical) attrVal * 2 else attrVal
        val total = effectiveAttr + skillVal + die + bonus
        
        val displayAttr = when(type) {
             RollType.ATTACK_F -> "Força"
             RollType.ATTACK_PDF -> "PdF"
             RollType.DEFENSE -> "Armadura"
             RollType.INITIATIVE -> "Agilidade"
             else -> "Atributo"
        }

        val attrDetail = if (isCritical) "$displayAttr ($attrVal x2 CRÍTICO!)" else "$displayAttr ($attrVal)"
        val bonusDetail = if (bonus != 0) " + Bonus ($bonus)" else ""
        
        val details = "$attrDetail + Hab ($skillVal) + 1d6 ($die)$bonusDetail = $total"

        return RollResult(
            total = total,
            die = die,
            attributeUsed = displayAttr,
            attributeValue = attrVal,
            skillValue = skillVal,
            bonus = bonus,
            isCritical = isCritical,
            timestamp = System.currentTimeMillis(),
            name = char.name,
            characterId = char.id,
            isHidden = char.isHidden,
            details = details,
            diceResults = listOf(die)
        )
    }

    private fun calculateCustomRollResult(
        char: com.galeria.defensores.models.Character, 
        roll: com.galeria.defensores.models.CustomRoll, 
        diceOverride: List<Int>? = null
    ): RollResult {
        var totalSum = 0
        val parts = mutableListOf<String>()
        var isCriticalSummary = false
        var totalCrits = 0
        val allDice = mutableListOf<Int>()
        
        var overrideIndex = 0

        // 1. Process Dice Components
        roll.components.forEach { comp ->
            var compTotal = 0
            val rolls = mutableListOf<Int>()
            
            repeat(comp.count) {
                val die = if (diceOverride != null && overrideIndex < diceOverride.size) {
                    diceOverride[overrideIndex++]
                } else {
                    Random.nextInt(comp.faces) + 1
                }
                
                val isCrit = comp.canCrit && (
                    (comp.critRangeStart != null && die >= comp.critRangeStart!!) || 
                    (comp.critRangeStart == null && die == comp.faces)
                )
                
                if (isCrit) {
                    totalCrits++
                    isCriticalSummary = true
                }
                
                rolls.add(die)
                allDice.add(die)
                compTotal += die
            }
            
            val finalCompTotal = if (comp.isNegative) -compTotal else compTotal
            totalSum += finalCompTotal + comp.bonus
            
            // Format: "2d6[3,5]+2" or "-1d6[4]"
            val signPrefix = if (comp.isNegative) "- " else (if (parts.isNotEmpty()) "+ " else "")
            val diceStr = "${comp.count}d${comp.faces}[${rolls.joinToString(",")}]"
            val bonusStr = if (comp.bonus != 0) {
                 if (comp.bonus > 0) "+${comp.bonus}" else "${comp.bonus}"
            } else ""
            
            parts.add("$signPrefix$diceStr$bonusStr")
        }

        // 2. Resolve Attributes & Crits
        fun getAttrValue(attrName: String): Int {
            return when(attrName) {
                "forca" -> char.forca
                "habilidade" -> char.habilidade
                "resistencia" -> char.resistencia
                "armadura" -> char.armadura
                "poderFogo" -> char.poderFogo
                else -> 0
            }
        }
        
        val primaryVal = getAttrValue(roll.primaryAttribute)
        val secondaryVal = getAttrValue(roll.secondaryAttribute)
        
        // Calculate Multiplier
        var critMultiplier = 1
        if (isCriticalSummary) {
             critMultiplier = if (roll.accumulateCrit) 1 + totalCrits else 2
        }
        
        val finalPrimary = primaryVal * critMultiplier
        totalSum += finalPrimary + secondaryVal + roll.globalModifier

        // 3. Append Attributes formatted
        if (roll.primaryAttribute != "none" && primaryVal != 0) {
            val pName = roll.primaryAttribute.take(3).replaceFirstChar { it.uppercase() }
            val critInfo = if (critMultiplier > 1) " x$critMultiplier Crit" else ""
            val prefix = if (parts.isNotEmpty()) "+ " else ""
            parts.add("$prefix$pName($primaryVal$critInfo)")
        }
        
        if (roll.secondaryAttribute != "none" && secondaryVal != 0) {
            val sName = roll.secondaryAttribute.take(3).replaceFirstChar { it.uppercase() }
            val prefix = if (parts.isNotEmpty()) "+ " else ""
            parts.add("$prefix$sName($secondaryVal)")
        }
        
        // 4. Global Modifier
        if (roll.globalModifier != 0) {
             val prefix = if (roll.globalModifier > 0) (if (parts.isNotEmpty()) "+ " else "") else "- "
             parts.add("$prefix${kotlin.math.abs(roll.globalModifier)}")
        }
        
        // 5. Build Final String
        val finalString = parts.joinToString(" ").replace("  ", " ").trim()

        return RollResult(
            total = totalSum,
            die = 0, 
            attributeUsed = "Custom", // Or keep blank to force usage of details?
            attributeValue = 0,
            skillValue = 0,
            bonus = roll.globalModifier,
            isCritical = isCriticalSummary,
            timestamp = System.currentTimeMillis(),
            name = "${char.name} - ${roll.name}",
            isHidden = char.isHidden,
            characterId = char.id,
            details = finalString,
            diceResults = allDice
        )
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

    private fun updateAvatarUrl(url: String) {
        val currentChar = _character.value ?: return
        currentChar.imageUrl = url
        _character.postValue(currentChar) // Update UI immediately
        saveCharacter() // Save big string to Firestore
    }
}
