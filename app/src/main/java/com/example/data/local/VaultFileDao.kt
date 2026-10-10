package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE category = :category ORDER BY timestamp DESC")
    fun getFilesByCategory(category: String): Flow<List<VaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFileEntity): Long

    @Update
    suspend fun updateFile(file: VaultFileEntity)

    @Delete
    suspend fun deleteFile(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM vault_files")
    suspend fun getAllFilesSync(): List<VaultFileEntity>
}
