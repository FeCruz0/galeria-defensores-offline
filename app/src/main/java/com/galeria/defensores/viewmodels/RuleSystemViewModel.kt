package com.galeria.defensores.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.galeria.defensores.data.RuleSystemRepository
import com.galeria.defensores.data.SharedCharacterState
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.domain.usecases.GetResourceMaxUseCase
import com.galeria.defensores.models.AttributeDefinition
import com.galeria.defensores.models.Character
import com.galeria.defensores.models.ResourceDefinition
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.models.UniqueAdvantage
import com.galeria.defensores.models.validateUniqueNameAndKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

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

    private val _ruleSystem = MutableStateFlow(RuleSystem())
    val ruleSystem: StateFlow<RuleSystem> = _ruleSystem.asStateFlow()

    private val _availableDamageTypes = MutableStateFlow<List<String>>(emptyList())
    val availableDamageTypes: StateFlow<List<String>> = _availableDamageTypes.asStateFlow()

    private val _availableUniqueAdvantages = MutableStateFlow<List<UniqueAdvantage>>(emptyList())
    val availableUniqueAdvantages: StateFlow<List<UniqueAdvantage>> = _availableUniqueAdvantages.asStateFlow()

    private val defaultDamageTypes = listOf(
        "Corte", "Perfuração", "Esmagamento",
        "Fogo", "Frio", "Elétrico", "Químico", "Sônico"
    )

    private var currentTableId: String? = null

    private val currentRuleSystem: RuleSystem
        get() = _ruleSystem.value

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
        currentRuleSystem.validateUniqueNameAndKey(attr.id, attr.key, attr.name)
        val newAttrs = currentRuleSystem.attributes.toMutableList().also { it.add(attr) }
        saveRuleSystem(currentRuleSystem.copy(attributes = newAttrs))
    }

    fun updateAttributeDefinition(attr: AttributeDefinition) {
        currentRuleSystem.validateUniqueNameAndKey(attr.id, attr.key, attr.name)
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
        currentRuleSystem.validateUniqueNameAndKey(res.id, res.key, res.name)
        val newList = currentRuleSystem.resources.toMutableList().also { it.add(res) }
        saveRuleSystem(currentRuleSystem.copy(resources = newList))
    }

    fun updateResourceDefinition(res: ResourceDefinition) {
        currentRuleSystem.validateUniqueNameAndKey(res.id, res.key, res.name)
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
        Json { prettyPrint = true }.encodeToString(_ruleSystem.value ?: RuleSystem())
    } catch (e: Exception) {
        "{ \"error\": \"Failed to export system\" }"
    }

    fun importSystemJson(json: String, onComplete: (Boolean, RuleSystem?) -> Unit) {
        viewModelScope.launch {
            try {
                val importedSystem = Json { ignoreUnknownKeys = true }.decodeFromString<RuleSystem>(json)
                if (importedSystem.attributes.isEmpty() && importedSystem.resources.isEmpty()) {
                    onComplete(false, null)
                    return@launch
                }
                val tableId = currentTableId
                val newSystem = importedSystem.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    isBaseSystem = false
                )
                ruleSystemRepository.saveSystem(newSystem)
                
                if (!tableId.isNullOrEmpty()) {
                    val table = tableRepository.getTableOnce(tableId)
                    if (table != null) {
                        val updatedTable = table.copy(ruleSystemId = newSystem.id)
                        tableRepository.updateTable(updatedTable)
                    }
                }
                
                _ruleSystem.value = newSystem
                onComplete(true, newSystem)
            } catch (e: Exception) {
                android.util.Log.e("SystemImport", "Error parsing/saving JSON", e)
                onComplete(false, null)
            }
        }
    }

    fun saveSystemAs(newName: String, onComplete: (Boolean, RuleSystem?) -> Unit) {
        val current = _ruleSystem.value ?: return
        val newSystem = current.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName,
            isBaseSystem = false
        )
        viewModelScope.launch {
            try {
                ruleSystemRepository.saveSystem(newSystem)
                
                val tableId = currentTableId
                if (!tableId.isNullOrEmpty()) {
                    val table = tableRepository.getTableOnce(tableId)
                    if (table != null) {
                        val updatedTable = table.copy(ruleSystemId = newSystem.id)
                        tableRepository.updateTable(updatedTable)
                    }
                }
                
                _ruleSystem.value = newSystem
                onComplete(true, newSystem)
            } catch (e: Exception) {
                onComplete(false, null)
            }
        }
    }

    fun resetBaseSystem(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                ruleSystemRepository.resetBaseSystem()
                val current = _ruleSystem.value
                if (current?.id == "3det_alpha_base") {
                    _ruleSystem.value = ruleSystemRepository.getSystemOnce("3det_alpha_base") ?: RuleSystem()
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- Damage Types ---

    private fun getSystemDefaultDamageTypes(): List<String> {
        return currentRuleSystem.damageTypes.ifEmpty { defaultDamageTypes }
    }

    fun loadDamageTypes(tableId: String?) {
        currentTableId = tableId
        val baseDamageTypes = getSystemDefaultDamageTypes()
        if (tableId.isNullOrEmpty()) {
            _availableDamageTypes.value = baseDamageTypes.sorted()
            return
        }
        viewModelScope.launch {
            tableRepository.getTable(tableId).collect { table ->
                val customTypes = table?.customDamageTypes ?: emptyList()
                val deletedDefaults = customTypes.filter { it.startsWith("-") }.map { it.drop(1) }
                val addedCustoms = customTypes.filter { !it.startsWith("-") }
                _availableDamageTypes.value = (baseDamageTypes.filterNot { it in deletedDefaults } + addedCustoms).distinct().sorted()
            }
        }
    }

    fun addCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        if (type.isBlank()) return
        val baseDamageTypes = getSystemDefaultDamageTypes()
        viewModelScope.launch {
            val table = tableRepository.getTableOnce(tableId) ?: return@launch
            if (baseDamageTypes.contains(type)) {
                if (table.customDamageTypes.remove("-$type")) {
                    tableRepository.updateTable(table)
                }
            } else if (!table.customDamageTypes.contains(type)) {
                table.customDamageTypes.add(type)
                tableRepository.updateTable(table)
            }
        }
    }

    fun removeCustomDamageType(type: String) {
        val tableId = currentTableId ?: return
        val baseDamageTypes = getSystemDefaultDamageTypes()
        viewModelScope.launch {
            val table = tableRepository.getTableOnce(tableId) ?: return@launch
            if (baseDamageTypes.contains(type)) {
                if (!table.customDamageTypes.contains("-$type")) {
                    table.customDamageTypes.add("-$type")
                    tableRepository.updateTable(table)
                }
            } else {
                if (table.customDamageTypes.remove(type)) tableRepository.updateTable(table)
            }
        }
    }

    // --- Unique Advantages ---

    fun loadUniqueAdvantages(tableId: String?) {
        currentTableId = tableId
        if (tableId.isNullOrEmpty()) {
            _availableUniqueAdvantages.value = com.galeria.defensores.data.UniqueAdvantagesData.defaults.sortedBy { it.name }
            return
        }
        viewModelScope.launch {
            tableRepository.getTable(tableId).collect { table ->
                val defaults = com.galeria.defensores.data.UniqueAdvantagesData.defaults
                val custom = table?.customUniqueAdvantages ?: emptyList()
                _availableUniqueAdvantages.value = (defaults + custom).distinctBy { it.name }.sortedBy { it.name }
            }
        }
    }

    fun addCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTableOnce(tableId) ?: return@launch
            val idx = table.customUniqueAdvantages.indexOfFirst { it.name == ua.name }
            if (idx == -1) table.customUniqueAdvantages.add(ua)
            else table.customUniqueAdvantages[idx] = ua
            tableRepository.updateTable(table)
        }
    }

    fun removeCustomUniqueAdvantage(ua: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTableOnce(tableId) ?: return@launch
            if (table.customUniqueAdvantages.removeAll { it.name == ua.name && it.group == ua.group }) {
                if (tableRepository.updateTable(table)) loadUniqueAdvantages(tableId)
            }
        }
    }

    fun updateCustomUniqueAdvantage(oldUA: UniqueAdvantage, newUA: UniqueAdvantage) {
        val tableId = currentTableId ?: return
        viewModelScope.launch {
            val table = tableRepository.getTableOnce(tableId) ?: return@launch
            val index = table.customUniqueAdvantages.indexOfFirst { it.name == oldUA.name && it.group == oldUA.group }
            if (index != -1) {
                table.customUniqueAdvantages[index] = newUA
                if (tableRepository.updateTable(table)) loadUniqueAdvantages(tableId)
            }
        }
    }

    // --- Private ---

    private fun saveRuleSystem(system: RuleSystem) {
        val previousSystem = _ruleSystem.value
        _ruleSystem.value = system
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    ruleSystemRepository.saveSystem(system)
                }
            } catch (e: Exception) {
                android.util.Log.e("RuleSystemViewModel", "Error saving rule system: ${e.message}", e)
                // Revert state if save fails
                _ruleSystem.value = previousSystem
            }
        }
    }
}
