package com.galeria.defensores.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.galeria.defensores.models.*

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val masterId: String,
    val players: List<String>,
    val isPrivate: Boolean,
    val password: String?,
    val ruleSystemId: String,
    // Note: rulesMod could be complex, for now we skip or store as Map via TypeConverter if needed
    // Assuming rulesMod is emptyMap or simple Map<String, String> mostly.
    // Based on Table.kt, it's Map<String, Any>. We'll use Map<String, String> or just GSON it.
    val rollHistory: List<RollResult>,
    val customDamageTypes: List<String>,
    val customUniqueAdvantages: List<UniqueAdvantage>,
    val lastVisualRoll: VisualRoll?,
    val combatState: CombatState?
) {
    fun toTable(): Table {
        val table = Table(
            id = id,
            name = name,
            description = description,
            masterId = masterId,
            players = players.toMutableList(),
            isPrivate = isPrivate,
            password = password,
            ruleSystemId = ruleSystemId,
            rollHistory = rollHistory.toMutableList(),
            customDamageTypes = customDamageTypes.toMutableList(),
            customUniqueAdvantages = customUniqueAdvantages.toMutableList(),
            lastVisualRoll = lastVisualRoll,
            combatState = combatState
        )
        return table
    }

    companion object {
        fun fromTable(table: Table): TableEntity {
            return TableEntity(
                id = table.id,
                name = table.name,
                description = table.description,
                masterId = table.masterId,
                players = table.players,
                isPrivate = table.isPrivate,
                password = table.password,
                ruleSystemId = table.ruleSystemId,
                rollHistory = table.rollHistory,
                customDamageTypes = table.customDamageTypes,
                customUniqueAdvantages = table.customUniqueAdvantages,
                lastVisualRoll = table.lastVisualRoll,
                combatState = table.combatState
            )
        }
    }
}
