package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RemoteRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteRequestDao {
    @Query("SELECT * FROM remote_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<RemoteRequestEntity>>

    @Query("SELECT * FROM remote_requests WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRequests(): Flow<List<RemoteRequestEntity>>

    @Query("SELECT * FROM remote_requests WHERE requestId = :id LIMIT 1")
    suspend fun getRequestById(id: String): RemoteRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RemoteRequestEntity)

    @Update
    suspend fun updateRequest(request: RemoteRequestEntity)

    @Query("UPDATE remote_requests SET status = :status, resolvedAt = :resolvedAt WHERE requestId = :id")
    suspend fun updateStatus(id: String, status: String, resolvedAt: Long)
}
