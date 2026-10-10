package com.example.data.security

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.data.model.AppPermissionInfo
import com.example.data.model.RiskLevel

object PermissionAuditorHelper {

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    fun scanInstalledApps(context: Context): List<AppPermissionInfo> {
        val pm = context.packageManager
        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList<PackageInfo>()
        }

        val result = mutableListOf<AppPermissionInfo>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                pkg.packageName
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val requestedPerms = pkg.requestedPermissions?.toList() ?: emptyList()

            val dangerous = requestedPerms.filter { DANGEROUS_PERMISSIONS.contains(it) }

            val reasons = mutableListOf<String>()
            if (dangerous.any { it.contains("SMS") }) {
                reasons.add("এসএমএস ও ওটিপি পড়ার অনুমতি (SMS Access)")
            }
            if (dangerous.any { it.contains("CAMERA") }) {
                reasons.add("ক্যামেরা ব্যবহারের অনুমতি (Camera Access)")
            }
            if (dangerous.any { it.contains("RECORD_AUDIO") }) {
                reasons.add("মাইক্রোফোন ও অডিও রেকর্ডিং (Audio Access)")
            }
            if (dangerous.any { it.contains("LOCATION") }) {
                reasons.add("লাইভ জিপিএস লোকেশন ট্র্যাকিং (Location Tracking)")
            }
            if (dangerous.any { it.contains("CONTACTS") }) {
                reasons.add("সকল ফোন কন্টাক্ট পড়ার অনুমতি (Contacts Access)")
            }

            val riskLevel = when {
                dangerous.size >= 4 || (dangerous.any { it.contains("SMS") } && dangerous.any { it.contains("CONTACTS") }) -> RiskLevel.HIGH
                dangerous.size >= 2 -> RiskLevel.MEDIUM
                else -> RiskLevel.SAFE
            }

            result.add(
                AppPermissionInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                    requestedPermissions = requestedPerms,
                    dangerousPermissions = dangerous,
                    riskLevel = riskLevel,
                    riskReasons = reasons
                )
            )
        }

        // Add prominent preset financial & privacy apps if they are not in the list (e.g. on emulator)
        val defaultKnown = listOf(
            AppPermissionInfo(
                packageName = "com.bKash.customerapp",
                appName = "bKash (বিকাশ)",
                isSystemApp = false,
                requestedPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS"),
                dangerousPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS"),
                riskLevel = RiskLevel.HIGH,
                riskReasons = listOf("এসএমএস ও ওটিপি পড়ার অনুমতি", "সকল ফোন কন্টাক্ট পড়ার অনুমতি", "লাইভ লোকেশন ও ক্যামেরা")
            ),
            AppPermissionInfo(
                packageName = "com.konasl.nagad",
                appName = "Nagad (নগদ)",
                isSystemApp = false,
                requestedPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.ACCESS_FINE_LOCATION"),
                dangerousPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.ACCESS_FINE_LOCATION"),
                riskLevel = RiskLevel.HIGH,
                riskReasons = listOf("এসএমএস ও ব্যালেন্স ট্রানজ্যাকশন অনুমতি", "ক্যামেরা ও লাইভ লোকেশন")
            ),
            AppPermissionInfo(
                packageName = "com.dbbl.nexus.pay",
                appName = "NexusPay DBBL",
                isSystemApp = false,
                requestedPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.READ_PHONE_STATE"),
                dangerousPermissions = listOf("android.permission.CAMERA", "android.permission.READ_SMS", "android.permission.READ_PHONE_STATE"),
                riskLevel = RiskLevel.HIGH,
                riskReasons = listOf("এসএমএস রিডিং ও ফোন স্টেট মনিটরিং")
            ),
            AppPermissionInfo(
                packageName = "com.binance.dev",
                appName = "Binance Crypto",
                isSystemApp = false,
                requestedPermissions = listOf("android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION"),
                dangerousPermissions = listOf("android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION"),
                riskLevel = RiskLevel.MEDIUM,
                riskReasons = listOf("ক্রিপ্টো ওয়ালেট ক্যামেরা ও লোকেশন এক্সেস")
            )
        )

        val existingPkgNames = result.map { it.packageName }.toSet()
        val combined = result.toMutableList()
        for (known in defaultKnown) {
            if (!existingPkgNames.contains(known.packageName)) {
                combined.add(0, known)
            }
        }

        // Sort: High risk first, then Medium, then Safe
        return combined.sortedWith(
            compareBy<AppPermissionInfo> {
                when (it.riskLevel) {
                    RiskLevel.HIGH -> 0
                    RiskLevel.MEDIUM -> 1
                    RiskLevel.SAFE -> 2
                }
            }.thenBy { it.appName }
        )
    }

    /**
     * Opens Android System App Details settings where user can revoke permissions immediately
     */
    fun openAppSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens global Android Permission Manager (Android 11+)
     */
    fun openPermissionManager(context: Context) {
        try {
            val intent = Intent("android.intent.action.MANAGE_PERMISSIONS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
