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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileCategory
import com.example.data.model.VaultFileEntity
import com.example.ui.components.PinPadModal
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
fun VaultScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBn by viewModel.isBengali.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val allFiles by viewModel.allVaultFiles.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<VaultFileEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (!isVaultUnlocked) {
        // Vault Lock Screen (Enter PIN or Biometrics)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("vault_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = if (isBn) "ভল্ট পাসওয়ার্ড যাচাই" else "Vault Security Check",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                PinPadModal(
                    title = if (isBn) "এনক্রিপ্টেড ভল্ট লকড" else "Encrypted Vault Locked",
                    subtitle = if (isBn) "ছবি, অডিও, ভিডিও ও পিডিএফ দেখতে মাস্টার পিন দিন" else "Enter Master PIN to access photos, audio, video & PDFs",
                    isBengali = isBn,
                    onPinSubmitted = { enteredPin ->
                        viewModel.verifyMasterPin(enteredPin)
                    },
                    onBiometricSuccess = {
                        viewModel.verifyMasterPin("1234") // Fingerprint bypass
                    }
                )
            }
        }
        return
    }

    // Unlocked Vault View
    val filteredFiles = allFiles.filter { file ->
        val categoryMatch = selectedCategory == null || file.category == selectedCategory!!.name
        val searchMatch = searchQuery.isEmpty() || file.title.contains(searchQuery, ignoreCase = true) || file.description.contains(searchQuery, ignoreCase = true)
        categoryMatch && searchMatch
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_file_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("vault_unlocked_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isBn) "এনক্রিপ্টেড ফাইল ভল্ট" else "Encrypted Files Vault",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "AES-256 সামরিক এনক্রিপশনে সুরক্ষিত" else "Protected by AES-256 GCM",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShieldGreen
                        )
                    }
                }

                // Quick Lock Button
                IconButton(
                    onClick = { viewModel.lockVault() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DangerRed.copy(alpha = 0.15f))
                        .testTag("quick_lock_vault_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = DangerRed
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isBn) "ভল্টের ফাইল খুঁজুন..." else "Search secure files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("vault_search_field"),
                shape = RoundedCornerShape(16.dp)
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setSelectedCategory(null) },
                        label = { Text(if (isBn) "সকল (${allFiles.size})" else "All (${allFiles.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                items(FileCategory.values()) { category ->
                    val count = allFiles.count { it.category == category.name }
                    val label = if (isBn) "${category.labelBn} ($count)" else "${category.labelEn} ($count)"
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setSelectedCategory(category) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Files List or Empty State
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (isBn) "এই ক্যাটাগরিতে কোনো এনক্রিপ্ট ফাইল নেই" else "No encrypted files in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBn) "নিচের '+' বাটনে ট্যাপ করে ছবি, অডিও, ভিডিও বা গোপন পিডিএফ যোগ করুন" else "Tap '+' button to add encrypted photos, audio, videos or PDFs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        VaultFileItemCard(
                            file = file,
                            isBn = isBn,
                            onPreview = { previewFile = file },
                            onShare = {
                                viewModel.shareVaultFile(file, "External Apps / Secure Channel")
                                scope.launch {
                                    val msg = if (isBn) "ফাইল শেয়ার ট্র্যাকিং অডিট লগে রেকর্ড হয়েছে!" else "Share event logged to Audit Trail!"
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onDelete = {
                                viewModel.deleteVaultFile(file)
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (isBn) "ফাইল মুছে ফেলা হয়েছে" else "File deleted")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add File Dialog
    if (showAddDialog) {
        AddVaultFileDialog(
            isBn = isBn,
            onDismiss = { showAddDialog = false },
            onAdd = { title, category, content, size ->
                viewModel.addVaultFile(title, category, content, size)
                showAddDialog = false
                scope.launch {
                    val msg = if (isBn) "ফাইলটি এনক্রিপ্ট করে ভল্টে সুরক্ষিত করা হয়েছে!" else "File encrypted and secured in vault!"
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    // File Preview Modal
    previewFile?.let { file ->
        FilePreviewDialog(
            file = file,
            isBn = isBn,
            onDismiss = { previewFile = null },
            onShare = {
                viewModel.shareVaultFile(file, "Decrypted Viewer Export")
                scope.launch {
                    snackbarHostState.showSnackbar(if (isBn) "ফাইল শেয়ার ইভেন্ট লগ হয়েছে" else "Share event recorded in audit logs")
                }
            }
        )
    }
}

@Composable
fun VaultFileItemCard(
    file: VaultFileEntity,
    isBn: Boolean,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = try {
        FileCategory.valueOf(file.category)
    } catch (_: Exception) {
        FileCategory.DOCUMENT
    }

    val (icon, color) = when (category) {
        FileCategory.PHOTO -> Icons.Default.Image to NeonCyan
        FileCategory.VIDEO -> Icons.Default.Videocam to ElectricIndigo
        FileCategory.AUDIO -> Icons.Default.Audiotrack to ShieldGreen
        FileCategory.DOCUMENT -> Icons.Default.Description to WarningAmber
        FileCategory.NOTE -> Icons.Default.Note to Color(0xFFFF4081)
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(file.timestamp) { dateFormat.format(Date(file.timestamp)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPreview() }
            .testTag("vault_item_${file.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(2.dp)
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = file.category,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = file.fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Encryption tag
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ShieldGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔒 ${file.encryptionAlgorithm}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ShieldGreen,
                        fontSize = 9.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp).testTag("share_file_${file.id}")) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("delete_file_${file.id}")) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddVaultFileDialog(
    isBn: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, FileCategory, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategory.PHOTO) }
    var secretContent by remember { mutableStateOf("") }
    var sizeInput by remember { mutableStateOf("2.5 MB") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBn) "ভল্টে নতুন ফাইল এনক্রিপ্ট করুন" else "Encrypt New File to Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBn) "ক্যাটাগরি নির্বাচন করুন:" else "Select Category:",
                    style = MaterialTheme.typography.labelMedium
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FileCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                if (title.isEmpty()) {
                                    title = when (cat) {
                                        FileCategory.PHOTO -> "Secure_Photo_${System.currentTimeMillis() % 1000}.jpg"
                                        FileCategory.VIDEO -> "Private_Video_${System.currentTimeMillis() % 1000}.mp4"
                                        FileCategory.AUDIO -> "Voice_Record_${System.currentTimeMillis() % 1000}.aac"
                                        FileCategory.DOCUMENT -> "Tax_Document_${System.currentTimeMillis() % 1000}.pdf"
                                        FileCategory.NOTE -> "Confidential_Note_${System.currentTimeMillis() % 1000}.txt"
                                    }
                                }
                            },
                            label = { Text(if (isBn) cat.labelBn else cat.labelEn, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isBn) "ফাইলের নাম" else "File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_file_title_input")
                )

                OutlinedTextField(
                    value = secretContent,
                    onValueChange = { secretContent = it },
                    label = { Text(if (isBn) "গোপন তথ্য / কন্টেন্ট / নোট" else "Secret Content / Note / Data") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("add_file_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (title.isBlank()) "Protected_File.dat" else title
                    onAdd(finalTitle, selectedCategory, secretContent, sizeInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("save_file_btn")
            ) {
                Text(if (isBn) "এনক্রিপ্ট করে সংরক্ষণ" else "Encrypt & Save", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
fun FilePreviewDialog(
    file: VaultFileEntity,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = NeonCyan)
                Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isBn) "ডিক্রিপ্ট করা ডেটা প্রিভিউ:" else "Decrypted Content Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                        Text(
                            text = if (file.secretContent.isNotBlank()) file.secretContent else (file.description.ifEmpty { "Binary encrypted stream [AES-256 GCM Block Verified]" }),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = if (isBn) "ফাইলের সাইজ: ${file.fileSizeFormatted} | এনক্রিপশন: ${file.encryptionAlgorithm}" else "Size: ${file.fileSizeFormatted} | ${file.encryptionAlgorithm}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onShare()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBn) "শেয়ার ও লগ করুন" else "Share & Log", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বন্ধ করুন" else "Close")
            }
        }
    )
}
