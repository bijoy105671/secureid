package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.WarningAmber
import com.example.data.model.ChallengeState
import kotlinx.coroutines.delay

@Composable
fun OtpChallengeModal(
    appName: String,
    generatedOtp: String?,
    challengeState: ChallengeState,
    secondaryPhoneName: String,
    isBengali: Boolean,
    onSubmitOtp: (String) -> Boolean,
    onSimulateRemoteApprove: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        while (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhonelinkLock,
                        contentDescription = "Challenge",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isBengali) "দ্বৈত নিরাপত্তা যাচাইকরণ" else "Dual-Factor Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_challenge_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (challengeState) {
                is ChallengeState.AwaitingOtp -> {
                    // OTP Challenge Phase
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(WarningAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "OTP",
                            tint = WarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isBengali) "$appName খোলার জন্য রেনডম ওটিপি প্রদান করুন" else "Enter Random OTP to open $appName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Live Dynamic Generated OTP hint for quick reference
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isBengali) "জেনারেট করা ক্রিপ্টোগ্রাফিক ওটিপি:" else "Live Generated OTP Challenge:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = generatedOtp ?: "------",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = NeonCyan
                            )
                            Text(
                                text = if (isBengali) "মেয়াদ বাকি: $timerSeconds সেকেন্ড" else "Expires in: ${timerSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (timerSeconds < 10) MaterialTheme.colorScheme.error else WarningAmber
                            )
                        }
                    }

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            if (it.length <= 6) {
                                enteredOtp = it
                                otpError = null
                            }
                        },
                        label = { Text(if (isBengali) "৬-ডিজিট ওটিপি লিখুন" else "Enter 6-digit OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input_field"),
                        isError = otpError != null,
                        supportingText = {
                            if (otpError != null) {
                                Text(otpError!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Fast autofill
                                enteredOtp = generatedOtp ?: ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("autofill_otp_btn")
                        ) {
                            Text(if (isBengali) "অটোফিল" else "Auto-fill")
                        }

                        Button(
                            onClick = {
                                val ok = onSubmitOtp(enteredOtp)
                                if (!ok) {
                                    otpError = if (isBengali) "ওটিপি মেলেনি! পুনরায় চেষ্টা করুন।" else "OTP mismatch! Please check code."
                                }
                            },
                            enabled = enteredOtp.length == 6,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("submit_otp_btn")
                        ) {
                            Text(
                                if (isBengali) "যাচাই করুন" else "Verify",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is ChallengeState.AwaitingRemoteApproval -> {
                    // Secondary Phone Push Approval Phase
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.5.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification",
                            tint = NeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBengali) "অন্য ফোনে পুশ নোটিফিকেশন পাঠানো হয়েছে!" else "Push Notification Sent to 2nd Phone!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isBengali) {
                            "আপনার লিংকড ফোন ($secondaryPhoneName)-এ অনুমোদন অনুরোধ গেছে। নোটিফিকেশন অ্যাপ্রুভ না করা পর্যন্ত $appName খুলবে না।"
                        } else {
                            "Approval request dispatched to $secondaryPhoneName. $appName will stay locked until approved on your secondary phone."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Pulse waiting indicator
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = if (isBengali) "অন্য ফোনের অনুমোদনের অপেক্ষায়..." else "Awaiting 2nd phone authorization...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Simulation action button (User can approve from here or switch to Remote Tab)
                    Button(
                        onClick = onSimulateRemoteApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = ShieldGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_approve_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Approve",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (isBengali) "সেকেন্ডারি ফোন থেকে অনুমোদন করুন (Approve)" else "Simulate Approve on 2nd Phone",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is ChallengeState.Granted -> {
                    // Success!
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ShieldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = ShieldGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isBengali) "নিরাপত্তা অনুমোদন সম্পন্ন!" else "Access Fully Approved!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ShieldGreen
                    )

                    Text(
                        text = if (isBengali) "$appName এখন খোলা হয়েছে।" else "$appName is now unlocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_granted_btn")
                    ) {
                        Text(
                            text = if (isBengali) "ঠিক আছে" else "Continue",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {}
            }
        }
    }
}
