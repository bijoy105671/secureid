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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudDriveItemEntity
import com.example.data.model.DriveShareStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityViewModel
import kotlinx.coroutines.launch

@Composable
fun CloudDriveDlpScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn by viewModel.isBengali.collectAsState()
    val driveItems by viewModel.allDriveItems.collectAsState()

    val leakingCount = driveItems.count { it.isLeaking }
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
                        modifier = Modifier.testTag("drive_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isBn) "গুগল ড্রাইভ লিকেজ গার্ড (Cloud DLP)" else "Google Drive DLP Guardian",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "ড্রাইভের কোনো ডাটা যেন অননুমোদিত শেয়ার না হয়" else "Cloud Data Leak Prevention & Access Shield",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (leakingCount > 0) DangerRed.copy(alpha = 0.15f) else ShieldGreen.copy(alpha = 0.15f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (leakingCount > 0) listOf(DangerRed, WarningAmber) else listOf(ShieldGreen, NeonCyan)
                        )
                    )
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
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (leakingCount > 0) DangerRed else ShieldGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (leakingCount > 0) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = "Cloud Status",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (leakingCount > 0) {
                                        if (isBn) "⚠️ $leakingCount টি ফাইলে পাবলিক শেয়ার চালু আছে!" else "⚠️ $leakingCount Files Exposed with Public Link!"
                                    } else {
                                        if (isBn) "🛡️ ড্রাইভের সকল ডাটা সম্পূর্ণ নিরাপদ ও প্রাইভেট" else "🛡️ All Google Drive Data is Strictly Private"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (leakingCount > 0) DangerRed else ShieldGreen
                                )
                                Text(
                                    text = if (leakingCount > 0) {
                                        if (isBn) "যে কেউ লিঙ্ক দিয়ে গোপন ফাইল দেখতে বা ডাউনলোড করতে পারে। এখনই লক করুন।" else "Anyone with the link can view or download your files. Lock down immediately."
                                    } else {
                                        if (isBn) "কোনো পাবলিক লিঙ্ক বা ডেটা লিকেজ ঝুঁকি নেই।" else "Zero public access leaks detected."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (leakingCount > 0) {
                            Button(
                                onClick = {
                                    viewModel.revokeAllDrivePublicLinks()
                                    scope.launch {
                                        val msg = if (isBn) "গুগল ড্রাইভের সকল পাবলিক লিঙ্ক বন্ধ করা হয়েছে!" else "All public share links revoked!"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("revoke_all_drive_links_btn")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "সকল পাবলিক লিঙ্ক বাতিল ও লক করুন (Revoke All)" else "Revoke All Public Links & Lock Data",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Explanatory DLP Policy Card
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = "DLP", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (isBn) "ক্লাউড ডেটা লিকেজ রোধ নীতি (Cloud DLP):" else "Cloud Data Leak Prevention Policy:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                        Text(
                            text = if (isBn) {
                                "• গুগল ড্রাইভে থাকা ন্যাশনাল আইডি, পাসপোর্ট বা ব্যাংক স্টেটমেন্টের ভুলবশত পাবলিক শেয়ার ('Anyone with link') শনাক্ত করে তাৎক্ষণিক অ্যালার্ট দেয়।\n• ১-ট্যাপে শেয়ার লিঙ্ক বাতিল করে শুধুমাত্র মালিকের এক্সেস সীমাবদ্ধ করে।"
                            } else {
                                "• Monitors sensitive Drive files for accidental public exposure ('Anyone with link').\n• Provides one-tap lockdown to revert sharing strictly to Owner Only."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Monitored Cloud Files list
            item {
                Text(
                    text = if (isBn) "নিরীক্ষিত গুগল ড্রাইভ ফাইলসমূহ (${driveItems.size})" else "Monitored Cloud Files (${driveItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            items(driveItems, key = { it.id }) { item ->
                DriveItemCard(item = item, isBn = isBn)
            }
        }
    }
}

@Composable
fun DriveItemCard(
    item: CloudDriveItemEntity,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = if (item.isLeaking) {
        DangerRed to (if (isBn) "পাবলিক লিঙ্ক অন (ঝুঁকি!)" else "Public Link (Leaked!)")
    } else {
        ShieldGreen to (if (isBn) "লকড / ব্যক্তিগত" else "Locked / Private")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isLeaking) DangerRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (item.isLeaking) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DangerRed)) else null
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isLeaking) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${item.fileSizeFormatted} • ${item.sharedWithEmails}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
