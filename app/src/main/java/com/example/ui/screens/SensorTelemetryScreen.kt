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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorAccessLogEntity
import com.example.data.model.SensorType
import com.example.data.security.PermissionAuditorHelper
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
fun SensorTelemetryScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn by viewModel.isBengali.collectAsState()
    val allLogs by viewModel.allSensorLogs.collectAsState()
    val unusualLogs by viewModel.unusualSensorLogs.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val micCount = allLogs.count { it.sensorType == SensorType.MICROPHONE.name }
    val camCount = allLogs.count { it.sensorType == SensorType.CAMERA.name }
    val callCount = allLogs.count { it.sensorType == SensorType.CALL.name }
    val totalCount = (micCount + camCount + callCount).coerceAtLeast(1)

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("sensor_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isBn) "মাইক্রোফোন, ক্যামেরা ও কল ট্র্যাকার" else "Sensor Telemetry & Call Monitor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "লাইভ সেন্সর ব্যবহার চার্ট ও অস্বাভাবিক এক্সেস অ্যালার্ট" else "Live Access Charts & Anomaly Alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Anomaly Warning Banner if unusual logs exist
            if (unusualLogs.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(DangerRed, WarningAmber))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(DangerRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color.White, modifier = Modifier.size(20.dp))
                                }

                                Column {
                                    Text(
                                        text = if (isBn) "🚨 অস্বাভাবিক সেন্সর এক্সেস শনাক্ত (${unusualLogs.size} টি)!" else "🚨 Suspicious Sensor Access Detected (${unusualLogs.size})!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                    Text(
                                        text = if (isBn) "অ্যাপ গোপনে ব্যাকগ্রাউন্ডে মাইক বা কল শোনার চেষ্টা করছে।" else "Apps attempting hidden background audio or call interception.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            unusualLogs.forEach { unusual ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${unusual.appName} (${unusual.sensorType})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmber
                                        )
                                        Text(
                                            text = unusual.unusualReason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            PermissionAuditorHelper.openAppSettings(context, unusual.packageName)
                                            viewModel.logPermissionCleanup(unusual.appName, unusual.packageName)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(if (isBn) "ব্লক করুন" else "Block", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Visual Sensor Usage Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isBn) "সেন্সর ব্যবহারের পরিসংখ্যান চার্ট" else "Sensor Telemetry Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Multi-color segmented bar chart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            if (micCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((micCount.toFloat() / totalCount).coerceAtLeast(0.05f))
                                        .fillMaxSize()
                                        .background(ShieldGreen)
                                )
                            }
                            if (camCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((camCount.toFloat() / totalCount).coerceAtLeast(0.05f))
                                        .fillMaxSize()
                                        .background(NeonCyan)
                                )
                            }
                            if (callCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight((callCount.toFloat() / totalCount).coerceAtLeast(0.05f))
                                        .fillMaxSize()
                                        .background(WarningAmber)
                                )
                            }
                        }

                        // Legend & Detailed counts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SensorStatPill(
                                icon = Icons.Default.Mic,
                                label = if (isBn) "মাইক্রোফোন" else "Microphone",
                                count = micCount,
                                color = ShieldGreen
                            )
                            SensorStatPill(
                                icon = Icons.Default.CameraAlt,
                                label = if (isBn) "ক্যামেরা" else "Camera",
                                count = camCount,
                                color = NeonCyan
                            )
                            SensorStatPill(
                                icon = Icons.Default.Call,
                                label = if (isBn) "কল ও ডায়লার" else "Phone Calls",
                                count = callCount,
                                color = WarningAmber
                            )
                        }
                    }
                }
            }

            // Platform Policy Notice
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Policy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "অ্যান্ড্রয়েড পলিসি ও সেন্সর নিরাপত্তা তথ্য:" else "Android OS Platform Capability Notice:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = if (isBn) {
                                "অ্যান্ড্রয়েড লিনাক্স সিকিউরিটি স্যান্ডবক্সিংয়ের কারণে সাধারণ কোনো থার্ড-পার্টি অ্যাপ অন্য কোনো অ্যাপের অডিও বা ক্যামেরা স্ট্রিম সরাসরি শুনতে পারে না। ইনস্টল করা অ্যাপগুলোর বিপজ্জনক পারমিশন স্ট্যাটাস দেখতে পারমিশন অডিটর ব্যবহার করুন।"
                            } else {
                                "Android OS process isolation and sandboxing prevent standard apps from intercepting other applications' live microphone or camera streams. Use Permission Auditor to inspect granted hardware permissions."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real-time access history
            item {
                Text(
                    text = if (isBn) "সাম্প্রতিক সেন্সর ব্যবহারের লগ (${allLogs.size})" else "Recent Sensor Access Events (${allLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (allLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "No Events", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Text(
                                text = if (isBn) "কোনো সেন্সর অনুপ্রবেশ রেকর্ড নেই" else "No Unauthorized Sensor Access Recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(allLogs, key = { it.id }) { log ->
                    SensorAccessLogItem(log = log, isBn = isBn)
                }
            }
        }
    }
}

@Composable
fun SensorStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SensorAccessLogItem(
    log: SensorAccessLogEntity,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (log.sensorType) {
        SensorType.MICROPHONE.name -> ShieldGreen to Icons.Default.Mic
        SensorType.CAMERA.name -> NeonCyan to Icons.Default.CameraAlt
        else -> WarningAmber to Icons.Default.Call
    }

    val dateFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isUnusual) DangerRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (log.isUnusual) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DangerRed)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = log.sensorType, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = log.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (log.isBackground) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isBn) "ব্যাকগ্রাউন্ড" else "Background",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarningAmber,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Text(
                    text = "$timeStr • ব্যবহার: ${log.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.isUnusual) {
                    Text(
                        text = "⚠️ ${log.unusualReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = log.sensorType,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
