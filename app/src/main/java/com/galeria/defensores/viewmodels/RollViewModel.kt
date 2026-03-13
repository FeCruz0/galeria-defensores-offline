package com.galeria.defensores.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.models.RollType
import com.galeria.defensores.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

/**
 * Handles all dice rolling logic: standard rolls, custom rolls, virtual (animated) rolls,
 * roll history, and broadcasting visual rolls to the table.
 */
@HiltViewModel
class RollViewModel @Inject constructor(
    application: Application,
    private val tableRepository: TableRepository,
    private val sharedCharacterState: SharedCharacterState,
    private val calculateStandardRollUseCase: com.galeria.defensores.domain.usecases.CalculateStandardRollUseCase,
    private val calculateCustomRollUseCase: com.galeria.defensores.domain.usecases.CalculateCustomRollUseCase
) : AndroidViewModel(application) {

    private val _isRolling = MutableLiveData<Boolean>()
    val isRolling: LiveData<Boolean> = _isRolling

    private val _lastRoll = MutableLiveData<RollResult>()
    val lastRoll: LiveData<RollResult> = _lastRoll

    private val _rollEvent = MutableLiveData<Event<RollResult>>()
    val rollEvent: LiveData<Event<RollResult>> = _rollEvent

    private val _virtualRollRequest = MutableLiveData<Event<com.galeria.defensores.models.RollRequest>>()
    val virtualRollRequest: LiveData<Event<com.galeria.defensores.models.RollRequest>> = _virtualRollRequest

    var isVirtualRollEnabled = true

    private fun getAttributeName(key: String): String {
        // We get the ruleSystem from shared state's character's table or default
        // For now, use a display-friendly fallback matching CharacterViewModel logic
        return key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }

    fun finalizeVirtualRoll(diceValues: List<Int>) {
        val request = _virtualRollRequest.value?.peekContent() ?: return
        val char = sharedCharacterState.character.value ?: return

        val result = if (request.type == com.galeria.defensores.models.RollRequestType.CUSTOM && request.customRoll != null) {
            calculateCustomRollUseCase(char, request.customRoll, request.diceOverride ?: diceValues)
        } else {
            val dieVal = (request.diceOverride ?: diceValues).firstOrNull() ?: 1
            val rollType = when (request.type) {
                com.galeria.defensores.models.RollRequestType.ATTACK_F   -> RollType.ATTACK_F
                com.galeria.defensores.models.RollRequestType.ATTACK_PDF  -> RollType.ATTACK_PDF
                com.galeria.defensores.models.RollRequestType.DEFENSE     -> RollType.DEFENSE
                com.galeria.defensores.models.RollRequestType.INITIATIVE  -> RollType.INITIATIVE
                else                                                       -> RollType.ATTRIBUTE
            }
            calculateStandardRollUseCase(char, rollType, request.bonus, request.attributeValue, request.skillValue, dieVal) { getAttributeName(it) }
        }

        _lastRoll.value = result
        _rollEvent.value = Event(result)
        saveRollToHistory(result)
    }

    fun rollDice(type: RollType) {
        val char = sharedCharacterState.character.value ?: return

        viewModelScope.launch {
            _isRolling.value = true
            var bonus = 0

            // PM deduction for special attacks
            if (type == RollType.SPECIAL_F || type == RollType.SPECIAL_PDF) {
                if (char.currentPm < 1) { _isRolling.value = false; return@launch }
                char.currentPm -= 1
                bonus = 2
                sharedCharacterState.update(char)
            }

            var attrVal = 0
            var displayAttr = ""
            var reqType = com.galeria.defensores.models.RollRequestType.ATTACK_F

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
                    displayAttr = "Iniciativa"
                    reqType = com.galeria.defensores.models.RollRequestType.INITIATIVE
                }
                RollType.ATTRIBUTE -> {
                    attrVal = 0
                    displayAttr = "Atributo"
                    reqType = com.galeria.defensores.models.RollRequestType.ATTRIBUTE
                }
            }

            if (isVirtualRollEnabled) {
                val diceValues = listOf(Random.nextInt(6) + 1)
                val canCrit = type == RollType.ATTACK_F || type == RollType.ATTACK_PDF ||
                        type == RollType.SPECIAL_F || type == RollType.SPECIAL_PDF || type == RollType.DEFENSE
                val isNegativeRoll = bonus < 0
                val diceProps = listOf(com.galeria.defensores.models.DieProperty(canCrit, isNegativeRoll, 6))

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
                    tableRepository.broadcastVisualRoll(char.tableId, visualRoll)
                }

                _virtualRollRequest.value = Event(
                    com.galeria.defensores.models.RollRequest(
                        type = reqType,
                        diceCount = 1,
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
                )
                _isRolling.value = false
                return@launch
            }

            // Non-virtual fallback with optional animation
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val animationEnabled = prefs.getBoolean("animation_enabled", true)
            if (animationEnabled) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 2500L) {
                    val fakeDie = Random.nextInt(6) + 1
                    _lastRoll.value = RollResult(
                        total = attrVal + char.habilidade + fakeDie + bonus,
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
                    delay(100)
                }
            }

            val result = calculateStandardRollUseCase(char, type, bonus, attrVal, char.habilidade, null) { getAttributeName(it) }
            _lastRoll.value = result
            _rollEvent.value = Event(result)
            _isRolling.value = false
            saveRollToHistory(result)
        }
    }

    fun rollCustom(roll: CustomRoll) {
        val char = sharedCharacterState.character.value ?: return

        viewModelScope.launch {
            _isRolling.value = true

            if (isVirtualRollEnabled) {
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

                if (char.tableId.isNotEmpty()) {
                    val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id ?: char.ownerId
                    tableRepository.broadcastVisualRoll(
                        char.tableId,
                        com.galeria.defensores.models.VisualRoll(
                            senderId = currentUserId,
                            senderName = char.name,
                            diceCount = diceValues.size,
                            diceValues = diceValues,
                            diceProperties = diceProps,
                            canCrit = canCrit,
                            isNegative = isNegativeRoll,
                            critRangeStart = minCritRange
                        )
                    )
                }

                _virtualRollRequest.value = Event(
                    com.galeria.defensores.models.RollRequest(
                        type = com.galeria.defensores.models.RollRequestType.CUSTOM,
                        diceCount = roll.components.sumOf { it.count },
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
                )
                _isRolling.value = false
                return@launch
            }

            val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("animation_enabled", true)) delay(1500)

            val result = calculateCustomRollUseCase(char, roll, null)
            _lastRoll.value = result
            _rollEvent.value = Event(result)
            _isRolling.value = false
            saveRollToHistory(result)
        }
    }

    // --- Custom Roll definitions (stored on Character) ---
    // These mutate the character through SharedCharacterState

    fun addCustomRoll(roll: CustomRoll) {
        val char = sharedCharacterState.character.value ?: return
        char.customRolls = (char.customRolls.toMutableList().also { it.add(roll) })
        sharedCharacterState.update(char)
    }

    fun updateCustomRoll(roll: CustomRoll) {
        val char = sharedCharacterState.character.value ?: return
        val list = char.customRolls.toMutableList()
        val i = list.indexOfFirst { it.id == roll.id }
        if (i != -1) { list[i] = roll; char.customRolls = list; sharedCharacterState.update(char) }
    }

    fun removeCustomRoll(roll: CustomRoll) {
        val char = sharedCharacterState.character.value ?: return
        char.customRolls = char.customRolls.toMutableList().also { it.removeAll { r -> r.id == roll.id } }
        sharedCharacterState.update(char)
    }

    private fun saveRollToHistory(result: RollResult) {
        val char = sharedCharacterState.character.value ?: return
        if (char.tableId.isNotEmpty()) {
            viewModelScope.launch {
                tableRepository.addRollToHistory(char.tableId, result)
            }
        }
    }
}
