package com.galeria.defensores.data.database.daos

import androidx.room.*
import com.galeria.defensores.data.database.entities.*

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE ownerId = :userId")
    suspend fun getByOwner(userId: String): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE tableId = :tableId")
    suspend fun getByTable(tableId: String): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity)

    @Delete
    suspend fun delete(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)
}
