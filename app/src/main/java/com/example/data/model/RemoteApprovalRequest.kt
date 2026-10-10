package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED
}

@Entity(tableName = "remote_requests")
data class RemoteRequestEntity(
    @PrimaryKey
    val requestId: String,
    val packageName: String,
    val appName: String,
    val requestingDeviceName: String,
    val targetPhoneName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = ApprovalStatus.PENDING.name,
    val challengeOtp: String,
    val ipAddressOrLocation: String = "192.168.1.45 (Local Secure)",
    val resolvedAt: Long? = null
)
