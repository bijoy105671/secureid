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

/**
 * Hardened Permission Auditor for SHIELD X.
 *
 * Implements:
 * - Scans real installed packages on the device (no fabricated apps).
 * - Distinguishes between requested permissions (manifest) and ACTUALLY GRANTED permissions.
 * - Accurately categorizes high-risk hardware sensors (Camera, Microphone, Call Logs, Contacts, SMS, Location).
 * - Honest platform disclosure: non-system apps cannot intercept other apps' audio or camera streams.
 */
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
        "android.permission.READ_CALL_LOG",
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
            val flags = pkg.requestedPermissionsFlags

            // Separate granted dangerous permissions from purely requested ones
            val grantedDangerous = mutableListOf<String>()
            val declaredDangerous = mutableListOf<String>()

            for (i in requestedPerms.indices) {
                val perm = requestedPerms[i]
                if (DANGEROUS_PERMISSIONS.contains(perm)) {
                    declaredDangerous.add(perm)
                    val isGranted = if (flags != null && i < flags.size) {
                        (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                    } else {
                        false
                    }
                    if (isGranted) {
                        grantedDangerous.add(perm)
                    }
                }
            }

            // Only report risk if dangerous permissions are actually GRANTED
            val reasons = mutableListOf<String>()
            if (grantedDangerous.any { it.contains("SMS") }) {
                reasons.add("এসএমএস ও ওটিপি পড়ার এক্সেস সক্রিয় (Granted SMS)")
            }
            if (grantedDangerous.any { it.contains("CAMERA") }) {
                reasons.add("ক্যামেরা ব্যবহারের অনুমতি সক্রিয় (Granted Camera)")
            }
            if (grantedDangerous.any { it.contains("RECORD_AUDIO") }) {
                reasons.add("মাইক্রোফোন রেকর্ডিং পারমিশন সক্রিয় (Granted Mic)")
            }
            if (grantedDangerous.any { it.contains("LOCATION") }) {
                reasons.add("লাইভ জিপিএস লোকেশন পারমিশন সক্রিয় (Granted Location)")
            }
            if (grantedDangerous.any { it.contains("CALL_LOG") || it.contains("READ_PHONE_STATE") }) {
                reasons.add("কল লগ ও ফোন স্টেট পারমিশন সক্রিয় (Granted Call Log)")
            }
            if (grantedDangerous.any { it.contains("CONTACTS") }) {
                reasons.add("ফোন কন্টাক্ট পড়ার অনুমতি সক্রিয় (Granted Contacts)")
            }

            val riskLevel = when {
                grantedDangerous.size >= 4 || (grantedDangerous.any { it.contains("SMS") } && grantedDangerous.any { it.contains("CONTACTS") }) -> RiskLevel.HIGH
                grantedDangerous.size >= 2 -> RiskLevel.MEDIUM
                grantedDangerous.size == 1 -> RiskLevel.SAFE
                else -> RiskLevel.SAFE
            }

            result.add(
                AppPermissionInfo(
                    packageName = pkg.packageName,
                    appName = appName,
                    isSystemApp = isSystem,
                    requestedPermissions = requestedPerms,
                    dangerousPermissions = grantedDangerous, // Report granted dangerous permissions
                    riskLevel = riskLevel,
                    riskReasons = reasons
                )
            )
        }

        // Return real installed apps sorted by granted risk level
        return result.sortedWith(
            compareBy<AppPermissionInfo> {
                when (it.riskLevel) {
                    RiskLevel.HIGH -> 0
                    RiskLevel.MEDIUM -> 1
                    RiskLevel.SAFE -> 2
                }
            }.thenBy { it.isSystemApp }
                .thenBy { it.appName }
        )
    }

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
