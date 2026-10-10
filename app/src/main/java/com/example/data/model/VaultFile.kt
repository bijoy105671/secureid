package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileCategory(val labelBn: String, val labelEn: String) {
    PHOTO("ছবি", "Photos"),
    VIDEO("ভিডিও", "Videos"),
    AUDIO("অডিও", "Audio"),
    DOCUMENT("পিডিএফ / ফাইল", "PDF & Documents"),
    NOTE("গোপন নোট", "Secure Notes")
}

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // String representation of FileCategory
    val filePath: String,
    val fileSizeFormatted: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val description: String = "",
    val secretContent: String = "" // For secure notes or decrypted text
)
