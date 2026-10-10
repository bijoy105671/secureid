package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditEventType
import com.example.data.model.AuditLogEntity
import com.example.data.model.AuditSeverity
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn by viewModel.isBengali.collectAsState()
    val allLogs by viewModel.auditLogs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredLogs = allLogs.filter { log ->
        val typeMatch = selectedTypeFilter == null || log.eventType == selectedTypeFilter
        val searchMatch = searchQuery.isEmpty() ||
                log.title.contains(searchQuery, ignoreCase = true) ||
                log.details.contains(searchQuery, ignoreCase = true) ||
                log.targetItem.contains(searchQuery, ignoreCase = true)
        typeMatch && searchMatch
    }

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
                            modifier = Modifier.testTag("logs_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = if (isBn) "ফাইল শেয়ার ও অডিট ট্রেইল" else "File Share & Audit Logs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "কোথাও ফাইল শেয়ার হলে সংরক্ষিত ফরেনসিক লগ" else "Forensic Leakage Audit Trail",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }

                    if (allLogs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_logs_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isBn) "ফাইল বা ইভেন্ট নাম দিয়ে খুঁজুন..." else "Search by file or event...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("log_search_field"),
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
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text(if (isBn) "সকল (${allLogs.size})" else "All (${allLogs.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    item {
                        val shareCount = allLogs.count { it.eventType == AuditEventType.FILE_SHARED.name }
                        FilterChip(
                            selected = selectedTypeFilter == AuditEventType.FILE_SHARED.name,
                            onClick = { selectedTypeFilter = AuditEventType.FILE_SHARED.name },
                            label = { Text(if (isBn) "ফাইল শেয়ার ($shareCount)" else "Shared ($shareCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    item {
                        val intruderCount = allLogs.count { it.eventType == AuditEventType.INTRUDER_ALERT.name }
                        FilterChip(
                            selected = selectedTypeFilter == AuditEventType.INTRUDER_ALERT.name,
                            onClick = { selectedTypeFilter = AuditEventType.INTRUDER_ALERT.name },
                            label = { Text(if (isBn) "অনুপ্রবেশ সতর্কতা ($intruderCount)" else "Intruders ($intruderCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    item {
                        val remoteCount = allLogs.count { it.eventType.contains("REMOTE") }
                        FilterChip(
                            selected = selectedTypeFilter == AuditEventType.REMOTE_APPROVED.name,
                            onClick = { selectedTypeFilter = AuditEventType.REMOTE_APPROVED.name },
                            label = { Text(if (isBn) "রিমোট অনুমোদন" else "Remote Approvals") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricIndigo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Log Items or Empty
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "No Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Text(
                                text = if (isBn) "কোনো অডিট লগ রেকর্ড নেই" else "No Audit Log Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) {
                                    "ভল্ট থেকে ফাইল শেয়ার করলে অথবা অ্যাপ লক পরীক্ষা করলে এখানে স্বয়ংক্রিয়ভাবে রেকর্ড থাকবে।"
                                } else {
                                    "Sharing files or testing app unlocks will automatically append records here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AuditLogItemCard(log = log, isBn = isBn)
                }
            }
        }
    }

    // Clear Logs Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isBn) "সকল অডিট লগ মুছে ফেলবেন?" else "Clear All Audit Logs?") },
            text = {
                Text(
                    if (isBn) "এই পদক্ষেপের পর পূর্ববর্তী সকল ফাইল শেয়ার ও অ্যাক্সেস ট্রেইল মুছে যাবে।" else "This will permanently delete all past security audit records."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAuditLogs()
                        showClearDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isBn) "অডিট লগ মুছে ফেলা হয়েছে" else "Logs cleared")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isBn) "মুছে ফেলুন" else "Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AuditLogItemCard(
    log: AuditLogEntity,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val severity = try {
        AuditSeverity.valueOf(log.severity)
    } catch (_: Exception) {
        AuditSeverity.INFO
    }

    val (badgeColor, icon) = when (severity) {
        AuditSeverity.CRITICAL -> DangerRed to Icons.Default.Warning
        AuditSeverity.WARNING -> WarningAmber to Icons.Default.Warning
        AuditSeverity.SUCCESS -> ShieldGreen to Icons.Default.CheckCircle
        AuditSeverity.INFO -> NeonCyan to Icons.Default.Info
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = log.eventType,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.eventType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Intruder photo badge if present
                if (log.intruderPhotoPath != null) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DangerRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Snapshot", tint = DangerRed, modifier = Modifier.size(12.dp))
                        Text(
                            text = if (isBn) "অনুপ্রবেশকারীর ছবি সংরক্ষিত (Front Camera)" else "Intruder Front Camera Snapshot Logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = DangerRed,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
                    Text(
                        text = "টার্গেট: ${log.targetItem}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
