package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CloudDriveItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudDriveDao {
    @Query("SELECT * FROM cloud_drive_items ORDER BY isLeaking DESC, id ASC")
    fun getAllDriveItems(): Flow<List<CloudDriveItemEntity>>

    @Query("SELECT * FROM cloud_drive_items WHERE isLeaking = 1")
    fun getLeakingItems(): Flow<List<CloudDriveItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CloudDriveItemEntity>)

    @Update
    suspend fun updateItem(item: CloudDriveItemEntity)

    @Query("UPDATE cloud_drive_items SET shareStatus = 'PRIVATE', isLeaking = 0")
    suspend fun revokeAllPublicLinks()
}
