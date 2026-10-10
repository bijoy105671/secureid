package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SensorAccessLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorAccessDao {
    @Query("SELECT * FROM sensor_access_logs ORDER BY timestamp DESC")
    fun getAllAccessLogs(): Flow<List<SensorAccessLogEntity>>

    @Query("SELECT * FROM sensor_access_logs WHERE sensorType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<SensorAccessLogEntity>>

    @Query("SELECT * FROM sensor_access_logs WHERE isUnusual = 1 ORDER BY timestamp DESC")
    fun getUnusualAccessLogs(): Flow<List<SensorAccessLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SensorAccessLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<SensorAccessLogEntity>)

    @Query("DELETE FROM sensor_access_logs")
    suspend fun clearAll()
}
