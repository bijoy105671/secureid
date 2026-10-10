package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SimCardAlert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PowerOffChallenge
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
fun FindHubScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn by viewModel.isBengali.collectAsState()
    val locationState by viewModel.lastKnownLocation.collectAsState()
    val simStatus by viewModel.simStatus.collectAsState()
    val isSirenActive by viewModel.isSirenAlarmActive.collectAsState()
    val powerOffChallenge by viewModel.activePowerOffChallenge.collectAsState()
    val isFakeShutdownActive by viewModel.isFakeShutdownActive.collectAsState()

    val isRealtimeTracking by viewModel.isRealtimeTracking.collectAsState()
    val movementStatusBn by viewModel.movementStatusBn.collectAsState()
    val movementStatusEn by viewModel.movementStatusEn.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val totalDistanceMeters by viewModel.totalDistanceTraveledMeters.collectAsState()

    val isLockdownServiceRunning by viewModel.isLockdownServiceRunning.collectAsState()
    val isLockdownActive by viewModel.isLockdownActive.collectAsState()
    val lockdownAttemptCount by viewModel.lockdownAttemptCount.collectAsState()
    val lockdownReason by viewModel.lockdownReason.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            viewModel.refreshLocationCoordinates()
            viewModel.startRealtimeMovementTracking()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPowerOffChallengeModal by remember { mutableStateOf(false) }
    var enteredPowerOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }

    // Fake Shutdown Decoy Screen:
    if (isFakeShutdownActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable {
                    // Tap to exit fake shutdown
                    viewModel.exitFakeShutdownMode()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (isBn) "[ছদ্মবেশী শাটডাউন মোড সক্রিয়]" else "[Decoy Fake Shutdown Active]",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
                Text(
                    text = if (isBn) "স্ক্রিন সম্পূর্ণ কালো রয়েছে কিন্তু ব্যাকগ্রাউন্ডে জিপিএস ও লাইভ ট্র্যাকিং চলছে। বের হতে স্ক্রিনে ট্যাপ করুন।" else "Screen looks powered off while background GPS & SIM guard run. Tap to restore.",
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("findhub_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isBn) "ফাইন্ড হাব ও এন্টি-থিফ কমান্ড সেন্টার" else "Find Hub & Anti-Theft Guard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "লাইভ লোকেশন, পাওয়ার অফ গার্ড ও সিম ট্রে এলার্ট" else "Live Location, Anti-Power-Off & SIM Eject Alert",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Siren Alert Bar if triggered
            if (isSirenActive) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRed)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Alarm", tint = Color.White)
                                Column {
                                    Text(
                                        text = if (isBn) "🚨 জরুরি এন্টি-থিফ সাইরেন বাজছে!" else "🚨 EMERGENCY ANTI-THEFT SIREN ACTIVE!",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (isBn) "সিম কার্ড সরানো বা অননুমোদিত অ্যাক্সেস শনাক্ত!" else "SIM removed or unauthorized access attempt!",
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.toggleSirenAlarm(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(if (isBn) "বন্ধ করুন" else "Mute", color = DangerRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 1: Live & Last Known Location Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = NeonCyan, modifier = Modifier.size(24.dp))
                                }

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
                                            text = if (isBn) "লাইভ লোকেশন ও ডাটা ট্র্যাকড" else "LIVE TRACKING & GPS ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ShieldGreen,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = locationState.locationName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.refreshLocationCoordinates()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isBn) "জিপিএস লোকেশন রিফ্রেশ করা হয়েছে" else "GPS location refreshed")
                                    }
                                },
                                modifier = Modifier.testTag("refresh_location_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                            }
                        }

                        // Last Known Coordinates & Telemetry Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "জিপিএস অবস্থান ও ফিক্স টাইপ:" else "GPS Position & Fix Type:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBn) locationState.fixType.labelBn else locationState.fixType.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (locationState.hasFix && locationState.latitude != null && locationState.longitude != null) {
                                Text(
                                    text = "${String.format(Locale.US, "%.5f", locationState.latitude)}° N, ${String.format(Locale.US, "%.5f", locationState.longitude)}° E",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                locationState.accuracyMeters?.let { acc ->
                                    Text(
                                        text = if (isBn) "নির্ভুলতা (Accuracy): ±${acc.toInt()} মিটার (${locationState.provider ?: "GPS"})" else "Accuracy: ±${acc.toInt()}m (${locationState.provider ?: "GPS"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isBn) "জিপিএস সিগন্যাল অপেক্ষমান..." else "Waiting for verified GPS signal...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = WarningAmber
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CellTower, contentDescription = "Network", tint = WarningAmber, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = locationState.networkName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (locationState.batteryPercentage >= 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = ShieldGreen, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "${locationState.batteryPercentage}% চার্জ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Power-off limitation disclosure
                            Text(
                                text = if (isBn) {
                                    "ℹ️ ফোন বন্ধ থাকলে বা ডাটা নেটওয়ার্ক বিচ্ছিন্ন হলে ডিভাইস কোনো লাইভ লোকেশন পাঠাতে পারে না।"
                                } else {
                                    "ℹ️ A powered-off or disconnected phone cannot report location updates."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }

                        // Map Action
                        Button(
                            onClick = {
                                val lat = locationState.latitude ?: 23.8103
                                val lng = locationState.longitude ?: 90.4125
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(ShieldX+Location)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_maps_btn")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Map", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "গুগল ম্যাপে অবস্থান দেখুন" else "View on Google Maps",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section: Real-Time Movement Guard (Google Play Services Fused Location)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRealtimeTracking) ShieldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isRealtimeTracking) ShieldGreen else NeonCyan.copy(alpha = 0.4f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isRealtimeTracking) ShieldGreen.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsRun,
                                        contentDescription = "Movement",
                                        tint = if (isRealtimeTracking) ShieldGreen else NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isRealtimeTracking) ShieldGreen else WarningAmber)
                                        )
                                        Text(
                                            text = if (isRealtimeTracking) (if (isBn) "রিয়েল-টাইম ট্র্যাকিং সক্রিয়" else "REAL-TIME TRACKING ACTIVE") else (if (isBn) "ট্র্যাকিং প্রস্তুত" else "READY TO TRACK"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRealtimeTracking) ShieldGreen else WarningAmber,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = if (isBn) "রিয়েল-টাইম মুভমেন্ট ও স্পিড গার্ড" else "Real-Time Movement Guard",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Google Play Services",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Movement Telemetry Grid (Speed, Status, Distance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Speed Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = NeonCyan, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", currentSpeedKmh)} km/h",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NeonCyan
                                        )
                                    }
                                    Text(
                                        text = if (isBn) "বর্তমান গতি" else "Live Speed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Distance Traveled Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Navigation, contentDescription = "Distance", tint = ShieldGreen, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (totalDistanceMeters >= 1000f) {
                                                "${String.format(Locale.US, "%.2f", totalDistanceMeters / 1000f)} km"
                                            } else {
                                                "${totalDistanceMeters.toInt()} m"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ShieldGreen
                                        )
                                    }
                                    Text(
                                        text = if (isBn) "অতিক্রান্ত দূরত্ব" else "Displacement",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Movement Status Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🏃 ${if (isBn) movementStatusBn else movementStatusEn}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentSpeedKmh > 25f) DangerRed else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Tracking Action Button
                        Button(
                            onClick = {
                                if (isRealtimeTracking) {
                                    viewModel.stopRealtimeMovementTracking()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isBn) "রিয়েল-টাইম ট্র্যাকিং বন্ধ করা হয়েছে" else "Movement tracking stopped")
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                    viewModel.startRealtimeMovementTracking()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isBn) "রিয়েল-টাইম মুভমেন্ট ট্র্যাকিং চালু হয়েছে!" else "Real-time movement guard active!")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRealtimeTracking) DangerRed else ShieldGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_realtime_tracking_btn")
                        ) {
                            Icon(
                                imageVector = if (isRealtimeTracking) Icons.Default.Close else Icons.Default.DirectionsRun,
                                contentDescription = "Toggle Tracking",
                                tint = if (isRealtimeTracking) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRealtimeTracking) {
                                    if (isBn) "ট্র্যাকিং বন্ধ করুন (Stop Tracking)" else "Stop Movement Tracking"
                                } else {
                                    if (isBn) "রিয়েল-টাইম মুভমেন্ট ট্র্যাকিং চালু করুন (Start Tracking)" else "Start Real-Time Movement Guard"
                                },
                                color = if (isRealtimeTracking) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section 2: Device Lockdown Service & Anti-Power-Off Protection
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLockdownActive) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isLockdownActive) DangerRed else if (isLockdownServiceRunning) ShieldGreen else WarningAmber.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (isLockdownServiceRunning) ShieldGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        contentDescription = "Power Off",
                                        tint = if (isLockdownServiceRunning) ShieldGreen else WarningAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isLockdownServiceRunning) ShieldGreen else WarningAmber)
                                        )
                                        Text(
                                            text = if (isLockdownServiceRunning) (if (isBn) "লকডাউন সার্ভিস চালু" else "LOCKDOWN SERVICE ACTIVE") else (if (isBn) "সার্ভিস স্থগিত" else "SERVICE PAUSED"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLockdownServiceRunning) ShieldGreen else WarningAmber,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = if (isBn) "ডিভাইস লকডাউন ও শাটডাউন ইন্টারসেপ্টর" else "Device Lockdown & Shutdown Guard",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Switch(
                                checked = isLockdownServiceRunning,
                                onCheckedChange = { viewModel.toggleLockdownService(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ShieldGreen
                                ),
                                modifier = Modifier.testTag("lockdown_service_switch")
                            )
                        }

                        // Explanation & Rules
                        Text(
                            text = if (isBn) {
                                "💡 নিয়ম: পাওয়ার বাটন চেপে ফোন অফ করার চেষ্টা বা সিস্টেম শাটডাউন ব্রডকাস্ট শনাক্ত হওয়ামাত্রই ডিভাইস লকডাউন সক্রিয় হয়। সেকেন্ডারি বিশ্বস্ত ফোনে ওটিপি পাঠানো হয় এবং ওটিপি দিয়ে অনুমোদন না করা পর্যন্ত ডিভাইস বন্ধ করা যাবে না।"
                            } else {
                                "💡 Rule: Long-pressing the power button or system shutdown triggers instant lockdown. An OTP is sent to your paired secondary phone; power-off is blocked until OTP authentication succeeds."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Monitored Triggers Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isBn) "মনিটরিং ইভেন্টসমূহ (Monitored Triggers):" else "Monitored Security Triggers:",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBn) "• পাওয়ার কি লং-প্রেস মেনু (Global Actions)" else "• Power Key Long-Press (Global Actions)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("PROTECTED", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBn) "• সিস্টেম শাটডাউন ব্রডকাস্ট (ACTION_SHUTDOWN)" else "• System Shutdown Broadcast",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("INTERCEPTED", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBn) "• পোস্ট-বুট বাধ্যতামূলক লক (Post-Boot Guard)" else "• Post-Boot Enforced Lockdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("ENFORCED", color = ShieldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (lockdownAttemptCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = "Attempts", tint = DangerRed, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = if (isBn) "মোট শাটডাউন চেষ্টা রোধ: $lockdownAttemptCount বার" else "Total Intercepted Attempts: $lockdownAttemptCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Paired Phone Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (isBn) "অনুমোদন ডিভাইস:" else "OTP Target:"} ${viewModel.prefs.getSecondaryPhoneName()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.triggerPowerOffAttempt("Manual Lockdown Test / Power Key Trigger")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_lockdown_attempt_btn")
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Lockdown", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "অননুমোদিত শাটডাউন টেস্ট" else "Test Shutdown Lockdown",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.activateFakeShutdownMode()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("fake_shutdown_btn")
                            ) {
                                Text(
                                    text = if (isBn) "ছদ্মবেশী স্ক্রিন (Decoy)" else "Stealth Decoy",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: SIM Eject & Tray Protection
            item {
                val isEjected = simStatus.isTrayEjected || !simStatus.isInserted
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEjected) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isEjected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DangerRed)) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isEjected) DangerRed.copy(alpha = 0.2f) else ShieldGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEjected) Icons.Default.SimCardAlert else Icons.Default.SimCard,
                                    contentDescription = "SIM",
                                    tint = if (isEjected) DangerRed else ShieldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isBn) "সিম কার্ড ও ট্রে প্রটেকশন (SIM Eject Alert)" else "SIM Card Eject & Tray Protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEjected) DangerRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEjected) {
                                        if (isBn) "🚨 সতর্কতা! সিম ট্রে খোলা বা কার্ড অপসারিত!" else "🚨 ALERT! SIM Card Ejected!"
                                    } else {
                                        "${simStatus.operatorName} • ${if (isBn) "সুরক্ষিত ও ট্রের ভেতর" else "Secured in Tray"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isEjected) DangerRed else ShieldGreen
                                )
                            }
                        }

                        Text(
                            text = if (isBn) {
                                "চোর সাধারণত ফোন চুরি করে সিম খুলে ফেলে। সিমের ট্রে খোলা বা সিম পরিবর্তন করলেই উচ্চস্বরে সাইরেন অ্যালার্ট বাজবে এবং সেকেন্ডারি ফোনে শেষ পাওয়া জিপিএস লোকেশন মেসেজ চলে যাবে।"
                            } else {
                                "Thieves often eject the SIM immediately. Any tray ejection or SIM removal fires a siren and broadcasts location alert."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!isEjected) {
                                Button(
                                    onClick = { viewModel.simulateSimEjection() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("simulate_sim_eject_btn")
                                ) {
                                    Text(
                                        text = if (isBn) "সিম খোলার পরীক্ষা (Simulate Eject)" else "Test SIM Eject Alert",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.restoreSimStatus() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("restore_sim_btn")
                                ) {
                                    Text(
                                        text = if (isBn) "সিম পুনঃসংযুক্ত করুন (Restore SIM)" else "Restore SIM Status",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleAirplaneModeSimulation() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AirplanemodeActive, contentDescription = "Airplane", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (simStatus.isAirplaneModeOn) (if (isBn) "ফ্লাইট মোড অফ" else "Airplane Off") else (if (isBn) "ফ্লাইট টেস্ট" else "Test Airplane"))
                            }
                        }
                    }
                }
            }

            // Section 4: Emergency Remote Siren Toggle
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSirenActive) DangerRed.copy(alpha = 0.2f) else ElectricIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Siren",
                                    tint = if (isSirenActive) DangerRed else ElectricIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isBn) "রিমোট ইমার্জেন্সি সাইরেন (Find Siren)" else "Emergency Remote Siren",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBn) "হারিয়ে গেলে সর্বোচ্চ শব্দে ফ্ল্যাশ অ্যালার্ট বাজান" else "Triggers loud alarm to find lost device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isSirenActive,
                            onCheckedChange = { viewModel.toggleSirenAlarm(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = DangerRed)
                        )
                    }
                }
            }
        }
    }

    // Power-off OTP challenge dialog when triggered
    powerOffChallenge?.let { challenge ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPowerOffChallenge() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power Off", tint = DangerRed)
                    Text(if (isBn) "ফোন শাটডাউন অনুমোদন আবশ্যক" else "Power-Off OTP Required")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) {
                            "অননুমোদিতভাবে ফোন বন্ধ করা রোধ করতে লিংকড ফোন (${challenge.targetSecondaryPhone})-এ ওটিপি পাঠানো হয়েছে।"
                        } else {
                            "To prevent unauthorized shutdown, OTP has been dispatched to ${challenge.targetSecondaryPhone}."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBn) "সেকেন্ডারি ফোনে প্রেরিত ওটিপি:" else "Dispatched Power-Off OTP:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = challenge.otp,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WarningAmber
                            )
                        }
                    }

                    OutlinedTextField(
                        value = enteredPowerOtp,
                        onValueChange = {
                            enteredPowerOtp = it
                            otpError = null
                        },
                        label = { Text(if (isBn) "৬-ডিজিট ওটিপি লিখুন" else "Enter 6-digit OTP") },
                        singleLine = true,
                        isError = otpError != null,
                        supportingText = {
                            if (otpError != null) {
                                Text(otpError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("power_otp_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = viewModel.submitPowerOffOtp(enteredPowerOtp)
                        if (!ok) {
                            otpError = if (isBn) "ওটিপি ভুল হয়েছে!" else "Incorrect OTP!"
                        } else {
                            enteredPowerOtp = ""
                            scope.launch {
                                snackbarHostState.showSnackbar(if (isBn) "পাওয়ার অফ অনুমোদিত হয়েছে।" else "Power-off authorized.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    modifier = Modifier.testTag("confirm_power_off_btn")
                ) {
                    Text(if (isBn) "শাটডাউন অনুমোদন" else "Authorize Shutdown", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPowerOffChallenge() }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
