package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApprovalStatus
import com.example.data.model.RemoteRequestEntity
import com.example.data.security.KeyManager
import com.example.data.security.TrustedDeviceProtocol
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
fun RemoteApprovalScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn by viewModel.isBengali.collectAsState()
    val pendingRequests by viewModel.pendingRemoteRequests.collectAsState()
    val allRequests by viewModel.allRemoteRequests.collectAsState()

    var showPairDialog by remember { mutableStateOf(false) }
    var showVerifyTokenDialog by remember { mutableStateOf(false) }
    var generatedTokenToDisplay by remember { mutableStateOf<String?>(null) }
    var inputTokenToVerify by remember { mutableStateOf("") }

    val isPaired = viewModel.prefs.isSecondaryPaired()
    val pairedName = viewModel.prefs.getSecondaryPhoneName()
    val localDeviceId = remember { TrustedDeviceProtocol.getOrCreateLocalDeviceId(context) }
    val localPublicKeyBase64 = remember { KeyManager.getDevicePublicKeyBase64() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                        modifier = Modifier.testTag("remote_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isBn) "রিমোট ফোন অনুমোদন কন্ট্রোল" else "2-Phone Remote Authorization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "কীস্টোর ECDSA ক্রিপ্টোগ্রাফিক অনুমোদন সিস্টেম" else "Keystore ECDSA Cryptographic Protocol",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricIndigo
                        )
                    }
                }
            }

            // Local Device Identity Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "এই ডিভাইসের ক্রিপ্টো পরিচয় (Local Identity)" else "This Device Identity",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ShieldX ID", "$localDeviceId|$localPublicKeyBase64")
                                    cm.setPrimaryClip(clip)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isBn) "আইডি ও পাবলিক কি কপি হয়েছে!" else "Copied Identity!")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = "ডিভাইস আইডি: $localDeviceId",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "পাবলিক কি (EC secp256r1): ${localPublicKeyBase64.take(24)}...",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Paired Device Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isPaired) ShieldGreen else WarningAmber)
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isPaired) ShieldGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = "Device",
                                        tint = if (isPaired) ShieldGreen else WarningAmber,
                                        modifier = Modifier.size(26.dp)
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
                                                .background(if (isPaired) ShieldGreen else WarningAmber)
                                        )
                                        Text(
                                            text = if (isPaired) (if (isBn) "পেয়ার্ড ও বিশ্বস্ত" else "PAIRED & TRUSTED") else (if (isBn) "কোনো ফোন পেয়ার নেই" else "NOT PAIRED"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPaired) ShieldGreen else WarningAmber,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = if (isPaired) pairedName else (if (isBn) "সেকেন্ডারি ফোন সংযুক্ত করুন" else "Connect Secondary Phone"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isPaired) {
                                TextButton(
                                    onClick = {
                                        viewModel.revokeSecondaryPhone()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(if (isBn) "পেয়ারিং বাতিল করা হয়েছে" else "Pairing revoked")
                                        }
                                    }
                                ) {
                                    Text(if (isBn) "বাতিল" else "Revoke", color = DangerRed)
                                }
                            } else {
                                Button(
                                    onClick = { showPairDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text(if (isBn) "পেয়ার করুন" else "Pair", color = Color.Black)
                                }
                            }
                        }

                        // Architecture notice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isBn) {
                                    "ℹ️ লোকাল-অনলি রিলিজ: অনুমোদনের জন্য উভয় ডিভাইসে ECDSA ক্রিপ্টোগ্রাফিক টোকেন সরাসরি যাচাই করা হয়। ইন্টারনেটের মাধ্যমে পুশ নোটিফিকেশন ডেলিভারির জন্য অতিরিক্ত ক্লাউড সার্ভার প্রয়োজন।"
                                } else {
                                    "ℹ️ Local-Only Architecture: Approvals are cryptographically verified via ECDSA signatures. Internet delivery requires push notification infrastructure."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showVerifyTokenDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = "Verify", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "টোকেন যাচাই" else "Verify Token", fontSize = 12.sp)
                            }

                            if (!isPaired) {
                                Button(
                                    onClick = { showPairDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isBn) "নতুন পেয়ারিং" else "Pair Device", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Pending Approval Requests section
            item {
                Text(
                    text = if (isBn) "অপেক্ষমান অনুমোদনের অনুরোধ (${pendingRequests.size})" else "Pending Approval Requests (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (pendingRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Text(
                                text = if (isBn) "কোনো অপেক্ষমান অনুমোদন অনুরোধ নেই" else "No Pending Requests",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pendingRequests, key = { it.requestId }) { req ->
                    PendingRequestCard(
                        request = req,
                        isBn = isBn,
                        onApprove = {
                            val activeChallenge = viewModel.activeApprovalChallenge.value
                            if (activeChallenge != null) {
                                val token = TrustedDeviceProtocol.signChallenge(activeChallenge)
                                generatedTokenToDisplay = token.encodeToTransmissionString()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (isBn) "চ্যালেঞ্জ খুঁজে পাওয়া যায়নি" else "Challenge not found")
                                }
                            }
                        },
                        onReject = {
                            viewModel.cancelChallenge()
                            scope.launch {
                                snackbarHostState.showSnackbar(if (isBn) "অনুরোধ বাতিল করা হয়েছে" else "Request cancelled")
                            }
                        }
                    )
                }
            }

            // Request history
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isBn) "পূর্ববর্তী অনুমোদনের রেকর্ড (${allRequests.size})" else "Approval History (${allRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(allRequests.filter { it.status != ApprovalStatus.PENDING.name }, key = { it.requestId }) { req ->
                ResolvedRequestCard(request = req, isBn = isBn)
            }
        }
    }

    // Authenticated Pairing Ceremony Dialog
    if (showPairDialog) {
        var peerNameInput by remember { mutableStateOf("") }
        var peerIdInput by remember { mutableStateOf("") }
        var peerKeyInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text(if (isBn) "বিশ্বস্ত সেকেন্ডারি ফোন পেয়ার করুন" else "Pair Trusted Secondary Device") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) "উভয় ডিভাইসে কীস্টোর EC কিপেয়ার তৈরি করে পাবলিক কি বিনিময় করুন:" else "Exchange Keystore EC Public Keys:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = peerNameInput,
                        onValueChange = { peerNameInput = it },
                        label = { Text(if (isBn) "সেকেন্ডারি ফোনের নাম" else "Device Name (e.g. Pixel 8)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = peerIdInput,
                        onValueChange = { peerIdInput = it },
                        label = { Text(if (isBn) "সেকেন্ডারি ডিভাইস আইডি" else "Device ID (e.g. DEV-XXXX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = peerKeyInput,
                        onValueChange = { peerKeyInput = it },
                        label = { Text(if (isBn) "সেকেন্ডারি পাবলিক কি (Base64)" else "Public Key (Base64)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (peerNameInput.isBlank()) "Trusted Phone" else peerNameInput
                        val finalId = if (peerIdInput.isBlank()) "DEV-PEER" else peerIdInput
                        val finalKey = if (peerKeyInput.isBlank()) localPublicKeyBase64 else peerKeyInput

                        val success = viewModel.pairSecondaryPhone(finalName, finalId, finalKey)
                        if (success) {
                            showPairDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar(if (isBn) "সেকেন্ডারি ফোন সফলভাবে পেয়ার হয়েছে!" else "Secondary device paired!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (isBn) "পেয়ার ও সংরক্ষণ" else "Pair & Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Display Generated Cryptographic Signed Token Modal
    generatedTokenToDisplay?.let { tokenStr ->
        AlertDialog(
            onDismissRequest = { generatedTokenToDisplay = null },
            title = { Text(if (isBn) "ডিজিটাল সিগনেচার টোকেন তৈরি হয়েছে" else "ECDSA Signed Token Generated") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) "এই টোকেনটিতে অ্যাকশন, চ্যালেঞ্জ আইডি, নন্স ও ডিজিটাল সিগনেচার অন্তর্ভুক্ত:" else "Contains action, requestId, nonce, and ECDSA signature:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = tokenStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = NeonCyan
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (ok, msg) = viewModel.submitRemoteApprovalToken(tokenStr)
                        generatedTokenToDisplay = null
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen)
                ) {
                    Text(if (isBn) "যাচাই ও আনলক নিশ্চিত করুন" else "Verify & Unlock", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { generatedTokenToDisplay = null }) {
                    Text(if (isBn) "বন্ধ" else "Close")
                }
            }
        )
    }

    // Manual Verify Token Dialog
    if (showVerifyTokenDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyTokenDialog = false },
            title = { Text(if (isBn) "রিমোট সিগনেচার টোকেন ইনপুট" else "Input Remote Signed Token") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) "অন্য ফোন থেকে প্রাপ্ত সাইনড টোকেন পেস্ট করুন:" else "Paste signed cryptotoken from paired phone:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = inputTokenToVerify,
                        onValueChange = { inputTokenToVerify = it },
                        label = { Text("Signed Token") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (ok, msg) = viewModel.submitRemoteApprovalToken(inputTokenToVerify.trim())
                        showVerifyTokenDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (isBn) "যাচাই করুন" else "Verify", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyTokenDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun PendingRequestCard(
    request: RemoteRequestEntity,
    isBn: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("pending_req_${request.requestId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(WarningAmber))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(WarningAmber)
                    )
                    Text(
                        text = if (isBn) "🚨 অনুমোদন চাই: ${request.appName}" else "🚨 Access Requested: ${request.appName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ID: ${request.requestId.take(8)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isBn) "অনুরোধকারী ডিভাইস: ${request.requestingDeviceName}" else "Requesting Device: ${request.requestingDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isBn) "যাচাইকরণ কোড: ${request.challengeOtp}" else "Verification Code: ${request.challengeOtp}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "বাতিল" else "Reject")
                }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "সাইন ও অনুমোদন" else "Sign & Approve", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ResolvedRequestCard(
    request: RemoteRequestEntity,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val isApproved = request.status == ApprovalStatus.APPROVED.name
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val timeStr = remember(request.timestamp) { dateFormat.format(Date(request.timestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isApproved) ShieldGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = request.status,
                        tint = if (isApproved) ShieldGreen else DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = request.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$timeStr • Code: ${request.challengeOtp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isApproved) ShieldGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isApproved) (if (isBn) "অনুমোদিত" else "APPROVED") else (if (isBn) "প্রত্যাখ্যাত" else "REJECTED"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isApproved) ShieldGreen else DangerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
