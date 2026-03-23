package com.galeria.defensores.data.database.daos

import androidx.room.*
import com.galeria.defensores.data.database.entities.*

@Dao
interface RuleSystemDao {
    @Query("SELECT * FROM rule_systems")
    fun getAllReactive(): kotlinx.coroutines.flow.Flow<List<RuleSystemEntity>>

    @Query("SELECT * FROM rule_systems WHERE id = :id")
    fun getById(id: String): kotlinx.coroutines.flow.Flow<RuleSystemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ruleSystem: RuleSystemEntity)

    @Delete
    suspend fun delete(ruleSystem: RuleSystemEntity)

    @Query("DELETE FROM rule_systems WHERE id = :id")
    suspend fun deleteById(id: String)
}
