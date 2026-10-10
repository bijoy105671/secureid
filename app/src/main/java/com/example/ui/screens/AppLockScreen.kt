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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.data.model.LockedAppEntity
import com.example.ui.components.OtpChallengeModal
import com.example.ui.components.PinPadModal
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.data.model.ChallengeState
import com.example.ui.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn by viewModel.isBengali.collectAsState()
    val lockedApps by viewModel.lockedApps.collectAsState()
    val challengeApp by viewModel.activeChallengeApp.collectAsState()
    val challengeState by viewModel.challengeState.collectAsState()
    val currentOtp by viewModel.currentChallengeOtp.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.testTag("applock_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text(
                                text = if (isBn) "আর্থিক অ্যাপ লকার ও ওটিপি গার্ড" else "Financial App Lock & OTP Guard",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) "পাসওয়ার্ড + রেনডম ওটিপি + অন্য ফোনের পুশ অনুমোদন" else "Password + Random OTP + 2nd Phone Approval",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }
                    }
                }

                // Info banner explaining the 3-layer protection
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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Security", tint = NeonCyan, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (isBn) "৩-স্তরের সুরক্ষা প্রটোকল সক্রিয়:" else "3-Layer Security Protocol Active:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                            Text(
                                text = if (isBn) {
                                    "১. মাস্টার পিন / বায়োমেট্রিক\n২. ডাইনামিক রেনডম ওটিপি (প্রতি ৩০ সেকেন্ডে রিফ্রেশ)\n৩. অন্য ফোনে পুশ নোটিফিকেশন (অ্যাপ্রুভ না করা পর্যন্ত খুলবে না)"
                                } else {
                                    "1. Master PIN / Biometric verification\n2. Dynamic Random OTP challenge\n3. Push notification sent to paired 2nd phone for final unlock approval"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Financial Apps Group
                item {
                    Text(
                        text = if (isBn) "আর্থিক ও ব্যাংকিং অ্যাপসমূহ (বিকাশ, নগদ, ব্যাংক)" else "Financial & Banking Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                val financialApps = lockedApps.filter { it.isFinancial }
                items(financialApps, key = { it.packageName }) { app ->
                    AppLockItemCard(
                        app = app,
                        isBn = isBn,
                        onToggleLock = { viewModel.toggleAppLocked(app.packageName, it) },
                        onToggleOtp = { viewModel.toggleAppOtp(app.packageName, it) },
                        onToggleRemote = { viewModel.toggleAppRemoteApproval(app.packageName, it) },
                        onTestAccess = {
                            viewModel.startAppAccessChallenge(app)
                        }
                    )
                }

                // Other Device Apps Group
                item {
                    Text(
                        text = if (isBn) "ডিভাইস ও প্রাইভেসি অ্যাপসমূহ" else "Privacy & System Apps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
                    )
                }

                val otherApps = lockedApps.filter { !it.isFinancial }
                items(otherApps, key = { it.packageName }) { app ->
                    AppLockItemCard(
                        app = app,
                        isBn = isBn,
                        onToggleLock = { viewModel.toggleAppLocked(app.packageName, it) },
                        onToggleOtp = { viewModel.toggleAppOtp(app.packageName, it) },
                        onToggleRemote = { viewModel.toggleAppRemoteApproval(app.packageName, it) },
                        onTestAccess = {
                            viewModel.startAppAccessChallenge(app)
                        }
                    )
                }
            }

            // Challenge Active Modals (Awaiting PIN, OTP, or Remote Approval)
            if (challengeApp != null && challengeState != ChallengeState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (challengeState) {
                        is ChallengeState.AwaitingPin -> {
                            PinPadModal(
                                title = if (isBn) "${challengeApp!!.appName} লকার" else "${challengeApp!!.appName} Lock",
                                subtitle = if (isBn) "প্রবেশ করতে প্রথম স্তরের মাস্টার পিন দিন" else "Enter Master PIN to begin authentication",
                                isBengali = isBn,
                                onPinSubmitted = { pin ->
                                    viewModel.submitChallengePin(pin)
                                },
                                onBiometricSuccess = {
                                    viewModel.submitChallengePin("1234")
                                },
                                onDismiss = { viewModel.cancelChallenge() }
                            )
                        }

                        is ChallengeState.AwaitingOtp,
                        is ChallengeState.AwaitingRemoteApproval,
                        is ChallengeState.Granted -> {
                            OtpChallengeModal(
                                appName = challengeApp!!.appName,
                                generatedOtp = currentOtp,
                                challengeState = challengeState,
                                secondaryPhoneName = viewModel.prefs.getSecondaryPhoneName(),
                                isBengali = isBn,
                                onSubmitOtp = { otp ->
                                    viewModel.submitChallengeOtp(otp)
                                },
                                onSimulateRemoteApprove = {
                                    viewModel.finalizeAccessGranted()
                                },
                                onCancel = { viewModel.cancelChallenge() }
                            )
                        }

                        is ChallengeState.Rejected -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Blocked",
                                        tint = DangerRed,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = if (isBn) "প্রবেশাধিকার বাতিল করা হয়েছে!" else "Access Denied by 2nd Phone!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                    Text(
                                        text = if (isBn) {
                                            "আপনার সেকেন্ডারি ফোন থেকে এই অনুরোধটি রিজেক্ট করা হয়েছে।"
                                        } else {
                                            "This access attempt was rejected on your paired secondary device."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { viewModel.cancelChallenge() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(if (isBn) "বন্ধ করুন" else "Close")
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockItemCard(
    app: LockedAppEntity,
    isBn: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onToggleOtp: (Boolean) -> Unit,
    onToggleRemote: (Boolean) -> Unit,
    onTestAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (app.isLocked && app.isFinancial) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningAmber.copy(alpha = 0.5f))) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: App Name, Financial Badge & Master Lock Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (app.isFinancial) WarningAmber.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (app.isFinancial) Icons.Default.AccountBalance else Icons.Default.Security,
                            contentDescription = app.appName,
                            tint = if (app.isFinancial) WarningAmber else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (app.isFinancial) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarningAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "আর্থিক" else "Financial",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (app.isLocked) {
                                if (isBn) "সুরক্ষা সক্রিয় (লকড)" else "Protection Active"
                            } else {
                                if (isBn) "সুরক্ষা বন্ধ" else "Unlocked / Inactive"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (app.isLocked) ShieldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = app.isLocked,
                    onCheckedChange = onToggleLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (app.isFinancial) WarningAmber else NeonCyan
                    ),
                    modifier = Modifier.testTag("lock_toggle_${app.packageName}")
                )
            }

            // Sub-controls when locked
            AnimatedVisibility(visible = app.isLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // OTP Requirement Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Password, contentDescription = "OTP", tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "রেনডম ওটিপি আবশ্যক (Random OTP)" else "Require Dynamic OTP",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = app.requireOtp,
                            onCheckedChange = onToggleOtp,
                            modifier = Modifier.testTag("otp_toggle_${app.packageName}")
                        )
                    }

                    // Remote Approval Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Remote", tint = ElectricIndigo, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "অন্য ফোনের অনুমোদন (2nd Phone Push)" else "Require 2nd Phone Approval",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = app.requireRemoteApproval,
                            onCheckedChange = onToggleRemote,
                            modifier = Modifier.testTag("remote_toggle_${app.packageName}")
                        )
                    }

                    // Test Challenge Action Button
                    Button(
                        onClick = onTestAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("test_launch_${app.packageName}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Test", tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "অ্যাপে প্রবেশের পরীক্ষা চালান (Test Challenge)" else "Simulate App Launch Challenge",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
