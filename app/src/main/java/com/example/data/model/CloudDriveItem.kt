package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DriveShareStatus(val labelBn: String, val labelEn: String) {
    PRIVATE("সম্পূর্ণ ব্যক্তিগত", "Strictly Private"),
    RESTRICTED("নির্দিষ্ট ব্যক্তিদের সাথে", "Restricted Access"),
    PUBLIC_LINK("পাবলিক লিঙ্ক অন (ঝুঁকিপূর্ণ)", "Public Link Active (Leak Risk)"),
    ANYONE_WITH_LINK("যে কেউ লিঙ্ক দিয়ে দেখতে পারে", "Anyone with link (Leaked)")
}

@Entity(tableName = "cloud_drive_items")
data class CloudDriveItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val mimeType: String,
    val fileSizeFormatted: String,
    val shareStatus: String, // DriveShareStatus.name
    val isLeaking: Boolean = false,
    val sharedWithEmails: String = "",
    val lastScannedTimestamp: Long = System.currentTimeMillis()
)
