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

    fun rollDice(type: RollType) {
        val char = _character.value ?: return
        viewModelScope.launch {
            _isRolling.value = true
            var bonus = 0
            var isSpecial = false

            if (type == RollType.SPECIAL_F || type == RollType.SPECIAL_PDF) {
                if (char.currentPm < 1) {
                    // Not enough PM – could signal an error to UI
                    _isRolling.value = false
                    return@launch
                }
                char.currentPm -= 1
                bonus = 2
                isSpecial = true
                _character.value = char // Update UI for PM change
            }

            var attrVal = 0
            var displayAttr = ""
            when (type) {
                RollType.ATTACK_F, RollType.SPECIAL_F -> {
                    attrVal = char.forca
                    displayAttr = "Força"
                }
                RollType.ATTACK_PDF, RollType.SPECIAL_PDF -> {
                    attrVal = char.poderFogo
                    displayAttr = "Poder de Fogo"
                }
                RollType.DEFENSE -> {
                    attrVal = char.armadura
                    displayAttr = "Armadura"
                }
            }

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

            val die = Random.nextInt(6) + 1
            val isCritical = die == 6
            val effectiveAttr = if (isCritical) attrVal * 2 else attrVal
            val total = effectiveAttr + char.habilidade + die + bonus
            val result = RollResult(
                total = total,
                die = die,
                attributeUsed = displayAttr,
                attributeValue = attrVal,
                skillValue = char.habilidade,
                bonus = bonus,
                isCritical = isCritical,
                timestamp = System.currentTimeMillis(),
                name = if (isSpecial) type.displayName else "${char.name} - ${type.displayName}",
                isHidden = char.isHidden,
                characterId = char.id
            )
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

    // --- Avatar Logic ---
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
