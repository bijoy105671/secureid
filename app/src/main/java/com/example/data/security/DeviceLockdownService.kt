package com.example.data.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.AuditEventType
import com.example.data.model.AuditLogEntity
import com.example.data.model.AuditSeverity
import com.example.data.model.PowerOffChallenge
import com.example.ui.screens.LockdownActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * DeviceLockdownService monitors the system for unauthorized shutdown attempts
 * (including power dialog triggers and shutdown broadcasts) and enforces
 * secondary device OTP authentication before allowing the device to power off.
 */
class DeviceLockdownService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var systemDialogsReceiver: BroadcastReceiver? = null
    private var shutdownBroadcastReceiver: BroadcastReceiver? = null

    companion object {
        const val NOTIFICATION_ID = 8821
        const val CHANNEL_ID = "secureshield_lockdown_service"
        const val CHANNEL_NAME = "SecureShield Device Lockdown Protection"

        const val ACTION_START_LOCKDOWN_SERVICE = "com.example.secureshield.START_LOCKDOWN_SERVICE"
        const val ACTION_STOP_LOCKDOWN_SERVICE = "com.example.secureshield.STOP_LOCKDOWN_SERVICE"
        const val ACTION_TRIGGER_SHUTDOWN_LOCKDOWN = "com.example.secureshield.TRIGGER_SHUTDOWN_LOCKDOWN"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isLockdownActive = MutableStateFlow(false)
        val isLockdownActive: StateFlow<Boolean> = _isLockdownActive.asStateFlow()

        private val _activeChallenge = MutableStateFlow<PowerOffChallenge?>(null)
        val activeChallenge: StateFlow<PowerOffChallenge?> = _activeChallenge.asStateFlow()

        private val _unauthorizedAttemptCount = MutableStateFlow(0)
        val unauthorizedAttemptCount: StateFlow<Int> = _unauthorizedAttemptCount.asStateFlow()
        val interceptedAttemptsCount: StateFlow<Int> = _unauthorizedAttemptCount.asStateFlow()

        private val _lastAttemptReason = MutableStateFlow("None")
        val lastAttemptReason: StateFlow<String> = _lastAttemptReason.asStateFlow()
        val lastInterceptedReason: StateFlow<String> = _lastAttemptReason.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, DeviceLockdownService::class.java).apply {
                action = ACTION_START_LOCKDOWN_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DeviceLockdownService::class.java).apply {
                action = ACTION_STOP_LOCKDOWN_SERVICE
            }
            context.startService(intent)
        }

        fun triggerLockdown(context: Context, reason: String = "Physical Power Button / Shutdown Attempt") {
            val prefs = SecurityPreferences(context)
            if (!prefs.isAntiPowerOffEnabled()) return

            val generatedOtp = EncryptionHelper.generateRandomOtp()
            val challenge = PowerOffChallenge(
                challengeId = "POW-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
                otp = generatedOtp,
                requestingDevice = "Protected Primary Device",
                targetSecondaryPhone = prefs.getSecondaryPhoneName(),
                isApproved = false
            )

            _activeChallenge.value = challenge
            _isLockdownActive.value = true
            _unauthorizedAttemptCount.value += 1
            _lastAttemptReason.value = reason

            // Set boot lockdown in preferences in case the device is forcibly powered down
            prefs.setBootLockdownActive(true)

            // Send emergency alert with OTP to secondary phone
            val isBn = prefs.getLanguage() == "bn"
            NotificationHelper.showAntiTheftEmergencyNotification(
                context = context,
                title = if (isBn) "🚨 অননুমোদিত ফোন বন্ধ করার চেষ্টা রোধ!" else "🚨 Unauthorized Shutdown Attempt Intercepted!",
                message = if (isBn) {
                    "লকডাউন মোড সক্রিয় হয়েছে! শাটডাউন অনুমোদন করতে সেকেন্ডারি ফোনের ওটিপি ব্যবহার করুন: $generatedOtp"
                } else {
                    "Lockdown active! Power-off is blocked. Secondary Device Approval OTP: $generatedOtp"
                }
            )

            // Launch full-screen LockdownActivity overlay
            val lockdownIntent = Intent(context, LockdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_REASON", reason)
                putExtra("EXTRA_CHALLENGE_ID", challenge.challengeId)
            }
            context.startActivity(lockdownIntent)

            // Log event in database (do NOT log OTP!)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    db.auditLogDao().insertLog(
                        AuditLogEntity(
                            eventType = AuditEventType.INTRUDER_ALERT.name,
                            title = if (isBn) "ডিভাইস লকডাউন: অননুমোদিত শাটডাউন রোধ" else "Device Lockdown: Unauthorized Shutdown Intercepted",
                            details = "পাওয়ার অফ চেষ্টা শনাক্ত। কারণ: $reason। লকডাউন মোড সক্রিয়।",
                            targetItem = "Anti-Theft Lockdown Service",
                            severity = AuditSeverity.CRITICAL.name
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        fun dismissLockdown() {
            _isLockdownActive.value = false
            _activeChallenge.value = null
        }

        fun verifyOtp(context: Context, enteredOtp: String): Boolean {
            val currentChallenge = _activeChallenge.value ?: return false
            val isBn = SecurityPreferences(context).getLanguage() == "bn"

            if (enteredOtp == currentChallenge.otp && currentChallenge.otp.isNotBlank()) {
                _isLockdownActive.value = false
                _activeChallenge.value = null
                SecurityPreferences(context).setBootLockdownActive(false)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.auditLogDao().insertLog(
                            AuditLogEntity(
                                eventType = AuditEventType.REMOTE_APPROVED.name,
                                title = if (isBn) "শাটডাউন ওটিপি সফলভাবে যাচাইকৃত" else "Shutdown OTP Verified Successfully",
                                details = "সেকেন্ডারি ফোনের ওটিপি দ্বারা ডিভাইস শাটডাউন অনুমোদিত। লকডাউন প্রত্যাহার করা হয়েছে।",
                                targetItem = "Anti-Theft Lockdown Service",
                                severity = AuditSeverity.SUCCESS.name
                            )
                        )
                    } catch (_: Exception) {}
                }
                return true
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        db.auditLogDao().insertLog(
                            AuditLogEntity(
                                eventType = AuditEventType.INTRUDER_ALERT.name,
                                title = if (isBn) "ভুল ওটিপি দিয়ে লকডাউন বাইপাসের চেষ্টা" else "Invalid OTP on Lockdown Screen",
                                details = "ভুল ওটিপি ইনপুট: $enteredOtp। ডিভাইস লকডাউন অব্যাহত রয়েছে।",
                                targetItem = "Anti-Theft Lockdown Service",
                                severity = AuditSeverity.CRITICAL.name
                            )
                        )
                    } catch (_: Exception) {}
                }
                return false
            }
        }

        fun simulateSecondaryPhoneApproval(context: Context) {
            val challenge = _activeChallenge.value ?: return
            verifyOtp(context, challenge.otp)
        }

        fun liftLockdown(context: Context) {
            _isLockdownActive.value = false
            _activeChallenge.value = null
            SecurityPreferences(context).setBootLockdownActive(false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerShutdownMonitors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LOCKDOWN_SERVICE -> {
                _isServiceRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_SHUTDOWN_LOCKDOWN -> {
                val reason = intent.getStringExtra("EXTRA_REASON") ?: "Manual Trigger"
                triggerLockdown(applicationContext, reason)
            }
            else -> {
                startForegroundServiceNotification()
                _isServiceRunning.value = true
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors unauthorized power-off and triggers anti-theft lockdown"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "findhub")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isBn = SecurityPreferences(this).getLanguage() == "bn"
        val title = if (isBn) "🛡️ সিকিউরশিল্ড ডিভাইস লকডাউন গার্ড সক্রিয়" else "🛡️ SecureShield Device Lockdown Guard Active"
        val desc = if (isBn) "অননুমোদিত শাটডাউন রোধ ও ওটিপি ভেরিফিকেশন সক্রিয় রয়েছে" else "Unauthorized shutdown prevention & OTP guard running"

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun registerShutdownMonitors() {
        // Monitor 1: System dialogs (power menu long press detection)
        systemDialogsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                    val reason = intent.getStringExtra("reason")
                    // "globalactions" is the reason emitted when user long-presses power key to show power menu!
                    if (reason == "globalactions") {
                        triggerLockdown(applicationContext, "Power Button Long-Press (Global Actions)")
                    }
                }
            }
        }
        try {
            val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemDialogsReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(systemDialogsReceiver, filter)
            }
        } catch (_: Exception) {}

        // Monitor 2: System Shutdown Broadcasts
        shutdownBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == Intent.ACTION_SHUTDOWN ||
                    action == "android.intent.action.QUICKBOOT_POWEROFF" ||
                    action == "com.htc.intent.action.QUICKBOOT_POWEROFF"
                ) {
                    triggerLockdown(applicationContext, "System Shutdown Broadcast Intercepted ($action)")
                }
            }
        }
        try {
            val shutdownFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SHUTDOWN)
                addAction("android.intent.action.QUICKBOOT_POWEROFF")
                addAction("com.htc.intent.action.QUICKBOOT_POWEROFF")
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(shutdownBroadcastReceiver, shutdownFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(shutdownBroadcastReceiver, shutdownFilter)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        try {
            systemDialogsReceiver?.let { unregisterReceiver(it) }
            shutdownBroadcastReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
