package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCardAlert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SecurityTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityViewModel

@Composable
fun DashboardScreen(
    viewModel: SecurityViewModel,
    onNavigateToVault: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToFindHub: () -> Unit,
    onNavigateToRemote: () -> Unit,
    onNavigateToSensors: () -> Unit,
    onNavigateToDriveDlp: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn by viewModel.isBengali.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val intruderAlert by viewModel.lastIntruderAlert.collectAsState()
    val vaultFiles by viewModel.allVaultFiles.collectAsState()
    val lockedApps by viewModel.lockedApps.collectAsState()
    val pendingRequests by viewModel.pendingRemoteRequests.collectAsState()
    val permissionApps by viewModel.permissionApps.collectAsState()
    val unusualSensors by viewModel.unusualSensorLogs.collectAsState()
    val driveItems by viewModel.allDriveItems.collectAsState()
    val simStatus by viewModel.simStatus.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Dynamic security health score (0-100)
    val healthScore = remember(lockedApps, vaultFiles, permissionApps, driveItems, unusualSensors) {
        var score = 40
        if (lockedApps.any { it.isLocked && it.isFinancial }) score += 20
        if (driveItems.none { it.isLeaking }) score += 15
        if (unusualSensors.isEmpty()) score += 15
        if (vaultFiles.isNotEmpty()) score += 10
        score.coerceIn(0, 100)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SecurityTopBar(
                title = if (isBn) "সিকিউরশিল্ড ভল্ট" else "SecureShield Vault",
                subtitle = if (isBn) "সামরিক মানের সাইবার এনক্রিপশন ও এন্টি-থিফ গার্ড" else "Military-Grade Cryptographic Defense & Anti-Theft",
                isBengali = isBn,
                isVaultLocked = !isVaultUnlocked,
                onToggleLanguage = { viewModel.toggleLanguage() },
                onActivateDisguise = { viewModel.toggleDisguiseCalculator(true) },
                onLockVault = if (isVaultUnlocked) { { viewModel.lockVault() } } else null
            )
        }

        // Intruder or SIM Emergency Banner
        if (intruderAlert != null || simStatus.isTrayEjected) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(DangerRed, WarningAmber))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DangerRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (simStatus.isTrayEjected) Icons.Default.SimCardAlert else Icons.Default.CameraAlt,
                                contentDescription = "Alert",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (simStatus.isTrayEjected) {
                                    (if (isBn) "🚨 সিম ট্রে খোলা বা কার্ড অপসারিত!" else "🚨 SIM Card / Tray Removed!")
                                } else {
                                    (if (isBn) "🚨 অননুমোদিত অ্যাক্সেস শনাক্ত!" else "🚨 Intruder Alert Detected!")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Text(
                                text = if (simStatus.isTrayEjected) {
                                    (if (isBn) "জরুরি এলার্ট ও জিপিএস কোঅর্ডিনেট অন্য ফোনে পাঠানো হয়েছে।" else "Critical emergency alert & GPS sent to 2nd phone.")
                                } else {
                                    intruderAlert ?: ""
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Hero Security Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.secureshield_hero_1791570272380),
                        contentDescription = "Security Vault Door",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.35f
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ShieldGreen)
                                    )
                                    Text(
                                        text = if (isBn) "সিস্টেম সক্রিয় ও সুরক্ষিত" else "SYSTEM SECURED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ShieldGreen,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Text(
                                    text = if (isBn) "নিরাপত্তা স্কোর: $healthScore%" else "Security Health: $healthScore%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(2.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$healthScore%",
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatusPill(label = if (isBn) "AES-256 ভল্ট" else "AES Vault", isActive = true, isBn = isBn)
                            StatusPill(label = if (isBn) "ফাইন্ড হাব জিপিএস" else "GPS Tracker", isActive = true, isBn = isBn)
                            StatusPill(label = if (isBn) "পাওয়ার গার্ড ওটিপি" else "Power Guard", isActive = true, isBn = isBn)
                        }
                    }
                }
            }
        }

        // Section Title: Security Modules
        item {
            Text(
                text = if (isBn) "প্রধান নিরাপত্তা মডিউলসমূহ" else "Core Security Modules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        // Module: Find Hub & Anti-Theft Guard
        item {
            SecurityModuleCard(
                icon = Icons.Default.LocationOn,
                iconColor = NeonCyan,
                title = if (isBn) "ফাইন্ড হাব ও এন্টি-থিফ কমান্ড সেন্টার" else "Find Hub & Anti-Theft Guard",
                description = if (isBn) {
                    "লাইভ লোকেশন, লাস্ট নোন কোঅর্ডিনেট, পাওয়ার-অফ ওটিপি গার্ড ও সিম ট্রে এলার্ট।"
                } else {
                    "Live GPS, last known coordinates, power-off OTP shield & SIM eject alert."
                },
                badgeText = if (isBn) "লাইভ গার্ড" else "LIVE GUARD",
                badgeColor = ShieldGreen,
                onClick = onNavigateToFindHub,
                testTag = "module_findhub_btn"
            )
        }

        // Module: Sensor Telemetry & Anomaly Detector
        item {
            val hasUnusual = unusualSensors.isNotEmpty()
            SecurityModuleCard(
                icon = Icons.Default.Mic,
                iconColor = if (hasUnusual) DangerRed else ShieldGreen,
                title = if (isBn) "মাইক্রোফোন, ক্যামেরা ও কল ট্র্যাকার (চার্ট ও এলার্ট)" else "Mic, Camera & Call Telemetry",
                description = if (isBn) {
                    "কোন অ্যাপ কখন মাইক, ক্যামেরা বা কল ব্যবহার করেছে চার্ট আকারে দেখুন; সন্দেহজনক ব্যবহারে এলার্ট।"
                } else {
                    "Visual charts of sensor usage and instant alerts on suspicious background access."
                },
                badgeText = if (hasUnusual) "${unusualSensors.size} Suspicious" else (if (isBn) "মনিটরড" else "Audited"),
                badgeColor = if (hasUnusual) DangerRed else ShieldGreen,
                onClick = onNavigateToSensors,
                testTag = "module_sensors_btn"
            )
        }

        // Module: Google Drive Cloud DLP
        item {
            val leaking = driveItems.count { it.isLeaking }
            SecurityModuleCard(
                icon = Icons.Default.Cloud,
                iconColor = if (leaking > 0) DangerRed else NeonCyan,
                title = if (isBn) "গুগল ড্রাইভ লিকেজ গার্ড (Cloud DLP)" else "Google Drive DLP Leak Guardian",
                description = if (isBn) {
                    "গুগল ড্রাইভে থাকা ফাইল যেন অননুমোদিতভাবে পাবলিক শেয়ার না হয় তা নজরদারি ও ১-ট্যাপে লক।"
                } else {
                    "Detects and revokes accidental public share links on Google Drive."
                },
                badgeText = if (leaking > 0) "$leaking Leaking" else (if (isBn) "নিরাপদ" else "Protected"),
                badgeColor = if (leaking > 0) DangerRed else ShieldGreen,
                onClick = onNavigateToDriveDlp,
                testTag = "module_drive_dlp_btn"
            )
        }

        // Module: Secure Vault
        item {
            SecurityModuleCard(
                icon = Icons.Default.Folder,
                iconColor = NeonCyan,
                title = if (isBn) "গোপন ডাটা ভল্ট (ছবি, ভিডিও, অডিও, পিডিএফ)" else "Encrypted Vault (Photos, Audio, PDF)",
                description = if (isBn) {
                    "মোট ${vaultFiles.size} টি সুরক্ষিত ফাইল সংরক্ষিত। শক্তিশালী পাসওয়ার্ড দিয়ে এনক্রিপ্ট করা।"
                } else {
                    "${vaultFiles.size} protected files. Encrypted with master passcode."
                },
                badgeText = if (isVaultUnlocked) (if (isBn) "আনলকড" else "UNLOCKED") else (if (isBn) "লকড" else "LOCKED"),
                badgeColor = if (isVaultUnlocked) WarningAmber else ShieldGreen,
                onClick = onNavigateToVault,
                testTag = "module_vault_btn"
            )
        }

        // Module: Financial App Lock & Dynamic OTP
        item {
            val lockedFinCount = lockedApps.count { it.isLocked }
            SecurityModuleCard(
                icon = Icons.Default.Lock,
                iconColor = WarningAmber,
                title = if (isBn) "আর্থিক অ্যাপ লকার ও রেনডম ওটিপি" else "Financial App Lock & Random OTP",
                description = if (isBn) {
                    "বিকাশ, নগদ বা ব্যাংক অ্যাপে ঢুকতে পাসওয়ার্ড এবং প্রতিবার পরিবর্তনশীল ওটিপি চাইবে।"
                } else {
                    "Requires Master PIN + 30s Random OTP before opening banking apps."
                },
                badgeText = if (isBn) "$lockedFinCount টি সুরক্ষিত" else "$lockedFinCount Secured",
                badgeColor = WarningAmber,
                onClick = onNavigateToAppLock,
                testTag = "module_applock_btn"
            )
        }

        // Module: Remote 2-Device Approval
        item {
            SecurityModuleCard(
                icon = Icons.Default.NotificationsActive,
                iconColor = ElectricIndigo,
                title = if (isBn) "অন্য ফোনে পুশ নোটিফিকেশন ও অনুমোদন" else "2-Phone Remote Push Approval",
                description = if (isBn) {
                    "অন্য ফোনে নোটিফিকেশন যাবে এবং সেই অনুমোদন (Approve) না করা পর্যন্ত অ্যাপ খুলবে না।"
                } else {
                    "Sends instant push to your 2nd phone; apps won't open without remote approval."
                },
                badgeText = if (pendingRequests.isNotEmpty()) "${pendingRequests.size} Pending" else (if (isBn) "সংযুক্ত" else "Paired"),
                badgeColor = if (pendingRequests.isNotEmpty()) DangerRed else ShieldGreen,
                onClick = onNavigateToRemote,
                testTag = "module_remote_btn"
            )
        }

        // Module: App Permission Auditor
        item {
            val highRiskCount = permissionApps.count { it.dangerousPermissions.size >= 3 }
            SecurityModuleCard(
                icon = Icons.Default.Policy,
                iconColor = ShieldGreen,
                title = if (isBn) "অ্যাপ পারমিশন অডিট ও অপ্রয়োজনীয় অনুমতি রিমুভ" else "Permission Auditor & Revoker",
                description = if (isBn) {
                    "কোন অ্যাপ কী কী পারমিশন নিয়ে রেখেছে তা পরীক্ষা করে অপ্রয়োজনীয় ক্যামেরা, মাইক, এসএমএস পারমিশন বন্ধ করুন।"
                } else {
                    "Audits all apps for SMS, Camera, Location access and helps revoke unneeded rights."
                },
                badgeText = if (highRiskCount > 0) "$highRiskCount High Risk" else "Audited",
                badgeColor = if (highRiskCount > 0) DangerRed else ShieldGreen,
                onClick = onNavigateToPermissions,
                testTag = "module_permissions_btn"
            )
        }

        // Module: File Share & Data Leakage Logs
        item {
            SecurityModuleCard(
                icon = Icons.Default.ReceiptLong,
                iconColor = NeonCyan,
                title = if (isBn) "ফাইল শেয়ার ট্র্যাকিং ও নিরাপত্তা অডিট লগ" else "File Share Tracking & Audit Trail",
                description = if (isBn) {
                    "কোথাও কোনো ফাইল শেয়ার হলে অ্যাপস লগ থাকবে এবং অননুমোদিত অ্যাক্সেস ট্র্যাকিং হবে।"
                } else {
                    "Comprehensive forensic audit trail of all shared files and access attempts."
                },
                badgeText = if (isBn) "লাইভ অডিট" else "Live Audit",
                badgeColor = NeonCyan,
                onClick = onNavigateToLogs,
                testTag = "module_logs_btn"
            )
        }

        // Settings Section
        item {
            Text(
                text = if (isBn) "নিরাপত্তা কন্ট্রোল ও সেটিংস" else "Security Controls & Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Change PIN action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showChangePinDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = "PIN", tint = NeonCyan)
                            Column {
                                Text(
                                    text = if (isBn) "মাস্টার পাসওয়ার্ড / পিন পরিবর্তন" else "Change Master Password / PIN",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isBn) "বর্তমান ডিফল্ট: 1234" else "Current default: 1234",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Calculator Disguise Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = "Disguise", tint = WarningAmber)
                            Column {
                                Text(
                                    text = if (isBn) "ক্যালকুলেটর ছদ্মবেশ (Disguise)" else "Calculator Disguise",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isBn) "অ্যাপটি কাজের ক্যালকুলেটর হিসেবে দেখাবে" else "Shows real calculator to hide vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.prefs.isDisguiseCalculatorEnabled(),
                            onCheckedChange = { viewModel.toggleDisguiseCalculator(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = WarningAmber)
                        )
                    }

                    // Anti-Power-Off Protection Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Power Guard", tint = ShieldGreen)
                            Column {
                                Text(
                                    text = if (isBn) "পাওয়ার অফ ওটিপি গার্ড সক্রিয়" else "Anti-Power-Off Protection",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (isBn) "ফোন বন্ধ করতে গেলে সেকেন্ডারি ফোনে ওটিপি যাবে" else "Requires 2nd phone OTP before shutdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.prefs.isAntiPowerOffEnabled(),
                            onCheckedChange = { viewModel.prefs.setAntiPowerOffEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = ShieldGreen)
                        )
                    }
                }
            }
        }
    }

    // Change Master PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text(if (isBn) "নতুন মাস্টার পিন সেট করুন" else "Set New Master PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6) newPinInput = it },
                        label = { Text(if (isBn) "নতুন ৪-৬ ডিজিট পিন" else "New 4-6 digit PIN") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text(if (isBn) "পুনরায় লিখুন" else "Confirm PIN") },
                        singleLine = true
                    )
                    if (pinError != null) {
                        Text(pinError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinError = if (isBn) "কমপক্ষে ৪ ডিজিট হতে হবে!" else "Must be at least 4 digits!"
                        } else if (newPinInput != confirmPinInput) {
                            pinError = if (isBn) "পিন মেলেনি!" else "PINs do not match!"
                        } else {
                            viewModel.setMasterPin(newPinInput)
                            showChangePinDialog = false
                            newPinInput = ""
                            confirmPinInput = ""
                            pinError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (isBn) "সংরক্ষণ করুন" else "Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isActive) ShieldGreen else WarningAmber)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun SecurityModuleCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

