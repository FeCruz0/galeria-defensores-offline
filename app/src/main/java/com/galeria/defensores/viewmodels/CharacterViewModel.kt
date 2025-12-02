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

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val _character = MutableLiveData<Character>()
    val character: LiveData<Character> = _character

    private val _isRolling = MutableLiveData<Boolean>()
    val isRolling: LiveData<Boolean> = _isRolling

    private val _lastRoll = MutableLiveData<RollResult>()
    val lastRoll: LiveData<RollResult> = _lastRoll

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
                        name = "Rolando..."
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
                name = type.displayName
            )
            _lastRoll.value = result
            _isRolling.value = false
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
}
