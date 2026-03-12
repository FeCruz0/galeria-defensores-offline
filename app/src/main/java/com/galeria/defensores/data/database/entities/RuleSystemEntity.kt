package com.galeria.defensores.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.galeria.defensores.models.*

@Entity(tableName = "rule_systems")
data class RuleSystemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isBaseSystem: Boolean,
    val attributes: List<AttributeDefinition>,
    val resources: List<ResourceDefinition>,
    val advantages: List<ItemDefinition>,
    val disadvantages: List<ItemDefinition>,
    val skills: List<ItemDefinition>,
    val damageTypes: List<String>,
    val diceConfig: DiceConfig
) {
    fun toRuleSystem(): RuleSystem {
        return RuleSystem(
            id = id,
            name = name,
            description = description,
            isBaseSystem = isBaseSystem,
            attributes = attributes.toMutableList(),
            resources = resources.toMutableList(),
            advantages = advantages.toMutableList(),
            disadvantages = disadvantages.toMutableList(),
            skills = skills.toMutableList(),
            damageTypes = damageTypes.toMutableList(),
            diceConfig = diceConfig
        )
    }

    companion object {
        fun fromRuleSystem(rs: RuleSystem): RuleSystemEntity {
            return RuleSystemEntity(
                id = rs.id,
                name = rs.name,
                description = rs.description,
                isBaseSystem = rs.isBaseSystem,
                attributes = rs.attributes,
                resources = rs.resources,
                advantages = rs.advantages,
                disadvantages = rs.disadvantages,
                skills = rs.skills,
                damageTypes = rs.damageTypes,
                diceConfig = rs.diceConfig
            )
        }
    }
}
