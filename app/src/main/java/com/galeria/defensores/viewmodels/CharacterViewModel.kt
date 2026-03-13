package com.galeria.defensores.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * Core character ViewModel: owns identity, load/save/delete, attributes, status,
 * resources, and all character traits (advantages, skills, spells, inventory, etc.).
 *
 * Roll logic → RollViewModel
 * Rule-system editing → RuleSystemViewModel
 */
@HiltViewModel
class CharacterViewModel @Inject constructor(
    application: Application,
    private val characterRepository: CharacterRepository,
    private val sharedCharacterState: SharedCharacterState,
    private val getResourceMaxUseCase: GetResourceMaxUseCase,
    private val loadCharacterUseCase: LoadCharacterUseCase
) : AndroidViewModel(application) {

    private val _character = MutableLiveData<Character>()
    val character: LiveData<Character> = _character

    var isAnimationEnabled = true

    // ────────────────────────────────────────────────────────────────────────
    // Load / Save / Delete
    // ────────────────────────────────────────────────────────────────────────

    fun loadCharacter(id: String?, tableId: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("CharacterDebug", "Loading character: id=$id, tableId=$tableId")
            when (val result = loadCharacterUseCase(id, tableId)) {
                is com.galeria.defensores.domain.usecases.CharacterResult.Success -> {
                    val char = result.character
                    _character.value = char
                    sharedCharacterState.update(char)
                }
                is com.galeria.defensores.domain.usecases.CharacterResult.Error -> {
                    android.util.Log.e("CharacterDebug", "Error loading character", result.throwable)
                    val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                    val fallback = Character(tableId = tableId ?: "", ownerId = currentUser?.id ?: "")
                    _character.value = fallback
                    sharedCharacterState.update(fallback)
                }
            }
        }
    }

    fun saveCharacter() {
        _character.value?.let { char ->
            viewModelScope.launch { characterRepository.saveCharacter(char) }
        }
    }

    fun deleteCharacter(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val charId = _character.value?.id ?: return
        viewModelScope.launch {
            if (characterRepository.deleteCharacter(charId)) {
                _character.value = null
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
        val char = _character.value ?: return
        val newValue = value.coerceIn(0, 99)
        char.attributeValues[attribute] = newValue
        when (attribute) {
            "forca"      -> char.forca = newValue
            "habilidade" -> char.habilidade = newValue
            "resistencia" -> {
                char.resistencia = newValue
                char.currentPv = char.getMaxPv()
                char.currentPm = char.getMaxPm()
            }
            "armadura"   -> char.armadura = newValue
            "poderFogo"  -> char.poderFogo = newValue
        }
        publish(char)
        saveCharacter()
    }

    fun updateStatus(type: String, delta: Int) {
        val char = _character.value ?: return
        when (type) {
            "pv" -> char.currentPv = (char.currentPv + delta).coerceIn(0, char.getMaxPv())
            "pm" -> char.currentPm = (char.currentPm + delta).coerceIn(0, char.getMaxPm())
        }
        publish(char)
        saveCharacter()
    }

    fun setStatus(type: String, value: Int) {
        val char = _character.value ?: return
        when (type) {
            "pv" -> char.currentPv = value.coerceIn(0, char.getMaxPv())
            "pm" -> char.currentPm = value.coerceIn(0, char.getMaxPm())
        }
        publish(char)
        saveCharacter()
    }

    fun updateResource(key: String, delta: Int) {
        if (key.equals("pv", ignoreCase = true)) { updateStatus("pv", delta); return }
        if (key.equals("pm", ignoreCase = true)) { updateStatus("pm", delta); return }
        val char = _character.value ?: return
        // For custom resources we need ruleSystem — access via sharedState's consumer or pass in
        val current = char.resourceValues[key] ?: 999
        char.resourceValues[key] = (current + delta).coerceIn(0, 999)
        publish(char)
        saveCharacter()
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
        val char = _character.value ?: return
        var newXp = xp.coerceAtLeast(0)
        var newSaved = char.savedPoints
        if (newXp >= 10) { newSaved += newXp / 10; newXp %= 10 }
        if (newXp != char.experience || newSaved != char.savedPoints) {
            char.experience = newXp
            char.savedPoints = newSaved
            publish(char); saveCharacter()
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

    fun addAdvantage(advantage: AdvantageItem)       { mutate { it.vantagens    = it.vantagens.toMutableList().also { l -> l.add(advantage) } } }
    fun updateAdvantage(advantage: AdvantageItem)    { mutateList({ it.vantagens }, { it.id == advantage.id }, advantage) { char, list -> char.vantagens = list } }
    fun removeAdvantage(advantage: AdvantageItem)    { mutate { it.vantagens = it.vantagens.filter { a -> a.id != advantage.id }.toMutableList() } }

    fun addDisadvantage(dis: AdvantageItem)          { mutate { it.desvantagens = it.desvantagens.toMutableList().also { l -> l.add(dis) } } }
    fun updateDisadvantage(dis: AdvantageItem)       { mutateList({ it.desvantagens }, { it.id == dis.id }, dis) { char, list -> char.desvantagens = list } }
    fun removeDisadvantage(dis: AdvantageItem)       { mutate { it.desvantagens = it.desvantagens.filter { a -> a.id != dis.id }.toMutableList() } }

    fun addSkill(skill: AdvantageItem)               { mutate { it.pericias = it.pericias.toMutableList().also { l -> l.add(skill) } } }
    fun updateSkill(skill: AdvantageItem)            { mutateList({ it.pericias }, { it.id == skill.id }, skill) { char, list -> char.pericias = list } }
    fun removeSkill(skill: AdvantageItem)            { mutate { it.pericias = it.pericias.filter { s -> s.id != skill.id }.toMutableList() } }

    fun addSpecializations(specs: List<AdvantageItem>) { mutate { it.especializacoes = it.especializacoes.toMutableList().also { l -> l.addAll(specs) } } }
    fun updateSpecialization(spec: AdvantageItem)      { mutateList({ it.especializacoes }, { it.id == spec.id }, spec) { char, list -> char.especializacoes = list } }
    fun removeSpecialization(spec: AdvantageItem)      { mutate { it.especializacoes = it.especializacoes.filter { s -> s.id != spec.id }.toMutableList() } }

    // ────────────────────────────────────────────────────────────────────────
    // Inventory
    // ────────────────────────────────────────────────────────────────────────

    fun addInventoryItem(item: InventoryItem)         { mutate { it.inventario = it.inventario.toMutableList().also { l -> l.add(item) } } }
    fun updateInventoryItem(item: InventoryItem)      { mutateList({ it.inventario }, { it.id == item.id }, item) { char, list -> char.inventario = list } }
    fun removeInventoryItem(item: InventoryItem)      { mutate { it.inventario = it.inventario.filter { i -> i.id != item.id }.toMutableList() } }
    fun adjustInventoryQuantity(item: InventoryItem, delta: Int) {
        val qty = item.quantity.toIntOrNull() ?: return
        updateInventoryItem(item.copy(quantity = (qty + delta).coerceAtLeast(0).toString()))
    }

    // ────────────────────────────────────────────────────────────────────────
    // Spells
    // ────────────────────────────────────────────────────────────────────────

    fun addSpell(spell: Spell)    { mutate { it.magias = it.magias.toMutableList().also { l -> l.add(spell) } } }
    fun updateSpell(spell: Spell) { mutateList({ it.magias }, { it.id == spell.id }, spell) { char, list -> char.magias = list } }
    fun removeSpell(spell: Spell) { mutate { it.magias = it.magias.filter { s -> s.id != spell.id }.toMutableList() } }

    // ────────────────────────────────────────────────────────────────────────
    // Custom Rolls (CRUD only — actual rolling is in RollViewModel)
    // ────────────────────────────────────────────────────────────────────────

    fun addCustomRoll(roll: CustomRoll)    { mutate { it.customRolls = it.customRolls.toMutableList().also { l -> l.add(roll) } } }
    fun updateCustomRoll(roll: CustomRoll) { mutateList({ it.customRolls }, { it.id == roll.id }, roll) { char, list -> char.customRolls = list } }
    fun removeCustomRoll(roll: CustomRoll) { mutate { it.customRolls = it.customRolls.filter { r -> r.id != roll.id }.toMutableList() } }

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
        val char = _character.value ?: return
        block(char)
        publish(char)
        saveCharacter()
    }

    /** Update a list item in-place. */
    private fun <T> mutateList(
        getList: (Character) -> MutableList<T>,
        predicate: (T) -> Boolean,
        replacement: T,
        setList: (Character, MutableList<T>) -> Unit
    ) {
        val char = _character.value ?: return
        val list = getList(char).toMutableList()
        val idx = list.indexOfFirst(predicate)
        if (idx != -1) { list[idx] = replacement; setList(char, list); publish(char); saveCharacter() }
    }

    /** Emit to LiveData and bridge. */
    private fun publish(char: Character) {
        _character.value = char
        sharedCharacterState.update(char)
    }
}
