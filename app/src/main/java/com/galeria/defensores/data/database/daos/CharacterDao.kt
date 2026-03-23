package com.galeria.defensores.data.database.daos

import androidx.room.*
import com.galeria.defensores.data.database.entities.*

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters")
    fun getAllReactive(): kotlinx.coroutines.flow.Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getById(id: String): kotlinx.coroutines.flow.Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE ownerId = :userId")
    fun getByOwner(userId: String): kotlinx.coroutines.flow.Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE tableId = :tableId")
    fun getByTable(tableId: String): kotlinx.coroutines.flow.Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity)

    @Delete
    suspend fun delete(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)
}
