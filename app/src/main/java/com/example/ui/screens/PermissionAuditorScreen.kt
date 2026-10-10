package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPermissionInfo
import com.example.data.model.RiskLevel
import com.example.data.security.PermissionAuditorHelper
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch

@Composable
fun PermissionAuditorScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn by viewModel.isBengali.collectAsState()
    val permissionApps by viewModel.permissionApps.collectAsState()
    val isScanning by viewModel.isScanningPermissions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }
    var showAndroidPolicyDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredApps = permissionApps.filter { app ->
        val levelMatch = selectedFilter == null || app.riskLevel == selectedFilter
        val searchMatch = searchQuery.isEmpty() || app.appName.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true)
        levelMatch && searchMatch
    }

    val highRiskCount = permissionApps.count { it.riskLevel == RiskLevel.HIGH }
    val mediumRiskCount = permissionApps.count { it.riskLevel == RiskLevel.MEDIUM }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("perm_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = if (isBn) "অ্যাপ পারমিশন অডিটর" else "App Permission Auditor",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "অপ্রয়োজনীয় ক্যামেরা, মাইক, এসএমএস পারমিশন রিমুভার" else "Privacy Risk Scanner & Permission Revoker",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShieldGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.scanAppPermissions() },
                        modifier = Modifier.testTag("rescan_perms_btn")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NeonCyan)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = NeonCyan)
                        }
                    }
                }
            }

            // Overview Summary Card
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "পারমিশন অডিট সারাংশ" else "Permission Audit Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(onClick = { showAndroidPolicyDialog = true }) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = NeonCyan)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RiskStatBox(
                                label = if (isBn) "উচ্চ ঝুঁকি" else "High Risk",
                                count = highRiskCount,
                                color = DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                            RiskStatBox(
                                label = if (isBn) "মাঝারি ঝুঁকি" else "Medium",
                                count = mediumRiskCount,
                                color = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            RiskStatBox(
                                label = if (isBn) "মোট স্ক্যানড" else "Scanned",
                                count = permissionApps.size,
                                color = ShieldGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Android Permission Manager global shortcut
                        Button(
                            onClick = {
                                PermissionAuditorHelper.openPermissionManager(context)
                                viewModel.logPermissionCleanup("System Permission Controller")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_system_perm_mgr_btn")
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = "Policy", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "অ্যান্ড্রয়েড পারমিশন ম্যানেজার খুলুন" else "Open Android Permission Manager",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isBn) "অ্যাপের নাম দিয়ে খুঁজুন..." else "Search app by name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("perm_search_field"),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text(if (isBn) "সকল (${permissionApps.size})" else "All (${permissionApps.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == RiskLevel.HIGH,
                            onClick = { selectedFilter = RiskLevel.HIGH },
                            label = { Text(if (isBn) "উচ্চ ঝুঁকি ($highRiskCount)" else "High Risk ($highRiskCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == RiskLevel.MEDIUM,
                            onClick = { selectedFilter = RiskLevel.MEDIUM },
                            label = { Text(if (isBn) "মাঝারি ঝুঁকি ($mediumRiskCount)" else "Medium Risk ($mediumRiskCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WarningAmber,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == RiskLevel.SAFE,
                            onClick = { selectedFilter = RiskLevel.SAFE },
                            label = { Text(if (isBn) "নিরাপদ" else "Safe") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShieldGreen,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // App items list
            items(filteredApps, key = { it.packageName }) { app ->
                AppPermissionItemCard(
                    app = app,
                    isBn = isBn,
                    onRevokePermissions = {
                        PermissionAuditorHelper.openAppSettings(context, app.packageName)
                        viewModel.logPermissionCleanup(app.appName)
                        scope.launch {
                            val msg = if (isBn) {
                                "${app.appName} এর সিস্টেম সেটিংস খোলা হয়েছে। অপ্রয়োজনীয় পারমিশন রিমুভ করুন।"
                            } else {
                                "Opened ${app.appName} settings to revoke permissions."
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )
            }
        }
    }

    // Android Policy Guidance Dialog
    if (showAndroidPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showAndroidPolicyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Security", tint = NeonCyan)
                    Text(if (isBn) "অটো পারমিশন রিমুভার ও অ্যান্ড্রয়েড পলিসি" else "Permission Revoke & OS Policy")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) {
                            "📌 অ্যান্ড্রয়েড ওএস এর নিরাপত্তা কাঠামো (Sandboxing) অনুযায়ী, কোনো অ্যাপ ব্যবহারকারীর অজ্ঞাতে অন্য অ্যাপের ব্যাকগ্রাউন্ড পারমিশন স্বয়ংক্রিয়ভাবে মুছে ফেলতে পারে না—যাতে কোনো ভাইরাস হঠাৎ প্রয়োজনীয় ব্যাংকিং বা এমার্জেন্সি অ্যাপের ক্ষতি না করতে পারে।\n\n🛡️ সিকিউরশিল্ড প্রতিটি অ্যাপের অপ্রয়োজনীয় অনুমতি (ক্যামেরা, এসএমএস, কন্টাক্ট, লোকেশন) শনাক্ত করে এবং ১-ট্যাপে সরাসরি সিস্টেম অ্যাপ ইনফো ও অ্যান্ড্রয়েড পারমিশন ম্যানেজারে নিয়ে যায় যাতে সহজে অপ্রয়োজনীয় অনুমতি বন্ধ করা যায়।"
                        } else {
                            "📌 Under Android OS security architecture, apps cannot silently strip permissions from other apps without user consent, preventing malicious tampering.\n\n🛡️ SecureShield audits risky permissions (SMS, Camera, Mic, GPS) and provides direct one-tap shortcuts to Android's native Permission Controller to revoke permissions seamlessly."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAndroidPolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (isBn) "বুঝেছি" else "Understood", color = Color.Black)
                }
            }
        )
    }
}

@Composable
fun RiskStatBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AppPermissionItemCard(
    app: AppPermissionInfo,
    isBn: Boolean,
    onRevokePermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeColor, badgeText) = when (app.riskLevel) {
        RiskLevel.HIGH -> DangerRed to (if (isBn) "উচ্চ ঝুঁকি" else "High Risk")
        RiskLevel.MEDIUM -> WarningAmber to (if (isBn) "মাঝারি ঝুঁকি" else "Medium")
        RiskLevel.SAFE -> ShieldGreen to (if (isBn) "নিরাপদ" else "Safe")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("perm_card_${app.packageName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: App Name, Package & Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (app.riskLevel) {
                                RiskLevel.HIGH -> Icons.Default.Warning
                                RiskLevel.MEDIUM -> Icons.Default.Info
                                RiskLevel.SAFE -> Icons.Default.CheckCircle
                            },
                            contentDescription = app.riskLevel.name,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontSize = 11.sp
                    )
                }
            }

            // Dangerous Permissions Tags
            if (app.dangerousPermissions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isBn) "শনাক্তকৃত সংবেদনশীল পারমিশন (${app.dangerousPermissions.size} টি):" else "Sensitive Permissions (${app.dangerousPermissions.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )

                    app.riskReasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⚠️", fontSize = 11.sp)
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Revoke Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onRevokePermissions,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (app.riskLevel == RiskLevel.HIGH) DangerRed else NeonCyan
                    ),
                    modifier = Modifier.testTag("revoke_btn_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "অনুমতি বাতিল করুন (Revoke)" else "Revoke Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
