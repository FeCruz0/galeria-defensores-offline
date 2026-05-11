package com.galeria.defensores.data.database

import androidx.room.TypeConverter
import com.galeria.defensores.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: String?): List<String> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListString(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun fromAdvantageList(value: String?): List<AdvantageItem> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListAdvantage(list: List<AdvantageItem>): String = json.encodeToString(list)

    @TypeConverter
    fun fromSpellList(value: String?): List<Spell> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListSpell(list: List<Spell>): String = json.encodeToString(list)

    @TypeConverter
    fun fromInventoryList(value: String?): List<InventoryItem> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListInventory(list: List<InventoryItem>): String = json.encodeToString(list)

    @TypeConverter
    fun fromCustomRollList(value: String?): List<CustomRoll> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListCustomRoll(list: List<CustomRoll>): String = json.encodeToString(list)

    @TypeConverter
    fun fromRollResultList(value: String?): List<RollResult> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListRollResult(list: List<RollResult>): String = json.encodeToString(list)

    @TypeConverter
    fun fromUniqueAdvantage(value: String?): UniqueAdvantage? =
        try { value?.let { json.decodeFromString(it) } } catch (e: Exception) { null }

    @TypeConverter
    fun fromUniqueAdvantageToString(ua: UniqueAdvantage?): String? =
        ua?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromUniqueAdvantageList(value: String?): List<UniqueAdvantage> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListUniqueAdvantage(list: List<UniqueAdvantage>): String = json.encodeToString(list)

    @TypeConverter
    fun fromRuleSystem(value: String?): RuleSystem? =
        try { value?.let { json.decodeFromString(it) } } catch (e: Exception) { null }

    @TypeConverter
    fun fromRuleSystemToString(rs: RuleSystem?): String? =
        rs?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromVisualRoll(value: String?): VisualRoll? =
        try { value?.let { json.decodeFromString(it) } } catch (e: Exception) { null }

    @TypeConverter
    fun fromVisualRollToString(vr: VisualRoll?): String? =
        vr?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromCombatState(value: String?): CombatState? =
        try { value?.let { json.decodeFromString(it) } } catch (e: Exception) { null }

    @TypeConverter
    fun fromCombatStateToString(cs: CombatState?): String? =
        cs?.let { json.encodeToString(it) }

    @TypeConverter
    fun fromIntMap(value: String?): Map<String, Int> =
        try { json.decodeFromString(value ?: "{}") } catch (e: Exception) { emptyMap() }

    @TypeConverter
    fun fromMapInt(map: Map<String, Int>): String = json.encodeToString(map)
    
    @TypeConverter
    fun fromAttributeList(value: String?): List<AttributeDefinition> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListAttribute(list: List<AttributeDefinition>): String = json.encodeToString(list)

    @TypeConverter
    fun fromResourceList(value: String?): List<ResourceDefinition> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListResource(list: List<ResourceDefinition>): String = json.encodeToString(list)

    @TypeConverter
    fun fromItemDefinitionList(value: String?): List<ItemDefinition> =
        try { json.decodeFromString(value ?: "[]") } catch (e: Exception) { emptyList() }

    @TypeConverter
    fun fromListItemDefinition(list: List<ItemDefinition>): String = json.encodeToString(list)

    @TypeConverter
    fun fromDiceConfig(value: String?): DiceConfig? =
        try { value?.let { json.decodeFromString(it) } } catch (e: Exception) { null }

    @TypeConverter
    fun fromDiceConfigToString(dc: DiceConfig?): String? =
        dc?.let { json.encodeToString(it) }
}
