package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SensorType(val labelBn: String, val labelEn: String) {
    MICROPHONE("মাইক্রোফোন", "Microphone"),
    CAMERA("ক্যামেরা", "Camera"),
    CALL("কল ও ডায়লার", "Phone & Calls")
}

@Entity(tableName = "sensor_access_logs")
data class SensorAccessLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val sensorType: String, // MICROPHONE, CAMERA, CALL
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 12,
    val isBackground: Boolean = false,
    val isUnusual: Boolean = false,
    val unusualReason: String = ""
)
