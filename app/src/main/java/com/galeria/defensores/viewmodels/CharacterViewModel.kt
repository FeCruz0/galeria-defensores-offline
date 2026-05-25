package com.galeria.defensores.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.domain.usecases.LoadCharacterUseCase
import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.InventoryItem
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.Spell
import com.galeria.defensores.models.UniqueAdvantage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Core character ViewModel: owns identity, load/save/delete, attributes, status,
 * resources, and all character traits (advantages, skills, spells, inventory, etc.).
 *
 * Roll logic → RollViewModel
 * Rule-system editing → RuleSystemViewModel
 */
sealed class CharacterUiState {
    object Loading : CharacterUiState()
    data class Success(val character: Character, val ruleSystem: com.galeria.defensores.models.RuleSystem) : CharacterUiState()
    data class Error(val message: String) : CharacterUiState()
}

@HiltViewModel
class CharacterViewModel @Inject constructor(
    application: Application,
    private val characterRepository: CharacterRepository,
    private val sharedCharacterState: SharedCharacterState,
    private val getResourceMaxUseCase: GetResourceMaxUseCase,
    private val loadCharacterUseCase: LoadCharacterUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CharacterUiState>(CharacterUiState.Loading)
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    private val currentCharacter: Character?
        get() = (_uiState.value as? CharacterUiState.Success)?.character

    private val currentRuleSystem: com.galeria.defensores.models.RuleSystem
        get() = (_uiState.value as? CharacterUiState.Success)?.ruleSystem ?: com.galeria.defensores.models.RuleSystem()

    var isAnimationEnabled = true
    private val saveMutex = Mutex()

    // ────────────────────────────────────────────────────────────────────────
    // Load / Save / Delete
    // ────────────────────────────────────────────────────────────────────────

    fun loadCharacter(id: String?, tableId: String? = null) {
        // Guard: if the ViewModel already holds the correct character, do not reload.
        // This prevents the sheet from switching when the user types a name matching another character,
        // because activityViewModels() is shared and onViewCreated calls loadCharacter on every resume.
        val alreadyLoadedId = (uiState.value as? CharacterUiState.Success)?.character?.id
        if (id != null && alreadyLoadedId == id) {
            android.util.Log.d("CharacterDebug", "Skipping reload — character $id already active.")
            return
        }
        
        // Clear previous state to prevent old character data from flashing in the UI
        _uiState.value = CharacterUiState.Loading
        
        viewModelScope.launch {
            android.util.Log.d("CharacterDebug", "Loading character: id=$id, tableId=$tableId")
            loadCharacterUseCase(id, tableId).collect { result ->
                when (result) {
                    is com.galeria.defensores.domain.usecases.CharacterResult.Success -> {
                        val char = result.character
                        val sys = result.ruleSystem
                        _uiState.value = CharacterUiState.Success(char, sys)
                        sharedCharacterState.update(char)
                    }
                    is com.galeria.defensores.domain.usecases.CharacterResult.Error -> {
                        android.util.Log.e("CharacterDebug", "Error loading character", result.throwable)
                        val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                        val fallback = Character(tableId = tableId ?: "", ownerId = currentUser?.id ?: "")
                        _uiState.value = CharacterUiState.Error("Falha ao carregar ficha.")
                        
                        sharedCharacterState.update(fallback)
                    }
                }
            }
        }
    }

    fun saveCharacter() {
        currentCharacter?.let { char ->
            viewModelScope.launch {
                saveMutex.withLock {
                    try {
                        characterRepository.saveCharacter(char)
                    } catch (e: Exception) {
                        android.util.Log.e("CharacterDebug", "Error saving character", e)
                    }
                }
            }
        }
    }

    fun deleteCharacter(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val charId = currentCharacter?.id ?: return
        viewModelScope.launch {
            if (characterRepository.deleteCharacter(charId)) {
                _uiState.value = CharacterUiState.Loading // Ou limpa estado.
                sharedCharacterState.update(null)
                onSuccess()
            } else {
                onError("Erro ao excluir. Verifique sua conexão.")
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Attributes & Status
    // ────────────────────────────────────────────────────────────────────────

    fun updateAttribute(attribute: String, value: Int) {
        val char = currentCharacter ?: return
        val newValue = value.coerceIn(0, 99)
        val updatedChar = char.deepCopy().also { it.attributeValues[attribute] = newValue }
        when (attribute) {
            "forca"      -> updatedChar.forca = newValue
            "habilidade" -> updatedChar.habilidade = newValue
            "resistencia" -> {
                updatedChar.resistencia = newValue
            }
            "armadura"   -> updatedChar.armadura = newValue
            "poderFogo"  -> updatedChar.poderFogo = newValue
        }
        publish(updatedChar)
        saveCharacter()
    }

    fun updateStatus(type: String, delta: Int) {
        mutate { char ->
            when (type) {
                "pv" -> char.currentPv = (char.currentPv + delta).coerceIn(0, char.getMaxPv())
                "pm" -> char.currentPm = (char.currentPm + delta).coerceIn(0, char.getMaxPm())
            }
        }
    }

    fun setStatus(type: String, value: Int) {
        mutate { char ->
            when (type) {
                "pv" -> char.currentPv = value.coerceIn(0, char.getMaxPv())
                "pm" -> char.currentPm = value.coerceIn(0, char.getMaxPm())
            }
        }
    }

    fun updateResource(key: String, delta: Int) {
        if (key.equals("pv", ignoreCase = true)) { updateStatus("pv", delta); return }
        if (key.equals("pm", ignoreCase = true)) { updateStatus("pm", delta); return }
        mutate { char ->
            val current = char.resourceValues[key] ?: 0
            char.resourceValues[key] = (current + delta).coerceIn(0, 9999)
        }
    }

    fun calculateResourceMax(res: ResourceDefinition, char: Character, ruleSystem: com.galeria.defensores.models.RuleSystem): Int =
        getResourceMaxUseCase(res, char, ruleSystem)

    // ────────────────────────────────────────────────────────────────────────
    // Basic fields
    // ────────────────────────────────────────────────────────────────────────

    fun updateName(name: String)  { mutate { it.name = name } }
    fun updateScale(scale: Int)   { mutate { it.scale = scale } }
    fun updateNotes(html: String) { mutate { it.anotacoes = html } }

    fun updateSavedPoints(points: Int) { mutate { it.savedPoints = points.coerceAtLeast(0) } }

    fun updateExperience(xp: Int) {
        mutate { char ->
            var newXp = xp.coerceAtLeast(0)
            var newSaved = char.savedPoints
            if (newXp >= 10) {
                newSaved += newXp / 10
                newXp %= 10
            }
            char.experience = newXp
            char.savedPoints = newSaved
        }
    }

    fun updateDamageType(type: String, isPdf: Boolean) {
        mutate { char ->
            if (isPdf) char.damageTypePdf = type else char.damageTypeForca = type
        }
    }

    fun setUniqueAdvantage(ua: UniqueAdvantage?) {
        mutate { it.uniqueAdvantage = ua }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Advantages / Disadvantages
    // ────────────────────────────────────────────────────────────────────────

    fun addAdvantage(advantage: AdvantageItem) {
        mutate { it.vantagens = it.vantagens.toMutableList().also { l -> l.add(advantage.copy(id = java.util.UUID.randomUUID().toString())) } }
    }
    fun updateAdvantage(advantage: AdvantageItem) {
        mutate { char ->
            val list = char.vantagens.toMutableList()
            val idx = list.indexOfFirst { it.id == advantage.id }
            if (idx != -1) {
                list[idx] = advantage
                char.vantagens = list
            }
        }
    }
    fun removeAdvantage(advantage: AdvantageItem) {
        mutate { it.vantagens = it.vantagens.filter { a -> a.id != advantage.id }.toMutableList() }
    }

    fun addDisadvantage(dis: AdvantageItem) {
        mutate { it.desvantagens = it.desvantagens.toMutableList().also { l -> l.add(dis.copy(id = java.util.UUID.randomUUID().toString())) } }
    }
    fun updateDisadvantage(dis: AdvantageItem) {
        mutate { char ->
            val list = char.desvantagens.toMutableList()
            val idx = list.indexOfFirst { it.id == dis.id }
            if (idx != -1) {
                list[idx] = dis
                char.desvantagens = list
            }
        }
    }
    fun removeDisadvantage(dis: AdvantageItem) {
        mutate { it.desvantagens = it.desvantagens.filter { a -> a.id != dis.id }.toMutableList() }
    }

    fun addSkill(skill: AdvantageItem) {
        mutate { it.pericias = it.pericias.toMutableList().also { l -> l.add(skill.copy(id = java.util.UUID.randomUUID().toString())) } }
    }
    fun updateSkill(skill: AdvantageItem) {
        mutate { char ->
            val list = char.pericias.toMutableList()
            val idx = list.indexOfFirst { it.id == skill.id }
            if (idx != -1) {
                list[idx] = skill
                char.pericias = list
            }
        }
    }
    fun removeSkill(skill: AdvantageItem) {
        mutate { it.pericias = it.pericias.filter { s -> s.id != skill.id }.toMutableList() }
    }

    fun addSpecializations(specs: List<AdvantageItem>) {
        mutate { it.especializacoes = it.especializacoes.toMutableList().also { l -> l.addAll(specs.map { it.copy(id = java.util.UUID.randomUUID().toString()) }) } }
    }
    fun updateSpecialization(spec: AdvantageItem) {
        mutate { char ->
            val list = char.especializacoes.toMutableList()
            val idx = list.indexOfFirst { it.id == spec.id }
            if (idx != -1) {
                list[idx] = spec
                char.especializacoes = list
            }
        }
    }
    fun removeSpecialization(spec: AdvantageItem) {
        mutate { it.especializacoes = it.especializacoes.filter { s -> s.id != spec.id }.toMutableList() }
    }

    fun addInventoryItem(item: InventoryItem) {
        mutate { it.inventario = it.inventario.toMutableList().also { l -> l.add(item.copy(id = java.util.UUID.randomUUID().toString())) } }
    }
    fun updateInventoryItem(item: InventoryItem) {
        mutate { char ->
            val list = char.inventario.toMutableList()
            val idx = list.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                list[idx] = item
                char.inventario = list
            }
        }
    }
    fun removeInventoryItem(item: InventoryItem) {
        mutate { it.inventario = it.inventario.filter { i -> i.id != item.id }.toMutableList() }
    }

    fun adjustInventoryQuantity(item: InventoryItem, delta: Int) {
        val qty = item.quantity.toIntOrNull() ?: return
        updateInventoryItem(item.copy(quantity = (qty + delta).coerceAtLeast(0).toString()))
    }

    fun addSpell(spell: Spell) {
        mutate { it.magias = it.magias.toMutableList().also { l -> l.add(spell.copy(id = java.util.UUID.randomUUID().toString())) } }
    }
    fun updateSpell(spell: Spell) {
        mutate { char ->
            val list = char.magias.toMutableList()
            val idx = list.indexOfFirst { it.id == spell.id }
            if (idx != -1) {
                list[idx] = spell
                char.magias = list
            }
        }
    }
    fun removeSpell(spell: Spell) {
        mutate { it.magias = it.magias.filter { s -> s.id != spell.id }.toMutableList() }
    }

    fun addCustomRoll(roll: CustomRoll) {
        mutate { it.customRolls = it.customRolls.toMutableList().also { l -> l.add(roll) } }
    }
    fun updateCustomRoll(roll: CustomRoll) {
        mutate { char ->
            val list = char.customRolls.toMutableList()
            val idx = list.indexOfFirst { it.id == roll.id }
            if (idx != -1) {
                list[idx] = roll
                char.customRolls = list
            }
        }
    }
    fun removeCustomRoll(roll: CustomRoll) {
        mutate { it.customRolls = it.customRolls.filter { r -> r.id != roll.id }.toMutableList() }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Avatar
    // ────────────────────────────────────────────────────────────────────────

    fun uploadCharacterAvatar(context: Context, uri: android.net.Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bytes = com.galeria.defensores.utils.ImageUtils.compressImage(context, uri)
                if (bytes != null) {
                    val dataUri = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        mutate { it.imageUrl = dataUri }
                        onSuccess()
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError("Falha ao processar imagem.") }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onError("Erro: ${e.message}") }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Apply a mutation, then publish to LiveData and SharedCharacterState. */
    private fun mutate(block: (Character) -> Unit) {
        val char = currentCharacter ?: return
        val newChar = char.deepCopy()
        block(newChar)
        publish(newChar)
        saveCharacter()
    }

    /** Emit to LiveData and bridge. */
    private fun publish(char: Character) {
        val currentState = _uiState.value
        if (currentState is CharacterUiState.Success) {
            _uiState.value = currentState.copy(character = char)
        }
        sharedCharacterState.update(char)
    }

    fun updateRuleSystem(system: com.galeria.defensores.models.RuleSystem) {
        val currentState = _uiState.value
        if (currentState is CharacterUiState.Success) {
            _uiState.value = currentState.copy(ruleSystem = system)
        }
    }
}
