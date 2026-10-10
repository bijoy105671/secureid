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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApprovalStatus
import com.example.data.model.RemoteRequestEntity
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
    val isBn by viewModel.isBengali.collectAsState()
    val pendingRequests by viewModel.pendingRemoteRequests.collectAsState()
    val allRequests by viewModel.allRemoteRequests.collectAsState()

    var showPairDialog by remember { mutableStateOf(false) }
    var phoneNameInput by remember { mutableStateOf(viewModel.prefs.getSecondaryPhoneName()) }
    var pairingCodeInput by remember { mutableStateOf(viewModel.prefs.getSecondaryPhoneCode()) }

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
                            text = if (isBn) "অন্য ফোনে পুশ নোটিফিকেশন অনুমোদন সিস্টেম" else "Paired Device Access Gatekeeper",
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricIndigo
                        )
                    }
                }
            }

            // Linked Phone Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricIndigo.copy(alpha = 0.4f)))
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
                                    .background(ElectricIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = "Device",
                                        tint = ElectricIndigo,
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
                                                .background(ShieldGreen)
                                        )
                                        Text(
                                            text = if (isBn) "লিংকড ফোন সক্রিয়" else "PAIRED & ONLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ShieldGreen,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = viewModel.prefs.getSecondaryPhoneName(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showPairDialog = true },
                                modifier = Modifier.testTag("edit_pair_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Pair", tint = NeonCyan)
                            }
                        }

                        // Pairing Token Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isBn) "সুরক্ষিত পেয়ারিং টোকেন" else "Security Pairing Token",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = viewModel.prefs.getSecondaryPhoneCode(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonCyan
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR",
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = if (isBn) {
                                "💡 নিয়ম: আর্থিক অ্যাপ খুলতে চাইলে এই ফোনে নোটিফিকেশন যাবে। নিচে থাকা 'অনুমোদন' বাটনে ট্যাপ না করলে অ্যাপ খুলবে না।"
                            } else {
                                "💡 Rule: Opening financial apps dispatches push requests to this device. The app won't unlock until Approved here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pending Approvals Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBn) "অনুমোদনের অপেক্ষায় অনুরোধ (${pendingRequests.size})" else "Pending Authorization Requests (${pendingRequests.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (pendingRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DangerRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTION REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (pendingRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "No Pending",
                                tint = ShieldGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isBn) "বর্তমানে কোনো পেন্ডিং অনুরোধ নেই" else "No Pending Access Requests",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isBn) {
                                    "অ্যাপ লকার ট্যাব থেকে বিকাশ বা নগদে 'Test Challenge' চালালে এখানে লাইভ অনুরোধ আসবে।"
                                } else {
                                    "Trigger 'Test Challenge' from App Lock to dispatch a live request here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                            viewModel.approveRemoteRequest(req)
                            scope.launch {
                                snackbarHostState.showSnackbar(if (isBn) "${req.appName} সফলভাবে অনুমোদন করা হয়েছে!" else "${req.appName} successfully approved!")
                            }
                        },
                        onReject = {
                            viewModel.rejectRemoteRequest(req)
                            scope.launch {
                                snackbarHostState.showSnackbar(if (isBn) "${req.appName} অনুরোধ বাতিল করা হয়েছে!" else "${req.appName} access rejected!")
                            }
                        }
                    )
                }
            }

            // Past Request History
            val resolvedRequests = allRequests.filter { it.status != ApprovalStatus.PENDING.name }
            item {
                Text(
                    text = if (isBn) "রিমোট অনুমোদনের পূর্ববর্তী হিস্ট্রি" else "Authorization History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            if (resolvedRequests.isEmpty()) {
                item {
                    Text(
                        text = if (isBn) "কোনো পূর্ববর্তী হিস্ট্রি রেকর্ড নেই।" else "No historical records yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            } else {
                items(resolvedRequests, key = { it.requestId }) { req ->
                    ResolvedRequestCard(request = req, isBn = isBn)
                }
            }
        }
    }

    // Pair Device Dialog
    if (showPairDialog) {
        AlertDialog(
            onDismissRequest = { showPairDialog = false },
            title = { Text(if (isBn) "সেকেন্ডারি ফোন কনফিগারেশন" else "Configure Secondary Phone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phoneNameInput,
                        onValueChange = { phoneNameInput = it },
                        label = { Text(if (isBn) "ফোনের নাম / মডেল" else "Phone Name / Model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("secondary_phone_name_input")
                    )
                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { pairingCodeInput = it },
                        label = { Text(if (isBn) "পেয়ারিং কোড" else "Pairing Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("secondary_phone_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.pairSecondaryPhone(phoneNameInput, pairingCodeInput)
                        showPairDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(if (isBn) "সংরক্ষণ" else "Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPairDialog = false }) {
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
                        text = "ID: ${request.requestId}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }

            // Details box
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
                    text = if (isBn) "যাচাইকরণ ওটিপি: ${request.challengeOtp}" else "Challenge OTP: ${request.challengeOtp}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = NeonCyan
                )
                Text(
                    text = if (isBn) "নেটওয়ার্ক লোকেশন: ${request.ipAddressOrLocation}" else "Network: ${request.ipAddressOrLocation}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons: Approve / Reject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reject_req_${request.requestId}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "বাতিল করুন" else "Reject", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("approve_req_${request.requestId}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "অনুমোদন (Approve)" else "Approve", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        text = "$timeStr • OTP: ${request.challengeOtp}",
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
