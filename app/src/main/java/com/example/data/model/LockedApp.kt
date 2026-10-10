package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isFinancial: Boolean = false,
    val isLocked: Boolean = true,
    val requirePin: Boolean = true,
    val requireOtp: Boolean = true,
    val requireRemoteApproval: Boolean = true,
    val lastAccessedTimestamp: Long = 0L,
    val unlockGraceSeconds: Int = 30
)
