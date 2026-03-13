package com.galeria.defensores.data.database

import androidx.room.TypeConverter
import com.galeria.defensores.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListString(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromAdvantageList(value: String?): List<AdvantageItem> {
        val listType = object : TypeToken<List<AdvantageItem>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListAdvantage(list: List<AdvantageItem>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromSpellList(value: String?): List<Spell> {
        val listType = object : TypeToken<List<Spell>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListSpell(list: List<Spell>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromInventoryList(value: String?): List<InventoryItem> {
        val listType = object : TypeToken<List<InventoryItem>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListInventory(list: List<InventoryItem>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromCustomRollList(value: String?): List<CustomRoll> {
        val listType = object : TypeToken<List<CustomRoll>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListCustomRoll(list: List<CustomRoll>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromRollResultList(value: String?): List<RollResult> {
        val listType = object : TypeToken<List<RollResult>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListRollResult(list: List<RollResult>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromUniqueAdvantage(value: String?): UniqueAdvantage? {
        return gson.fromJson(value, UniqueAdvantage::class.java)
    }

    @TypeConverter
    fun fromUniqueAdvantageToString(ua: UniqueAdvantage?): String? {
        return gson.toJson(ua)
    }

    @TypeConverter
    fun fromUniqueAdvantageList(value: String?): List<UniqueAdvantage> {
        val listType = object : TypeToken<List<UniqueAdvantage>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListUniqueAdvantage(list: List<UniqueAdvantage>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromRuleSystem(value: String?): RuleSystem? {
        return gson.fromJson(value, RuleSystem::class.java)
    }

    @TypeConverter
    fun fromRuleSystemToString(rs: RuleSystem?): String? {
        return gson.toJson(rs)
    }

    @TypeConverter
    fun fromVisualRoll(value: String?): VisualRoll? {
        return gson.fromJson(value, VisualRoll::class.java)
    }

    @TypeConverter
    fun fromVisualRollToString(vr: VisualRoll?): String? {
        return gson.toJson(vr)
    }

    @TypeConverter
    fun fromCombatState(value: String?): CombatState? {
        return gson.fromJson(value, CombatState::class.java)
    }

    @TypeConverter
    fun fromCombatStateToString(cs: CombatState?): String? {
        return gson.toJson(cs)
    }

    @TypeConverter
    fun fromIntMap(value: String?): Map<String, Int> {
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value ?: "{}", mapType)
    }

    @TypeConverter
    fun fromMapInt(map: Map<String, Int>): String {
        return gson.toJson(map)
    }
    
    @TypeConverter
    fun fromAttributeList(value: String?): List<AttributeDefinition> {
        val listType = object : TypeToken<List<AttributeDefinition>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListAttribute(list: List<AttributeDefinition>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromResourceList(value: String?): List<ResourceDefinition> {
        val listType = object : TypeToken<List<ResourceDefinition>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListResource(list: List<ResourceDefinition>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromItemDefinitionList(value: String?): List<ItemDefinition> {
        val listType = object : TypeToken<List<ItemDefinition>>() {}.type
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromListItemDefinition(list: List<ItemDefinition>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromDiceConfig(value: String?): DiceConfig? {
        return gson.fromJson(value, DiceConfig::class.java)
    }

    @TypeConverter
    fun fromDiceConfigToString(dc: DiceConfig?): String? {
        return gson.toJson(dc)
    }
}
