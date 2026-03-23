package com.galeria.defensores.data.database.daos

import androidx.room.*
import com.galeria.defensores.data.database.entities.*

@Dao
interface TableDao {
    @Query("SELECT * FROM tables")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    fun getById(id: String): kotlinx.coroutines.flow.Flow<TableEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(table: TableEntity)

    @Delete
    suspend fun delete(table: TableEntity)

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteById(id: String)
}
