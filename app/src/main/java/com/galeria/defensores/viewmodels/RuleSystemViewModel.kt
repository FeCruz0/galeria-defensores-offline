package com.galeria.defensores.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.UniqueAdvantage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Manages the rule system definition (attributes, resources, dice config),
 * damage types, and unique advantages.
 */
@HiltViewModel
class RuleSystemViewModel @Inject constructor(
    private val ruleSystemRepository: RuleSystemRepository,
    private val tableRepository: TableRepository,
    private val sharedCharacterState: SharedCharacterState,
    private val getResourceMaxUseCase: GetResourceMaxUseCase
) : ViewModel() {

    private val _ruleSystem = MutableLiveData<RuleSystem>(RuleSystem())
    val ruleSystem: LiveData<RuleSystem> = _ruleSystem

    private val _availableDamageTypes = MutableLiveData<List<String>>()
    val availableDamageTypes: LiveData<List<String>> = _availableDamageTypes

    private val _availableUniqueAdvantages = MutableLiveData<List<UniqueAdvantage>>()
    val availableUniqueAdvantages: LiveData<List<UniqueAdvantage>> = _availableUniqueAdvantages

    private val defaultDamageTypes = listOf(
        "Corte", "Perfuração", "Esmagamento",
        "Fogo", "Frio", "Elétrico", "Químico", "Sônico"
    )

    private var currentTableId: String? = null

    private val currentRuleSystem: RuleSystem
        get() = _ruleSystem.value!!

    // --- Public API ---

    fun loadRuleSystem(system: RuleSystem) {
        _ruleSystem.value = system
    }

    fun getAttributeName(key: String): String =
        currentRuleSystem.attributes.find { it.key == key }?.name
            ?: key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }

    fun getDerivedStatName(key: String): String =
        currentRuleSystem.resources.find { it.key == key }?.name ?: key.uppercase()

    fun calculateResourceMax(res: ResourceDefinition, char: Character): Int =
        getResourceMaxUseCase(res, char, currentRuleSystem)

    // --- Rule System Editing: Attributes ---

    fun addAttributeDefinition(attr: AttributeDefinition) {
        val newAttrs = currentRuleSystem.attributes.toMutableList().also { it.add(attr) }
        saveRuleSystem(currentRuleSystem.copy(attributes = newAttrs))
    }

    fun updateAttributeDefinition(attr: AttributeDefinition) {
        val system = currentRuleSystem
        val newAttrs = system.attributes.toMutableList()
        val index = newAttrs.indexOfFirst { it.id == attr.id }
        if (index == -1) return

        val oldKey = newAttrs[index].key
        val newKey = attr.key
        newAttrs[index] = attr

        var newResources = system.resources.toMutableList()
        if (oldKey != newKey) {
            newResources = newResources.map { res ->
                if (res.formula.contains(oldKey, ignoreCase = true)) {
                    val pattern = "(?i)\\b$oldKey\\b".toRegex()
                    res.copy(formula = res.formula.replace(pattern, newKey))
                } else res
            }.toMutableList()

            // Migrate character attribute values
            sharedCharacterState.character.value?.let { char ->
                if (char.attributeValues.containsKey(oldKey)) {
                    val oldVal = char.attributeValues.remove(oldKey) ?: 0
                    char.attributeValues[newKey] = oldVal
                    sharedCharacterState.update(char)
                }
            }
        }

        saveRuleSystem(system.copy(attributes = newAttrs, resources = newResources))
    }

    fun removeAttributeDefinition(attr: AttributeDefinition) {
        val newAttrs = currentRuleSystem.attributes.toMutableList().also { it.removeAll { a -> a.id == attr.id } }
        saveRuleSystem(currentRuleSystem.copy(attributes = newAttrs))
    }

    // --- Rule System Editing: Resources ---

    fun addResourceDefinition(res: ResourceDefinition) {
        val newList = currentRuleSystem.resources.toMutableList().also { it.add(res) }
        saveRuleSystem(currentRuleSystem.copy(resources = newList))
    }

    fun updateResourceDefinition(res: ResourceDefinition) {
        val newList = currentRuleSystem.resources.toMutableList()
        val index = newList.indexOfFirst { it.id == res.id }
        if (index != -1) {
            newList[index] = res
            saveRuleSystem(currentRuleSystem.copy(resources = newList))
        }
    }

    fun removeResourceDefinition(res: ResourceDefinition) {
        val newList = currentRuleSystem.resources.toMutableList().also { it.removeAll { r -> r.id == res.id } }
        saveRuleSystem(currentRuleSystem.copy(resources = newList))
    }

    // --- Rule System Import/Export/Reset ---

    fun exportSystemJson(): String = try {
        com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(_ruleSystem.value ?: RuleSystem())
    } catch (e: Exception) {
        "{ \"error\": \"Failed to export system\" }"
    }

    fun importSystemJson(json: String): Boolean {
        return try {
            val newSystem = com.google.gson.Gson().fromJson(json, RuleSystem::class.java) ?: return false
            if (newSystem.attributes.isEmpty() && newSystem.resources.isEmpty()) return false
            val current = _ruleSystem.value ?: RuleSystem()
            val updated = current.copy(
                name = newSystem.name,
                description = newSystem.description,
                attributes = newSystem.attributes,
                resources = newSystem.resources,
                diceConfig = newSystem.diceConfig
            )
            saveRuleSystem(updated)
            true
        } catch (e: Exception) {
            android.util.Log.e("SystemImport", "Error parsing JSON", e)
            false
        }
    }

    fun saveSystemAs(newName: String, onComplete: (Boolean) -> Unit) {
        val current = _ruleSystem.value ?: return
        val newSystem = current.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName,
            isBaseSystem = false
        )
        viewModelScope.launch {
            try {
                ruleSystemRepository.saveSystem(newSystem)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun resetBaseSystem(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                ruleSystemRepository.resetBaseSystem()
                val current = _ruleSystem.value
                if (current?.id == "3det_alpha_base") {
                    _ruleSystem.value = ruleSystemRepository.getSystem("3det_alpha_base")
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- Damage Types ---

    fun loadDamageTypes(tableId: String?) {
        currentTableId = tableId
        viewModelScope.launch {
            val customTypes = if (!tableId.isNullOrEmpty()) {
                tableRepository.getTable(tableId)?.customDamageTypes ?: emptyList()
            } else emptyList()
            _availableDamageTypes.value = (defaultDamageTypes + customTypes).distinct().sorted()
        }
    }

    fun addCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        if (type.isBlank()) return
        viewModelScope.launch {
            val table = tableRepository.getTable(tableId) ?: return@launch
            if (!table.customDamageTypes.contains(type)) {
                table.customDamageTypes.add(type)
                tableRepository.updateTable(table)
            }
            loadDamageTypes(tableId)
        }
    }

    fun removeCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTable(tableId) ?: return@launch
            if (table.customDamageTypes.remove(type)) tableRepository.updateTable(table)
            loadDamageTypes(tableId)
        }
    }

    // --- Unique Advantages ---

    fun loadUniqueAdvantages(tableId: String?) {
        currentTableId = tableId
        viewModelScope.launch {
            val defaults = com.galeria.defensores.data.UniqueAdvantagesData.defaults
            val custom = if (!tableId.isNullOrEmpty()) {
                tableRepository.getTable(tableId)?.customUniqueAdvantages ?: emptyList()
            } else emptyList()
            _availableUniqueAdvantages.value = (defaults + custom).sortedBy { it.name }
        }
    }

    fun addCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTable(tableId) ?: return@launch
            val idx = table.customUniqueAdvantages.indexOfFirst { it.name == ua.name }
            if (idx == -1) table.customUniqueAdvantages.add(ua)
            else table.customUniqueAdvantages[idx] = ua
            if (tableRepository.updateTable(table)) loadUniqueAdvantages(tableId)
        }
    }

    fun removeCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTable(tableId) ?: return@launch
            if (table.customUniqueAdvantages.removeIf { it.name == ua.name && it.group == ua.group }) {
                if (tableRepository.updateTable(table)) loadUniqueAdvantages(tableId)
            }
        }
    }

    fun updateCustomUniqueAdvantage(oldUA: UniqueAdvantage, newUA: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTable(tableId) ?: return@launch
            val index = table.customUniqueAdvantages.indexOfFirst { it.name == oldUA.name && it.group == oldUA.group }
            if (index != -1) {
                table.customUniqueAdvantages[index] = newUA
                if (tableRepository.updateTable(table)) loadUniqueAdvantages(tableId)
            }
        }
    }

    // --- Private ---

    private fun saveRuleSystem(system: RuleSystem) {
        _ruleSystem.value = system
        viewModelScope.launch {
            withContext(NonCancellable) {
                ruleSystemRepository.saveSystem(system)
            }
        }
    }
}
