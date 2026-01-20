package com.galeria.defensores.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.models.Combatant
import com.galeria.defensores.models.CombatState
import com.galeria.defensores.models.CombatAction
import com.galeria.defensores.models.Character
import kotlinx.coroutines.launch

class CombatViewModel(application: Application) : AndroidViewModel(application) {

    private val _combatState = MutableLiveData<CombatState?>()
    val combatState: LiveData<CombatState?> = _combatState

    private var currentTableId: String? = null

    fun setTableId(tableId: String) {
        this.currentTableId = tableId
        // Listen to table updates to sync combat state
        viewModelScope.launch {
            TableRepository.getTableFlow(tableId).collect { table ->
                if (table != null) {
                    _combatState.value = table.combatState
                }
            }
        }
    }

    // --- Master Actions ---
    
    fun getAvailableCharacters(onResult: (List<Character>) -> Unit) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val chars = CharacterRepository.getCharacters(tableId)
            onResult(chars)
        }
    }

    fun startCombat(participants: List<Character>) {
        val tableId = currentTableId ?: return
        val combatants = participants.map { char ->
            Combatant(
                characterId = char.id,
                name = char.name,
                initiative = 0, // Will be rolled or set
                currentPv = char.currentPv,
                maxPv = char.getMaxPv(),
                avatarUrl = char.imageUrl,
                isNpc = false // Logic to determine if NPC? For now assume Characters are players usually
            )
        }.toMutableList()

        val newState = CombatState(
            isActive = true,
            round = 1,
            currentTurnIndex = 0,
            combatants = combatants,
            log = mutableListOf("Combat Started!")
        )
        
        updateCombatState(tableId, newState)
    }
    
    fun setInitiative(combatantId: String, value: Int) {
        val state = _combatState.value ?: return
        val tableId = currentTableId ?: return
        
        val newCombatants = state.combatants.map { 
            if (it.id == combatantId) it.copy(initiative = value) else it 
        }.sortedByDescending { it.initiative }.toMutableList()
        
        val newState = state.copy(combatants = newCombatants)
        updateCombatState(tableId, newState)
    }

    fun nextTurn() {
        val state = _combatState.value ?: return
        val tableId = currentTableId ?: return
        
        var nextIndex = state.currentTurnIndex + 1
        var round = state.round
        
        if (nextIndex >= state.combatants.size) {
            nextIndex = 0
            round += 1
        }
        
        // Skip Defeated - simple loop check (guard against infinite loop if all defeated)
        var checks = 0
        while (state.combatants.getOrNull(nextIndex)?.isDefeated == true && checks < state.combatants.size) {
            nextIndex = (nextIndex + 1) % state.combatants.size
            if (nextIndex == 0) round += 1
            checks++
        }

        val newState = state.copy(
            currentTurnIndex = nextIndex,
            round = round,
            pendingAction = null // Clear any pending actions on turn switch
        )
        updateCombatState(tableId, newState)
    }

    fun endCombat() {
        val tableId = currentTableId ?: return
        updateCombatState(tableId, null) // Clear state
    }

    // --- Player Actions ---

    fun attack(attackerId: String, targetId: String, rollTotal: Int, details: String) {
        val state = _combatState.value ?: return
        val tableId = currentTableId ?: return
        
        if (!state.isActive) return
        
        // Validation: Is it attacker's turn?
        val currentCombatant = state.combatants.getOrNull(state.currentTurnIndex)
        if (currentCombatant?.characterId != attackerId) {
             // Ideally we shouldn't reach here if UI is correct
             return 
        }

        val action = CombatAction(
            type = "ATTACK",
            attackerId = attackerId,
            targetId = targetId,
            attackRoll = rollTotal,
            attackDetails = details
        )
        
        val newLog = state.log.toMutableList()
        val attackerName = state.combatants.find { it.characterId == attackerId }?.name ?: "Unknown"
        val targetName = state.combatants.find { it.characterId == targetId }?.name ?: "Unknown"
        newLog.add("$attackerName attacks $targetName! ($details)")

        val newState = state.copy(
            pendingAction = action,
            log = newLog
        )
        updateCombatState(tableId, newState)
    }

    fun defend(defenderId: String, rollTotal: Int, details: String) {
         val state = _combatState.value ?: return
         val tableId = currentTableId ?: return
         val action = state.pendingAction ?: return
         
         if (action.type != "ATTACK" || (state.combatants.find { it.characterId == action.targetId }?.id != defenderId && action.targetId != defenderId)) {
             // ID check mismatch (using char ID vs combatant ID? Combatant stores char ID)
             // We should be consistent. Combatant has ID (random) and CharacterID.
             // Assume 'defenderId' passed here is CharacterID involved.
             return
         }

         // Resolve
         val damage = (action.attackRoll - rollTotal).coerceAtLeast(0)
         
         val newCombatants = state.combatants.map { combatant ->
             if (combatant.characterId == defenderId) {
                 val newPv = (combatant.currentPv - damage).coerceAtLeast(0)
                 val isDefeated = newPv <= 0
                 
                 // Update Repository (Real Character)
                 updateCharacterPv(combatant.characterId, newPv)
                 
                 combatant.copy(
                     currentPv = newPv,
                     isDefeated = isDefeated
                 )
             } else {
                 combatant
             }
         }.toMutableList()
         
         val defenderName = state.combatants.find { it.characterId == defenderId }?.name ?: "Unknown"
         val newLog = state.log.toMutableList()
         newLog.add("$defenderName defends! Result: $damage Damage taken.")
         if (newCombatants.find { it.characterId == defenderId }?.isDefeated == true) {
             newLog.add("$defenderName has been defeated!")
         }

         val newState = state.copy(
             combatants = newCombatants,
             pendingAction = null,
             log = newLog
         )
         updateCombatState(tableId, newState)
    }
    
    private fun updateCombatState(tableId: String, state: CombatState?) {
        viewModelScope.launch {
            val table = TableRepository.getTable(tableId)
            if (table != null) {
                table.combatState = state
                TableRepository.updateTable(table)
            }
        }
    }
    
    private fun updateCharacterPv(charId: String, newPv: Int) {
        viewModelScope.launch {
            val char = CharacterRepository.getCharacter(charId)
            if (char != null) {
                char.currentPv = newPv
                CharacterRepository.saveCharacter(char)
            }
        }
    }
}
