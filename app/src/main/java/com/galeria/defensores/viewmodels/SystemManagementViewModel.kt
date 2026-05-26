package com.galeria.defensores.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.InventoryItem
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.Spell
import com.galeria.defensores.models.UniqueAdvantage
import com.galeria.defensores.models.validateUniqueNameAndKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject

@HiltViewModel
class SystemManagementViewModel @Inject constructor(
    private val ruleSystemRepository: RuleSystemRepository,
    private val sharedCharacterState: SharedCharacterState,
    private val getResourceMaxUseCase: GetResourceMaxUseCase
) : ViewModel() {

    // List of all rule systems in DB
    val systems: StateFlow<List<RuleSystem>> = ruleSystemRepository.getSystems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedSystem = MutableStateFlow<RuleSystem?>(null)
    val selectedSystem: StateFlow<RuleSystem?> = _selectedSystem.asStateFlow()

    private val _sandboxCharacter = MutableStateFlow<Character?>(null)
    val sandboxCharacter: StateFlow<Character?> = _sandboxCharacter.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _formulaValidationError = MutableStateFlow<String?>(null)
    val formulaValidationError: StateFlow<String?> = _formulaValidationError.asStateFlow()

    private val _customUniqueAdvantages = MutableStateFlow<List<UniqueAdvantage>>(emptyList())
    val customUniqueAdvantages: StateFlow<List<UniqueAdvantage>> = _customUniqueAdvantages.asStateFlow()

    /** Reactive list of damage types for the currently selected system. */
    val damageTypes: StateFlow<List<String>> = _selectedSystem
        .map { it?.damageTypes?.sorted() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var originalSystem: RuleSystem? = null

    // --- Loading & Initialization ---

    fun loadSystem(system: RuleSystem) {
        val sysCopy = system.copy(
            attributes = system.attributes.toMutableList(),
            resources = system.resources.toMutableList(),
            advantages = system.advantages.toMutableList(),
            disadvantages = system.disadvantages.toMutableList(),
            skills = system.skills.toMutableList(),
            damageTypes = system.damageTypes.toMutableList()
        )
        originalSystem = system
        _selectedSystem.value = sysCopy
        _isDirty.value = false
        _customUniqueAdvantages.value = emptyList()
        validateFormulaSyntax(sysCopy)
        resetSandboxCharacter(sysCopy)
    }

    private fun resetSandboxCharacter(system: RuleSystem) {
        val newChar = Character(
            id = java.util.UUID.randomUUID().toString(),
            name = "Personagem de Teste",
            tableId = "",
            ownerId = ""
        )
        // Initialize attributes
        system.attributes.forEach { attr ->
            newChar.attributeValues[attr.key] = 0
        }
        newChar.forca = 0
        newChar.habilidade = 0
        newChar.resistencia = 0
        newChar.armadura = 0
        newChar.poderFogo = 0

        // Initialize resource values based on formulas
        system.resources.forEach { res ->
            val maxVal = getResourceMaxUseCase(res, newChar, system)
            newChar.resourceValues[res.key] = maxVal
            if (res.key.equals("pv", ignoreCase = true)) newChar.currentPv = maxVal
            if (res.key.equals("pm", ignoreCase = true)) newChar.currentPm = maxVal
        }

        _sandboxCharacter.value = newChar
        sharedCharacterState.update(newChar)
    }

    private fun checkDirty() {
        val current = _selectedSystem.value
        val original = originalSystem
        if (current == null || original == null) {
            _isDirty.value = false
            return
        }
        
        // Base systems are read-only so structural changes should keep dirty state
        val isBase = original.isBaseSystem
        
        val structureChanged = current.name != original.name ||
                current.description != original.description ||
                current.attributes != original.attributes ||
                current.resources != original.resources ||
                current.advantages != original.advantages ||
                current.disadvantages != original.disadvantages ||
                current.skills != original.skills ||
                current.damageTypes != original.damageTypes ||
                current.diceConfig != original.diceConfig

        _isDirty.value = structureChanged || (isBase && structureChanged)
    }

    private fun validateFormulaSyntax(system: RuleSystem) {
        val dummyChar = Character()
        // Initialize dummy properties to check for errors in calculations
        system.attributes.forEach { dummyChar.attributeValues[it.key] = 1 }
        dummyChar.resistencia = 1
        dummyChar.habilidade = 1
        dummyChar.forca = 1
        dummyChar.armadura = 1
        dummyChar.poderFogo = 1

        var errorFound: String? = null
        for (res in system.resources) {
            try {
                getResourceMaxUseCase(res, dummyChar, system)
            } catch (e: Exception) {
                errorFound = "Erro na fórmula de ${res.name}: ${e.message}"
                break
            }
        }
        _formulaValidationError.value = errorFound
    }

    // --- Structural Customization ---

    fun addDamageType(type: String) {
        val current = _selectedSystem.value ?: return
        val trimmed = type.trim()
        if (trimmed.isBlank()) return
        if (current.damageTypes.any { it.equals(trimmed, ignoreCase = true) }) return
        val newTypes = current.damageTypes.toMutableList().also { it.add(trimmed) }
        _selectedSystem.value = current.copy(damageTypes = newTypes)
        checkDirty()
    }

    fun removeDamageType(type: String) {
        val current = _selectedSystem.value ?: return
        val newTypes = current.damageTypes.toMutableList()
        if (newTypes.remove(type)) {
            _selectedSystem.value = current.copy(damageTypes = newTypes)
            checkDirty()
        }
    }

    fun updateMetadata(name: String, description: String) {
        val current = _selectedSystem.value ?: return
        if (current.name == name && current.description == description) return
        val updated = current.copy(name = name, description = description)
        _selectedSystem.value = updated
        checkDirty()
    }

    fun addAttributeDefinition(attr: AttributeDefinition) {
        val current = _selectedSystem.value ?: return
        current.validateUniqueNameAndKey(attr.id, attr.key, attr.name)
        
        val newAttrs = current.attributes.toMutableList().also { it.add(attr) }
        val updated = current.copy(attributes = newAttrs)
        _selectedSystem.value = updated
        
        // Sync sandbox character properties
        mutateCharacter { char ->
            char.attributeValues[attr.key] = 0
        }
        
        validateFormulaSyntax(updated)
        checkDirty()
    }

    fun updateAttributeDefinition(attr: AttributeDefinition) {
        val current = _selectedSystem.value ?: return
        current.validateUniqueNameAndKey(attr.id, attr.key, attr.name)
        
        val newAttrs = current.attributes.toMutableList()
        val index = newAttrs.indexOfFirst { it.id == attr.id }
        if (index == -1) return

        val oldKey = newAttrs[index].key
        val newKey = attr.key
        newAttrs[index] = attr

        // Migrate resource formulas referencing the old key
        val newResources = current.resources.map { res ->
            if (res.formula.contains(oldKey, ignoreCase = true)) {
                val pattern = "(?i)\\b$oldKey\\b".toRegex()
                res.copy(formula = res.formula.replace(pattern, newKey))
            } else res
        }.toMutableList()

        // Migrate character attribute values
        mutateCharacter { char ->
            if (char.attributeValues.containsKey(oldKey)) {
                val value = char.attributeValues.remove(oldKey) ?: 0
                char.attributeValues[newKey] = value
            }
            if (oldKey == "forca") char.forca = 0
            if (oldKey == "habilidade") char.habilidade = 0
            if (oldKey == "resistencia") char.resistencia = 0
            if (oldKey == "armadura") char.armadura = 0
            if (oldKey == "poderFogo") char.poderFogo = 0
            
            when (newKey) {
                "forca" -> char.forca = char.attributeValues[newKey] ?: 0
                "habilidade" -> char.habilidade = char.attributeValues[newKey] ?: 0
                "resistencia" -> char.resistencia = char.attributeValues[newKey] ?: 0
                "armadura" -> char.armadura = char.attributeValues[newKey] ?: 0
                "poderFogo" -> char.poderFogo = char.attributeValues[newKey] ?: 0
            }
        }

        val updated = current.copy(attributes = newAttrs, resources = newResources)
        _selectedSystem.value = updated
        validateFormulaSyntax(updated)
        checkDirty()
    }

    fun removeAttributeDefinition(attr: AttributeDefinition) {
        val current = _selectedSystem.value ?: return
        val newAttrs = current.attributes.toMutableList().also { it.removeAll { a -> a.id == attr.id } }
        val updated = current.copy(attributes = newAttrs)
        _selectedSystem.value = updated

        mutateCharacter { char ->
            char.attributeValues.remove(attr.key)
        }

        validateFormulaSyntax(updated)
        checkDirty()
    }

    fun addResourceDefinition(res: ResourceDefinition) {
        val current = _selectedSystem.value ?: return
        current.validateUniqueNameAndKey(res.id, res.key, res.name)
        
        val newResources = current.resources.toMutableList().also { it.add(res) }
        val updated = current.copy(resources = newResources)
        _selectedSystem.value = updated
        
        mutateCharacter { char ->
            // Trigger resource initialization
        }

        validateFormulaSyntax(updated)
        checkDirty()
    }

    fun updateResourceDefinition(res: ResourceDefinition) {
        val current = _selectedSystem.value ?: return
        current.validateUniqueNameAndKey(res.id, res.key, res.name)
        
        val newResources = current.resources.toMutableList()
        val index = newResources.indexOfFirst { it.id == res.id }
        if (index == -1) return
        newResources[index] = res
        val updated = current.copy(resources = newResources)
        _selectedSystem.value = updated

        mutateCharacter { char ->
            // Trigger recalculation
        }

        validateFormulaSyntax(updated)
        checkDirty()
    }

    fun removeResourceDefinition(res: ResourceDefinition) {
        val current = _selectedSystem.value ?: return
        val newResources = current.resources.toMutableList().also { it.removeAll { r -> r.id == res.id } }
        val updated = current.copy(resources = newResources)
        _selectedSystem.value = updated

        mutateCharacter { char ->
            char.resourceValues.remove(res.key)
        }

        validateFormulaSyntax(updated)
        checkDirty()
    }

    // --- Import & Export JSON ---

    fun exportSystemJson(): String = try {
        Json { prettyPrint = true }.encodeToString(_selectedSystem.value ?: RuleSystem())
    } catch (e: Exception) {
        "{ \"error\": \"Failed to export system\" }"
    }

    fun importSystemJson(json: String, onComplete: (Boolean, RuleSystem?) -> Unit) {
        try {
            val imported = Json { ignoreUnknownKeys = true }.decodeFromString<RuleSystem>(json)
            if (imported.attributes.isEmpty() && imported.resources.isEmpty()) {
                onComplete(false, null)
                return
            }
            val draft = imported.copy(
                id = java.util.UUID.randomUUID().toString(),
                isBaseSystem = false,
                attributes = imported.attributes.toMutableList(),
                resources = imported.resources.toMutableList()
            )
            _selectedSystem.value = draft
            _isDirty.value = true // Since it's imported and not yet in database
            validateFormulaSyntax(draft)
            resetSandboxCharacter(draft)
            onComplete(true, draft)
        } catch (e: Exception) {
            onComplete(false, null)
        }
    }

    // --- Save / Revert / Delete Operations ---

    fun saveSystem(newName: String? = null, onComplete: (Boolean, String?) -> Unit) {
        val current = _selectedSystem.value ?: return
        viewModelScope.launch {
            try {
                if (current.isBaseSystem && newName == null) {
                    onComplete(false, "Sistemas base não podem ser modificados diretamente. Salve como um novo sistema.")
                    return@launch
                }

                val systemToSave = if (newName != null) {
                    current.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        name = newName,
                        isBaseSystem = false,
                        attributes = current.attributes.toMutableList(),
                        resources = current.resources.toMutableList(),
                        advantages = current.advantages.toMutableList(),
                        disadvantages = current.disadvantages.toMutableList(),
                        skills = current.skills.toMutableList(),
                        damageTypes = current.damageTypes.toMutableList()
                    )
                } else {
                    current
                }

                ruleSystemRepository.saveSystem(systemToSave)
                originalSystem = systemToSave.copy(
                    attributes = systemToSave.attributes.toMutableList(),
                    resources = systemToSave.resources.toMutableList()
                )
                _selectedSystem.value = systemToSave
                checkDirty()
                onComplete(true, null)
            } catch (e: IllegalArgumentException) {
                onComplete(false, e.message)
            } catch (e: Exception) {
                onComplete(false, "Falha ao salvar sistema: ${e.message}")
            }
        }
    }

    fun revertSystem() {
        val original = originalSystem ?: return
        loadSystem(original)
    }

    fun deleteSystem(onComplete: (Boolean, String?) -> Unit) {
        val current = _selectedSystem.value ?: return
        if (current.isBaseSystem) {
            onComplete(false, "Não é possível excluir um sistema base.")
            return
        }
        viewModelScope.launch {
            try {
                val success = ruleSystemRepository.deleteSystem(current.id)
                if (success) {
                    val fallback = ruleSystemRepository.getSystemOrDefault(null)
                    loadSystem(fallback)
                    onComplete(true, null)
                } else {
                    onComplete(false, "Falha ao excluir o sistema.")
                }
            } catch (e: Exception) {
                onComplete(false, "Erro: ${e.message}")
            }
        }
    }

    // --- Sandbox Character Manipulation ---

    private fun mutateCharacter(block: (Character) -> Unit) {
        val char = _sandboxCharacter.value ?: return
        val newChar = char.deepCopy()
        block(newChar)

        val sys = _selectedSystem.value ?: RuleSystem()
        sys.resources.forEach { res ->
            val max = getResourceMaxUseCase(res, newChar, sys)
            if (!newChar.resourceValues.containsKey(res.key)) {
                newChar.resourceValues[res.key] = max
            }
            if (res.key.equals("pv", ignoreCase = true)) {
                newChar.currentPv = newChar.currentPv.coerceAtMost(max)
            } else if (res.key.equals("pm", ignoreCase = true)) {
                newChar.currentPm = newChar.currentPm.coerceAtMost(max)
            } else {
                val current = newChar.resourceValues[res.key] ?: max
                newChar.resourceValues[res.key] = current.coerceAtMost(max)
            }
        }

        _sandboxCharacter.value = newChar
        sharedCharacterState.update(newChar)
    }

    fun updateAttribute(key: String, value: Int) {
        mutateCharacter { char ->
            val newValue = value.coerceIn(0, 99)
            char.attributeValues[key] = newValue
            when (key) {
                "forca" -> char.forca = newValue
                "habilidade" -> char.habilidade = newValue
                "resistencia" -> char.resistencia = newValue
                "armadura" -> char.armadura = newValue
                "poderFogo" -> char.poderFogo = newValue
            }
        }
    }

    fun updateResource(key: String, delta: Int) {
        if (key.equals("pv", ignoreCase = true)) { updateStatus("pv", delta); return }
        if (key.equals("pm", ignoreCase = true)) { updateStatus("pm", delta); return }
        mutateCharacter { char ->
            val current = char.resourceValues[key] ?: 0
            char.resourceValues[key] = (current + delta).coerceIn(0, 9999)
        }
    }

    fun updateStatus(type: String, delta: Int) {
        mutateCharacter { char ->
            val sys = _selectedSystem.value ?: RuleSystem()
            val pvRes = sys.resources.find { it.key.equals("pv", ignoreCase = true) } ?: ResourceDefinition(key="pv", name="PV")
            val pmRes = sys.resources.find { it.key.equals("pm", ignoreCase = true) } ?: ResourceDefinition(key="pm", name="PM")
            val maxPv = getResourceMaxUseCase(pvRes, char, sys)
            val maxPm = getResourceMaxUseCase(pmRes, char, sys)
            
            when (type) {
                "pv" -> char.currentPv = (char.currentPv + delta).coerceIn(0, maxPv)
                "pm" -> char.currentPm = (char.currentPm + delta).coerceIn(0, maxPm)
            }
        }
    }

    fun setStatus(type: String, value: Int) {
        mutateCharacter { char ->
            val sys = _selectedSystem.value ?: RuleSystem()
            val pvRes = sys.resources.find { it.key.equals("pv", ignoreCase = true) } ?: ResourceDefinition(key="pv", name="PV")
            val pmRes = sys.resources.find { it.key.equals("pm", ignoreCase = true) } ?: ResourceDefinition(key="pm", name="PM")
            val maxPv = getResourceMaxUseCase(pvRes, char, sys)
            val maxPm = getResourceMaxUseCase(pmRes, char, sys)

            when (type) {
                "pv" -> char.currentPv = value.coerceIn(0, maxPv)
                "pm" -> char.currentPm = value.coerceIn(0, maxPm)
            }
        }
    }

    fun updateName(name: String) { mutateCharacter { it.name = name } }
    fun updateScale(scale: Int) { mutateCharacter { it.scale = scale } }
    fun updateNotes(html: String) { mutateCharacter { it.anotacoes = html } }
    fun updateSavedPoints(points: Int) { mutateCharacter { it.savedPoints = points.coerceAtLeast(0) } }

    fun updateExperience(xp: Int) {
        mutateCharacter { char ->
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
        mutateCharacter { char ->
            if (isPdf) char.damageTypePdf = type else char.damageTypeForca = type
        }
    }

    fun setUniqueAdvantage(ua: UniqueAdvantage?) {
        mutateCharacter { it.uniqueAdvantage = ua }
    }

    fun addCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val current = _customUniqueAdvantages.value.toMutableList()
        current.add(ua)
        _customUniqueAdvantages.value = current
    }

    fun removeCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val current = _customUniqueAdvantages.value.filter { it.name != ua.name }
        _customUniqueAdvantages.value = current
    }

    fun updateCustomUniqueAdvantage(old: UniqueAdvantage, new: UniqueAdvantage) {
        val current = _customUniqueAdvantages.value.toMutableList()
        val idx = current.indexOfFirst { it.name == old.name }
        if (idx != -1) {
            current[idx] = new
            _customUniqueAdvantages.value = current
        }
    }

    fun addAdvantage(advantage: AdvantageItem) {
        mutateCharacter { it.vantagens = it.vantagens.toMutableList().also { l -> l.add(advantage.copy(id = java.util.UUID.randomUUID().toString())) } }
    }

    fun updateAdvantage(advantage: AdvantageItem) {
        mutateCharacter { char ->
            val list = char.vantagens.toMutableList()
            val idx = list.indexOfFirst { it.id == advantage.id }
            if (idx != -1) {
                list[idx] = advantage
                char.vantagens = list
            }
        }
    }

    fun removeAdvantage(advantage: AdvantageItem) {
        mutateCharacter { it.vantagens = it.vantagens.filter { a -> a.id != advantage.id }.toMutableList() }
    }

    fun addDisadvantage(dis: AdvantageItem) {
        mutateCharacter { it.desvantagens = it.desvantagens.toMutableList().also { l -> l.add(dis.copy(id = java.util.UUID.randomUUID().toString())) } }
    }

    fun updateDisadvantage(dis: AdvantageItem) {
        mutateCharacter { char ->
            val list = char.desvantagens.toMutableList()
            val idx = list.indexOfFirst { it.id == dis.id }
            if (idx != -1) {
                list[idx] = dis
                char.desvantagens = list
            }
        }
    }

    fun removeDisadvantage(dis: AdvantageItem) {
        mutateCharacter { it.desvantagens = it.desvantagens.filter { a -> a.id != dis.id }.toMutableList() }
    }

    fun addSkill(skill: AdvantageItem) {
        mutateCharacter { it.pericias = it.pericias.toMutableList().also { l -> l.add(skill.copy(id = java.util.UUID.randomUUID().toString())) } }
    }

    fun updateSkill(skill: AdvantageItem) {
        mutateCharacter { char ->
            val list = char.pericias.toMutableList()
            val idx = list.indexOfFirst { it.id == skill.id }
            if (idx != -1) {
                list[idx] = skill
                char.pericias = list
            }
        }
    }

    fun removeSkill(skill: AdvantageItem) {
        mutateCharacter { it.pericias = it.pericias.filter { s -> s.id != skill.id }.toMutableList() }
    }

    fun addSpecializations(specs: List<AdvantageItem>) {
        mutateCharacter { it.especializacoes = it.especializacoes.toMutableList().also { l -> l.addAll(specs.map { it.copy(id = java.util.UUID.randomUUID().toString()) }) } }
    }

    fun updateSpecialization(spec: AdvantageItem) {
        mutateCharacter { char ->
            val list = char.especializacoes.toMutableList()
            val idx = list.indexOfFirst { it.id == spec.id }
            if (idx != -1) {
                list[idx] = spec
                char.especializacoes = list
            }
        }
    }

    fun removeSpecialization(spec: AdvantageItem) {
        mutateCharacter { it.especializacoes = it.especializacoes.filter { s -> s.id != spec.id }.toMutableList() }
    }

    fun addSpell(spell: Spell) {
        mutateCharacter { it.magias = it.magias.toMutableList().also { l -> l.add(spell.copy(id = java.util.UUID.randomUUID().toString())) } }
    }

    fun updateSpell(spell: Spell) {
        mutateCharacter { char ->
            val list = char.magias.toMutableList()
            val idx = list.indexOfFirst { it.id == spell.id }
            if (idx != -1) {
                list[idx] = spell
                char.magias = list
            }
        }
    }

    fun removeSpell(spell: Spell) {
        mutateCharacter { it.magias = it.magias.filter { s -> s.id != spell.id }.toMutableList() }
    }

    fun addInventoryItem(item: InventoryItem) {
        mutateCharacter { it.inventario = it.inventario.toMutableList().also { l -> l.add(item.copy(id = java.util.UUID.randomUUID().toString())) } }
    }

    fun updateInventoryItem(item: InventoryItem) {
        mutateCharacter { char ->
            val list = char.inventario.toMutableList()
            val idx = list.indexOfFirst { it.id == item.id }
            if (idx != -1) {
                list[idx] = item
                char.inventario = list
            }
        }
    }

    fun removeInventoryItem(item: InventoryItem) {
        mutateCharacter { it.inventario = it.inventario.filter { i -> i.id != item.id }.toMutableList() }
    }

    fun adjustInventoryQuantity(item: InventoryItem, delta: Int) {
        val qty = item.quantity.toIntOrNull() ?: return
        updateInventoryItem(item.copy(quantity = (qty + delta).coerceAtLeast(0).toString()))
    }

    fun addCustomRoll(roll: CustomRoll) {
        mutateCharacter { it.customRolls = it.customRolls.toMutableList().also { l -> l.add(roll) } }
    }

    fun updateCustomRoll(roll: CustomRoll) {
        mutateCharacter { char ->
            val list = char.customRolls.toMutableList()
            val idx = list.indexOfFirst { it.id == roll.id }
            if (idx != -1) {
                list[idx] = roll
                char.customRolls = list
            }
        }
    }

    fun removeCustomRoll(roll: CustomRoll) {
        mutateCharacter { it.customRolls = it.customRolls.filter { r -> r.id != roll.id }.toMutableList() }
    }
}
