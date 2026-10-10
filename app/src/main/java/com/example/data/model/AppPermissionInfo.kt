package com.example.data.model

enum class RiskLevel(val labelBn: String, val labelEn: String) {
    HIGH("উচ্চ ঝুঁকি", "High Risk"),
    MEDIUM("মাঝারি ঝুঁকি", "Medium Risk"),
    SAFE("নিরাপদ", "Safe")
}

data class AppPermissionInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val requestedPermissions: List<String>,
    val dangerousPermissions: List<String>,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>
)
