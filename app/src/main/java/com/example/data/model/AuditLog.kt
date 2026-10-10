package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuditEventType(val labelBn: String, val labelEn: String) {
    FILE_SHARED("ফাইল শেয়ার", "File Shared"),
    FILE_ENCRYPTED("ফাইল এনক্রিপ্ট", "File Encrypted"),
    FILE_DECRYPTED("ফাইল খোলা হয়েছে", "File Opened/Exported"),
    APP_UNLOCKED("অ্যাপ আনলক সফল", "App Unlocked"),
    APP_BLOCKED("অননুমোদিত অ্যাক্সেস রোধ", "Unauthorized Access Blocked"),
    REMOTE_APPROVED("অন্য ফোন থেকে অনুমোদিত", "Remote Approved"),
    REMOTE_REJECTED("অন্য ফোন থেকে প্রত্যাখ্যাত", "Remote Rejected"),
    INTRUDER_ALERT("ভুল পাসওয়ার্ড সতর্কতা", "Intruder Alert"),
    PERMISSION_AUDIT("পারমিশন অডিট সম্পন্ন", "Permission Audit Run"),
    PERMISSION_CLEANUP("পারমিশন সেটিংসে পাঠানো", "Permission Cleanup")
}

enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL,
    SUCCESS
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String, // String of AuditEventType
    val title: String,
    val details: String,
    val targetItem: String, // file name or app name
    val timestamp: Long = System.currentTimeMillis(),
    val severity: String = AuditSeverity.INFO.name,
    val intruderPhotoPath: String? = null
)
