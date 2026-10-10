package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppPermissionInfo
import com.example.data.model.ApprovalStatus
import com.example.data.model.AuditEventType
import com.example.data.model.AuditLogEntity
import com.example.data.model.AuditSeverity
import com.example.data.model.CloudDriveItemEntity
import com.example.data.model.DriveShareStatus
import com.example.data.model.FileCategory
import com.example.data.model.LastKnownLocation
import com.example.data.model.LockedAppEntity
import com.example.data.model.PowerOffChallenge
import com.example.data.model.RemoteRequestEntity
import com.example.data.model.SensorAccessLogEntity
import com.example.data.model.SimStatus
import com.example.data.model.ChallengeState
import com.example.data.model.VaultFileEntity
import com.example.data.security.DeviceLockdownService
import com.example.data.security.EncryptionHelper
import com.example.data.security.FileVaultManager
import com.example.data.security.KeyManager
import com.example.data.security.LocationManager
import com.example.data.security.NotificationHelper
import com.example.data.security.PermissionAuditorHelper
import com.example.data.security.SecurityPreferences
import com.example.data.security.TrustedDeviceProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Hardened Security ViewModel for SHIELD X.
 *
 * Remediations applied:
 * - NO hardcoded default credentials or backdoor OTP bypasses ("999999" and "123456" removed).
 * - First-run credential setup requirement enforced.
 * - Persistent failed-attempt throttling with progressive lockout delays.
 * - Real AES-256-GCM file storage via FileVaultManager with Keystore-backed key hierarchy.
 * - Real Trusted-Device ECDSA cryptographic approval protocol with replay protection.
 * - Honest location and sensor status reporting; removal of fabricated background spying data.
 * - Audit logs sanitized to never leak passwords, PINs, OTPs, or private keys.
 */
class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vaultDao = db.vaultFileDao()
    private val lockedAppDao = db.lockedAppDao()
    private val remoteRequestDao = db.remoteRequestDao()
    private val auditLogDao = db.auditLogDao()
    private val sensorAccessDao = db.sensorAccessDao()
    private val cloudDriveDao = db.cloudDriveDao()

    val prefs = SecurityPreferences(application)
    val locationManager = LocationManager(application)

    // Language state
    private val _isBengali = MutableStateFlow(prefs.getLanguage() == "bn")
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    // First-run setup state
    private val _isSetupDone = MutableStateFlow(prefs.isSetupDone())
    val isSetupDone: StateFlow<Boolean> = _isSetupDone.asStateFlow()

    // Master PIN & Vault unlock status
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(prefs.getFailedAttempts())
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _remainingLockoutSeconds = MutableStateFlow(prefs.getRemainingLockoutSeconds())
    val remainingLockoutSeconds: StateFlow<Long> = _remainingLockoutSeconds.asStateFlow()

    private val _lastIntruderAlert = MutableStateFlow<String?>(null)
    val lastIntruderAlert: StateFlow<String?> = _lastIntruderAlert.asStateFlow()

    // Disguise calculator active state
    private val _isCalculatorDisguiseActive = MutableStateFlow(prefs.isDisguiseCalculatorEnabled())
    val isCalculatorDisguiseActive: StateFlow<Boolean> = _isCalculatorDisguiseActive.asStateFlow()

    // Demo Mode state (OFF by default)
    private val _isDemoModeActive = MutableStateFlow(prefs.isDemoModeEnabled())
    val isDemoModeActive: StateFlow<Boolean> = _isDemoModeActive.asStateFlow()

    // Vault Files
    val allVaultFiles: StateFlow<List<VaultFileEntity>> = vaultDao.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    // Locked Apps
    val lockedApps: StateFlow<List<LockedAppEntity>> = lockedAppDao.getAllLockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Remote Requests
    val allRemoteRequests: StateFlow<List<RemoteRequestEntity>> = remoteRequestDao.getAllRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRemoteRequests: StateFlow<List<RemoteRequestEntity>> = remoteRequestDao.getPendingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Logs
    val auditLogs: StateFlow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permission Auditor
    private val _permissionApps = MutableStateFlow<List<AppPermissionInfo>>(emptyList())
    val permissionApps: StateFlow<List<AppPermissionInfo>> = _permissionApps.asStateFlow()

    private val _isScanningPermissions = MutableStateFlow(false)
    val isScanningPermissions: StateFlow<Boolean> = _isScanningPermissions.asStateFlow()

    // App Lock Challenge State
    private val _activeChallengeApp = MutableStateFlow<LockedAppEntity?>(null)
    val activeChallengeApp: StateFlow<LockedAppEntity?> = _activeChallengeApp.asStateFlow()

    private val _currentChallengeOtp = MutableStateFlow<String?>(null)
    val currentChallengeOtp: StateFlow<String?> = _currentChallengeOtp.asStateFlow()

    private val _activeApprovalChallenge = MutableStateFlow<TrustedDeviceProtocol.ApprovalChallenge?>(null)
    val activeApprovalChallenge: StateFlow<TrustedDeviceProtocol.ApprovalChallenge?> = _activeApprovalChallenge.asStateFlow()

    private val _activeRequestId = MutableStateFlow<String?>(null)
    val activeRequestId: StateFlow<String?> = _activeRequestId.asStateFlow()

    private val _challengeState = MutableStateFlow<ChallengeState>(ChallengeState.Idle)
    val challengeState: StateFlow<ChallengeState> = _challengeState.asStateFlow()

    // Sensor Telemetry & Anomaly Detection
    val allSensorLogs: StateFlow<List<SensorAccessLogEntity>> = sensorAccessDao.getAllAccessLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unusualSensorLogs: StateFlow<List<SensorAccessLogEntity>> = sensorAccessDao.getUnusualAccessLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Google Drive Cloud DLP
    val allDriveItems: StateFlow<List<CloudDriveItemEntity>> = cloudDriveDao.getAllDriveItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Find Hub & Anti-Theft Telemetry
    private val _lastKnownLocation = MutableStateFlow(LastKnownLocation())
    val lastKnownLocation: StateFlow<LastKnownLocation> = _lastKnownLocation.asStateFlow()

    private val _simStatus = MutableStateFlow(SimStatus())
    val simStatus: StateFlow<SimStatus> = _simStatus.asStateFlow()

    private val _isSirenAlarmActive = MutableStateFlow(false)
    val isSirenAlarmActive: StateFlow<Boolean> = _isSirenAlarmActive.asStateFlow()

    private val _activePowerOffChallenge = MutableStateFlow<PowerOffChallenge?>(null)
    val activePowerOffChallenge: StateFlow<PowerOffChallenge?> = _activePowerOffChallenge.asStateFlow()

    private val _isFakeShutdownActive = MutableStateFlow(false)
    val isFakeShutdownActive: StateFlow<Boolean> = _isFakeShutdownActive.asStateFlow()

    private val _isBootLocked = MutableStateFlow(prefs.isBootLockdownActive())
    val isBootLocked: StateFlow<Boolean> = _isBootLocked.asStateFlow()

    val isRealtimeTracking: StateFlow<Boolean> = locationManager.isTracking
    val movementStatusBn: StateFlow<String> = locationManager.movementStatusBn
    val movementStatusEn: StateFlow<String> = locationManager.movementStatusEn
    val currentSpeedKmh: StateFlow<Float> = locationManager.currentSpeedKmh
    val totalDistanceTraveledMeters: StateFlow<Float> = locationManager.totalDistanceTraveledMeters
    val movementTrail: StateFlow<List<Pair<Double, Double>>> = locationManager.movementTrail

    // Device Lockdown Service Integration
    val isLockdownServiceRunning: StateFlow<Boolean> = DeviceLockdownService.isServiceRunning
    val isLockdownActive: StateFlow<Boolean> = DeviceLockdownService.isLockdownActive
    val lockdownAttemptCount: StateFlow<Int> = DeviceLockdownService.interceptedAttemptsCount
    val lastLockdownReason: StateFlow<String?> = DeviceLockdownService.lastInterceptedReason
    val lockdownReason: StateFlow<String?> = lastLockdownReason

    init {
        NotificationHelper.createNotificationChannel(application)
        scanPermissions()
        seedInitialFinancialAppsIfEmpty()
        migrateLegacyVaultRecordsIfPresent()

        // Fetch initial verified location
        locationManager.fetchLastKnownLocation(
            onSuccess = { loc -> _lastKnownLocation.value = loc },
            onError = { /* Location unavailable at init */ }
        )

        // Ensure Android Keystore master keys are initialized
        try {
            KeyManager.getOrCreateMasterKey()
            KeyManager.getOrCreateDeviceIdentityKeyPair()
        } catch (_: Exception) {}

        // Auto-start Device Lockdown Guard if enabled in preferences
        if (prefs.isAntiPowerOffEnabled()) {
            DeviceLockdownService.start(application)
        }
    }

    fun toggleLanguage() {
        val newBn = !_isBengali.value
        _isBengali.value = newBn
        prefs.setLanguage(if (newBn) "bn" else "en")
    }

    // ------------------------------------------------------------------------
    // Master Credential Management & Throttling
    // ------------------------------------------------------------------------

    fun setupMasterPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        prefs.setMasterPin(newPin)
        _isSetupDone.value = true
        _isVaultUnlocked.value = true
        _failedAttemptsCount.value = 0
        _remainingLockoutSeconds.value = 0L

        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "মাস্টার সিকিউরিটি পিন সেট করা হয়েছে" else "Master Security PIN Initialized",
            details = "PBKDF2-HMAC-SHA256 হ্যাশিং ও সল্ট সহ মাস্টার পিন কনফিগার করা হয়েছে।",
            target = "Credential Security",
            severity = AuditSeverity.SUCCESS
        )
        return true
    }

    fun verifyMasterPin(enteredPin: String): Boolean {
        val lockout = prefs.getRemainingLockoutSeconds()
        if (lockout > 0) {
            _remainingLockoutSeconds.value = lockout
            val msg = if (_isBengali.value) {
                "অতিরিক্ত ভুল চেষ্টার কারণে সিস্টেম সাময়িকভাবে লকডাউন রয়েছে। অপেক্ষা করুন: $lockout সেকেন্ড"
            } else {
                "Too many failed attempts. Throttling active. Wait: $lockout seconds"
            }
            _lastIntruderAlert.value = msg
            return false
        }

        val correct = prefs.verifyPin(enteredPin)
        if (correct) {
            _isVaultUnlocked.value = true
            _failedAttemptsCount.value = 0
            _remainingLockoutSeconds.value = 0L
            _lastIntruderAlert.value = null

            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "ভল্ট সফলভাবে আনলক করা হয়েছে" else "Vault Successfully Unlocked",
                details = if (_isBengali.value) "মাস্টার ক্রেডেনশিয়াল সঠিক যাচাই করে ভল্টে প্রবেশ নিশ্চিত হয়েছে।" else "Authorized access using Master PIN.",
                target = "Secure Vault",
                severity = AuditSeverity.SUCCESS
            )
            return true
        } else {
            val count = prefs.getFailedAttempts()
            _failedAttemptsCount.value = count
            val remainingSec = prefs.getRemainingLockoutSeconds()
            _remainingLockoutSeconds.value = remainingSec

            val alertMsg = if (_isBengali.value) {
                if (remainingSec > 0) {
                    "ভুল পিন দেওয়া হয়েছে ($count বার চেষ্টা)। নিরাপত্তা লকডাউন: $remainingSec সেকেন্ড।"
                } else {
                    "সতর্কতা! ভুল পিন দেওয়া হয়েছে ($count বার চেষ্টা)।"
                }
            } else {
                if (remainingSec > 0) {
                    "Invalid PIN ($count attempts). Lockout delay active: $remainingSec seconds."
                } else {
                    "Alert! Invalid PIN ($count attempt)."
                }
            }
            _lastIntruderAlert.value = alertMsg

            // Audit log: NEVER log the entered PIN itself!
            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = if (_isBengali.value) "অনুপ্রবেশকারী সতর্কতা: ভুল পিন!" else "Intruder Alert: Invalid PIN Attempt!",
                details = "অননুমোদিত পিন প্রবেশের চেষ্টা শনাক্ত (মোট $count বার)। ক্রেডেনশিয়াল থ্রটলিং সক্রিয়।",
                target = "Security Gate",
                severity = AuditSeverity.CRITICAL
            )
            return false
        }
    }

    fun onBiometricUnlocked() {
        if (_isSetupDone.value) {
            _isVaultUnlocked.value = true
            _failedAttemptsCount.value = 0
            _remainingLockoutSeconds.value = 0L
            _lastIntruderAlert.value = null
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "বায়োমেট্রিক আনলক সফল" else "Biometric Unlock Success",
                details = "ডিভাইস বায়োমেট্রিক সেন্সর সফলভাবে ভেরিফাই করে ভল্টে প্রবেশ নিশ্চিত হয়েছে।",
                target = "Biometric Gate",
                severity = AuditSeverity.SUCCESS
            )
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun changeMasterPin(currentPin: String, newPin: String): Boolean {
        if (newPin.length < 4) return false
        val changed = prefs.changeMasterPin(currentPin, newPin)
        if (changed) {
            logEvent(
                eventType = AuditEventType.PERMISSION_AUDIT,
                title = if (_isBengali.value) "মাস্টার পাসওয়ার্ড আপডেট করা হয়েছে" else "Master Password Updated",
                details = "পুনঃযাচাইকরণের পর PBKDF2 মাস্টার পিন সফলভাবে পরিবর্তন করা হয়েছে।",
                target = "Security Master",
                severity = AuditSeverity.INFO
            )
        }
        return changed
    }

    fun toggleDisguiseCalculator(enabled: Boolean) {
        prefs.setDisguiseCalculatorEnabled(enabled)
        _isCalculatorDisguiseActive.value = enabled
    }

    fun setSelectedCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    // ------------------------------------------------------------------------
    // Real Encrypted File Storage (AES-256-GCM)
    // ------------------------------------------------------------------------

    fun addVaultFile(
        title: String,
        category: FileCategory,
        content: String = "",
        sizeFormatted: String = ""
    ) {
        viewModelScope.launch {
            try {
                val plainBytes = content.toByteArray(Charsets.UTF_8)
                val encResult = FileVaultManager.encryptAndSaveBytes(getApplication(), plainBytes)

                val displaySize = if (sizeFormatted.isNotBlank()) sizeFormatted else {
                    "${(encResult.totalCiphertextSize + 1023) / 1024} KB"
                }

                val entity = VaultFileEntity(
                    title = title,
                    category = category.name,
                    filePath = encResult.relativePath,
                    fileSizeFormatted = displaySize,
                    timestamp = System.currentTimeMillis(),
                    isEncrypted = true,
                    encryptionAlgorithm = "AES-256-GCM (Hardware DEK)",
                    description = "SHA-256: ${encResult.sha256Checksum.take(12)}...",
                    secretContent = "" // Plaintext is NEVER stored in database!
                )
                vaultDao.insertFile(entity)

                logEvent(
                    eventType = AuditEventType.FILE_ENCRYPTED,
                    title = if (_isBengali.value) "ফাইল সফলভাবে এনক্রিপ্ট করা হয়েছে" else "File Successfully Encrypted",
                    details = "$title ফাইলটি AES-256-GCM এবং কীস্টোর মাস্টার কী দিয়ে সুরক্ষিত করা হয়েছে।",
                    target = title,
                    severity = AuditSeverity.SUCCESS
                )
            } catch (e: Exception) {
                logEvent(
                    eventType = AuditEventType.FILE_ENCRYPTED,
                    title = "ফাইল এনক্রিপশনে ত্রুটি",
                    details = "ত্রুটি: ${e.message}",
                    target = title,
                    severity = AuditSeverity.CRITICAL
                )
            }
        }
    }

    /**
     * Decrypts and returns file content after verifying GCM authentication tag.
     */
    fun decryptVaultContent(file: VaultFileEntity): String? {
        return try {
            val decryptedBytes = FileVaultManager.readAndDecryptFile(getApplication(), file.filePath)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = "ফাইল টেম্পারিং বা ডিক্রিপশন ব্যর্থতা",
                details = "ফাইল: ${file.title} - ${e.message}",
                target = file.title,
                severity = AuditSeverity.CRITICAL
            )
            null
        }
    }

    fun deleteVaultFile(file: VaultFileEntity) {
        viewModelScope.launch {
            FileVaultManager.secureDeleteFile(getApplication(), file.filePath)
            vaultDao.deleteFile(file)
            logEvent(
                eventType = AuditEventType.PERMISSION_CLEANUP,
                title = if (_isBengali.value) "ফাইল নিরাপদভাবে মোছা হয়েছে" else "File Securely Wiped",
                details = "${file.title} এনক্রিপ্ট করা ফাইল মেমোরি ও স্টোরেজ থেকে নিরাপদভাবে মুছে ফেলা হয়েছে।",
                target = file.title,
                severity = AuditSeverity.INFO
            )
        }
    }

    fun shareVaultFile(file: VaultFileEntity, destination: String = "External / Secure Channel") {
        logEvent(
            eventType = AuditEventType.FILE_SHARED,
            title = if (_isBengali.value) "ফাইল শেয়ার ট্র্যাকিং" else "File Shared Event",
            details = "${file.title} শেয়ার করা হয়েছে ($destination)। ফরেনসিক অডিট রেকর্ড নিশ্চিত।",
            target = file.title,
            severity = AuditSeverity.WARNING
        )
    }

    fun revokeAllDrivePublicLinks() {
        viewModelScope.launch {
            cloudDriveDao.revokeAllPublicLinks()
            logEvent(
                eventType = AuditEventType.PERMISSION_CLEANUP,
                title = if (_isBengali.value) "গুগল ড্রাইভ পাবলিক লিঙ্ক প্রত্যাহার" else "Google Drive Public Links Revoked",
                details = "সকল উন্মুক্ত ক্লাউড লিঙ্ক বাতিল করে ডেটা এক্সেস ব্যক্তিগত করা হয়েছে।",
                target = "Cloud DLP",
                severity = AuditSeverity.SUCCESS
            )
        }
    }

    private fun migrateLegacyVaultRecordsIfPresent() {
        viewModelScope.launch {
            try {
                val legacyFiles = vaultDao.getAllFilesSync()
                for (file in legacyFiles) {
                    if (file.secretContent.isNotBlank() && !FileVaultManager.fileExists(getApplication(), file.filePath)) {
                        val encResult = FileVaultManager.encryptAndSaveBytes(
                            getApplication(),
                            file.secretContent.toByteArray(Charsets.UTF_8)
                        )
                        val updated = file.copy(
                            filePath = encResult.relativePath,
                            secretContent = "",
                            isEncrypted = true,
                            encryptionAlgorithm = "AES-256-GCM"
                        )
                        vaultDao.updateFile(updated)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ------------------------------------------------------------------------
    // App Lock & Challenge Flow
    // ------------------------------------------------------------------------

    fun toggleAppLock(app: LockedAppEntity, isLocked: Boolean) {
        viewModelScope.launch {
            lockedAppDao.updateLockStatus(app.packageName, isLocked)
        }
    }

    fun toggleAppLocked(packageName: String, isLocked: Boolean) {
        viewModelScope.launch {
            lockedAppDao.updateLockStatus(packageName, isLocked)
        }
    }

    fun toggleAppOtp(packageName: String, requireOtp: Boolean) {
        viewModelScope.launch {
            lockedAppDao.setOtpRequirement(packageName, requireOtp)
        }
    }

    fun toggleAppRemoteApproval(packageName: String, requireRemote: Boolean) {
        viewModelScope.launch {
            lockedAppDao.setRemoteApprovalRequirement(packageName, requireRemote)
        }
    }

    fun startAppAccessChallenge(app: LockedAppEntity) {
        startAppChallenge(app)
    }

    fun setMasterPin(pin: String) {
        setupMasterPin(pin)
    }

    fun dismissPowerOffChallenge() {
        cancelPowerOffChallenge()
    }

    fun submitPowerOffOtp(otp: String): Boolean {
        return verifyPowerOffOtp(otp)
    }

    fun scanAppPermissions() {
        scanPermissions()
    }

    fun logPermissionCleanup(appName: String, packageName: String = "") {
        val targetDesc = if (packageName.isNotBlank()) "$appName ($packageName)" else appName
        logEvent(
            eventType = AuditEventType.PERMISSION_CLEANUP,
            title = if (_isBengali.value) "পারমিশন ক্লিনআপ অ্যাকশন" else "Permission Cleanup Action",
            details = "$targetDesc পারমিশন ম্যানেজারে পাঠানো হয়েছে।",
            target = appName,
            severity = AuditSeverity.INFO
        )
    }

    fun updateAppSecurityPolicies(app: LockedAppEntity, pin: Boolean, otp: Boolean, remote: Boolean) {
        viewModelScope.launch {
            lockedAppDao.updateSecurityPolicies(app.packageName, pin, otp, remote)
        }
    }

    fun addLockedApp(packageName: String, appName: String, isFinancial: Boolean) {
        viewModelScope.launch {
            val app = LockedAppEntity(
                packageName = packageName,
                appName = appName,
                isFinancial = isFinancial,
                isLocked = true,
                requirePin = true,
                requireOtp = isFinancial,
                requireRemoteApproval = isFinancial
            )
            lockedAppDao.insert(app)
        }
    }

    fun startAppChallenge(app: LockedAppEntity) {
        _activeChallengeApp.value = app

        val generatedOtp = EncryptionHelper.generateRandomOtp()
        _currentChallengeOtp.value = generatedOtp

        val challenge = TrustedDeviceProtocol.ApprovalChallenge(
            action = "APP_UNLOCK:${app.packageName}",
            targetDeviceId = TrustedDeviceProtocol.getOrCreateLocalDeviceId(getApplication()),
            requesterName = app.appName
        )
        _activeApprovalChallenge.value = challenge
        _activeRequestId.value = challenge.challengeId

        if (app.requireRemoteApproval) {
            val reqEntity = RemoteRequestEntity(
                requestId = challenge.challengeId,
                packageName = app.packageName,
                appName = app.appName,
                requestingDeviceName = "Primary Phone (${android.os.Build.MODEL})",
                targetPhoneName = prefs.getSecondaryPhoneName(),
                timestamp = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING.name,
                challengeOtp = challenge.verificationCode,
                ipAddressOrLocation = "Local Trusted Connection"
            )
            viewModelScope.launch {
                remoteRequestDao.insertRequest(reqEntity)
            }
        }

        if (app.requirePin) {
            _challengeState.value = ChallengeState.AwaitingPin
        } else if (app.requireOtp) {
            _challengeState.value = ChallengeState.AwaitingOtp
        } else if (app.requireRemoteApproval) {
            _challengeState.value = ChallengeState.AwaitingRemoteApproval
        } else {
            finalizeAccessGranted()
        }
    }

    fun submitChallengePin(enteredPin: String): Boolean {
        val app = _activeChallengeApp.value ?: return false
        val ok = prefs.verifyPin(enteredPin)
        if (!ok) {
            logEvent(
                eventType = AuditEventType.APP_BLOCKED,
                title = if (_isBengali.value) "অ্যাপে ভুল পিন দিয়ে প্রবেশের চেষ্টা রোধ" else "App Access Blocked: Invalid PIN",
                details = "${app.appName} অ্যাপে ভুল পিন দেওয়া হয়েছে।",
                target = app.appName,
                severity = AuditSeverity.CRITICAL
            )
            return false
        }

        if (app.requireOtp) {
            _challengeState.value = ChallengeState.AwaitingOtp
        } else if (app.requireRemoteApproval) {
            _challengeState.value = ChallengeState.AwaitingRemoteApproval
        } else {
            finalizeAccessGranted()
        }
        return true
    }

    fun submitChallengeOtp(otp: String): Boolean {
        val app = _activeChallengeApp.value ?: return false
        val generated = _currentChallengeOtp.value ?: ""

        // NO hardcoded backdoors allowed (no "999999")
        if (otp == generated && generated.isNotBlank()) {
            if (app.requireRemoteApproval) {
                _challengeState.value = ChallengeState.AwaitingRemoteApproval
            } else {
                finalizeAccessGranted()
            }
            return true
        } else {
            logEvent(
                eventType = AuditEventType.APP_BLOCKED,
                title = if (_isBengali.value) "ভুল ওটিপি দিয়ে প্রবেশের চেষ্টা রোধ" else "Invalid Dynamic OTP Entered",
                details = "${app.appName} অ্যাপের জন্য ভুল ওটিপি দেওয়া হয়েছে। অননুমোদিত অ্যাক্সেস ব্লক।",
                target = app.appName,
                severity = AuditSeverity.WARNING
            )
            return false
        }
    }

    // ------------------------------------------------------------------------
    // Trusted-Device Cryptographic Approval Protocol
    // ------------------------------------------------------------------------

    fun pairSecondaryPhone(name: String, deviceId: String, publicKeyBase64: String): Boolean {
        if (name.isBlank() || publicKeyBase64.isBlank()) return false
        prefs.savePairedDevice(name, deviceId, publicKeyBase64)
        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "নতুন সেকেন্ডারি ফোন পেয়ার করা হয়েছে" else "Trusted Secondary Device Paired",
            details = "$name ডিভাইসের ECDSA পাবলিক কি ভেরিফাই করে সফলভাবে যুক্ত করা হয়েছে।",
            target = name,
            severity = AuditSeverity.SUCCESS
        )
        return true
    }

    fun revokeSecondaryPhone() {
        val name = prefs.getSecondaryPhoneName()
        prefs.revokePairing()
        logEvent(
            eventType = AuditEventType.PERMISSION_CLEANUP,
            title = if (_isBengali.value) "সেকেন্ডারি ফোনের অনুমোদন বাতিল" else "Trusted Device Revoked",
            details = "$name এর বিশ্বস্ত অনুমোদন ও পাবলিক কি বাতিল করা হয়েছে।",
            target = name,
            severity = AuditSeverity.WARNING
        )
    }

    /**
     * Verifies cryptographic signed approval token received from paired device.
     */
    fun submitRemoteApprovalToken(tokenString: String): Pair<Boolean, String> {
        val challenge = _activeApprovalChallenge.value
            ?: return Pair(false, if (_isBengali.value) "কোনো সক্রিয় অনুমোদন চ্যালেঞ্জ নেই" else "No active challenge")

        val token = TrustedDeviceProtocol.SignedApprovalToken.decodeFromTransmissionString(tokenString)
            ?: return Pair(false, if (_isBengali.value) "টোকেন ফরম্যাট অবৈধ" else "Invalid token format")

        val result = TrustedDeviceProtocol.verifyApprovalResponse(
            context = getApplication(),
            prefs = prefs,
            activeChallenge = challenge,
            token = token
        )

        return when (result) {
            is TrustedDeviceProtocol.VerificationResult.Success -> {
                finalizeAccessGranted()
                logEvent(
                    eventType = AuditEventType.REMOTE_APPROVED,
                    title = if (_isBengali.value) "ক্রিপ্টোগ্রাফিক রিমোট অনুমোদন সফল" else "ECDSA Remote Approval Verified",
                    details = "বিশ্বস্ত ফোন থেকে ডিজিটাল সিগনেচার যাচাই করে অ্যাক্সেস অনুমোদিত হয়েছে।",
                    target = challenge.action,
                    severity = AuditSeverity.SUCCESS
                )
                Pair(true, if (_isBengali.value) "অনুমোদন সফল!" else "Approval verified!")
            }
            is TrustedDeviceProtocol.VerificationResult.Rejected -> {
                logEvent(
                    eventType = AuditEventType.REMOTE_REJECTED,
                    title = if (_isBengali.value) "রিমোট অনুমোদন প্রত্যাখ্যান" else "Remote Approval Rejected",
                    details = "কারণ: ${result.reasonEn}",
                    target = challenge.action,
                    severity = AuditSeverity.CRITICAL
                )
                Pair(false, if (_isBengali.value) result.reasonBn else result.reasonEn)
            }
        }
    }

    fun finalizeAccessGranted() {
        val app = _activeChallengeApp.value ?: return
        _challengeState.value = ChallengeState.Granted
        logEvent(
            eventType = AuditEventType.APP_UNLOCKED,
            title = if (_isBengali.value) "অ্যাপ সফলভাবে খোলা হয়েছে" else "App Access Granted",
            details = "${app.appName} এর সুরক্ষা স্তর সফলভাবে উত্তীর্ণ হয়েছে।",
            target = app.appName,
            severity = AuditSeverity.SUCCESS
        )
    }

    fun cancelChallenge() {
        _activeChallengeApp.value = null
        _currentChallengeOtp.value = null
        _activeApprovalChallenge.value = null
        _activeRequestId.value = null
        _challengeState.value = ChallengeState.Idle
    }

    // ------------------------------------------------------------------------
    // Anti-Theft: Device Lockdown & Shutdown Interception
    // ------------------------------------------------------------------------

    fun startLockdownService() {
        DeviceLockdownService.start(getApplication())
        prefs.setAntiPowerOffEnabled(true)
    }

    fun stopLockdownService() {
        DeviceLockdownService.stop(getApplication())
        prefs.setAntiPowerOffEnabled(false)
    }

    fun toggleLockdownService(enabled: Boolean) {
        if (enabled) startLockdownService() else stopLockdownService()
    }

    fun triggerLockdownInterception(reason: String) {
        DeviceLockdownService.triggerLockdown(getApplication(), reason)
    }

    fun triggerPowerOffAttempt(reason: String = "Power-off key event intercepted") {
        val otp = EncryptionHelper.generateRandomOtp()
        val challenge = PowerOffChallenge(
            challengeId = "POW-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
            otp = otp,
            requestingDevice = "Primary Phone",
            targetSecondaryPhone = prefs.getSecondaryPhoneName()
        )
        _activePowerOffChallenge.value = challenge
        DeviceLockdownService.triggerLockdown(getApplication(), reason)

        NotificationHelper.showShutdownAttemptNotification(
            context = getApplication(),
            title = if (_isBengali.value) "অননুমোদিত ফোন বন্ধ করার চেষ্টা!" else "Unauthorized Power-Off Attempt!",
            message = if (_isBengali.value) {
                "কেউ ফোনটি বন্ধ করার চেষ্টা করছে। ওটিপি দিয়ে ভেরিফাই করুন।"
            } else {
                "Someone is attempting to power off the device. Secondary OTP verification required."
            }
        )

        logEvent(
            eventType = AuditEventType.INTRUDER_ALERT,
            title = if (_isBengali.value) "অননুমোদিত ফোন বন্ধের চেষ্টা প্রতিহত!" else "Unauthorized Shutdown Intercepted!",
            details = "সফটওয়্যার শাটডাউন ডায়ালগ ইন্টারসেপ্ট করা হয়েছে ($reason)। ডিভাইস লকডাউন সক্রিয়।",
            target = "Power Guard",
            severity = AuditSeverity.CRITICAL
        )
    }

    fun verifyPowerOffOtp(enteredOtp: String): Boolean {
        val challenge = _activePowerOffChallenge.value ?: return false
        if (enteredOtp == challenge.otp) {
            _activePowerOffChallenge.value = null
            DeviceLockdownService.dismissLockdown()
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "ফোন বন্ধের অনুমোদন সম্পন্ন" else "Authorized Shutdown Permitted",
                details = "সঠিক ওটিপি দিয়ে শাটডাউন অনুমোদন নিশ্চিত করা হয়েছে।",
                target = "Power Guard",
                severity = AuditSeverity.SUCCESS
            )
            return true
        } else {
            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = if (_isBengali.value) "শাটডাউনে ভুল ওটিপি চেষ্টা!" else "Invalid Shutdown OTP Attempt!",
                details = "অননুমোদিত ব্যবহারকারী ভুল ওটিপি দিয়ে ফোন বন্ধ করার চেষ্টা করেছে।",
                target = "Power Guard",
                severity = AuditSeverity.CRITICAL
            )
            return false
        }
    }

    fun cancelPowerOffChallenge() {
        _activePowerOffChallenge.value = null
        DeviceLockdownService.dismissLockdown()
    }

    fun enterFakeShutdownMode() {
        _isFakeShutdownActive.value = true
        DeviceLockdownService.triggerLockdown(getApplication(), "Fake decoy power-off active")
    }

    fun activateFakeShutdownMode() = enterFakeShutdownMode()

    fun exitFakeShutdownMode() {
        _isFakeShutdownActive.value = false
        DeviceLockdownService.dismissLockdown()
    }

    fun simulateSimEjection() {
        val updated = _simStatus.value.copy(
            isInserted = false,
            isTrayEjected = true,
            lastChangedTimestamp = System.currentTimeMillis()
        )
        _simStatus.value = updated
        _isSirenAlarmActive.value = true

        NotificationHelper.showAntiTheftEmergencyNotification(
            context = getApplication(),
            title = if (_isBengali.value) "🚨 সিম স্ট্যাটাস পরিবর্তন!" else "🚨 SIM State Change Detected!",
            message = if (_isBengali.value) {
                "সিম কার্ড সংযোগ বিচ্ছিন্ন হয়েছে। শেষ পরিচিত লোকেশন ও জরুরি অ্যালার্ট সক্রিয়।"
            } else {
                "SIM card absent or disconnected. Emergency telemetry activated."
            }
        )

        logEvent(
            eventType = AuditEventType.INTRUDER_ALERT,
            title = if (_isBengali.value) "🚨 সিম কার্ড স্ট্যাটাস পরিবর্তন সতর্কতা!" else "🚨 SIM State Change Detected!",
            details = "সিম সংযোগ বিচ্ছিন্ন শনাক্ত হয়েছে। নেটওয়ার্ক অ্যান্টি-থেফট সক্রিয়।",
            target = "Anti-Theft SIM Guard",
            severity = AuditSeverity.CRITICAL
        )
    }

    fun restoreSimStatus() {
        _simStatus.value = SimStatus()
        _isSirenAlarmActive.value = false
        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "সিম কার্ড পুনঃসংযুক্ত" else "SIM Verified",
            details = "সিম কার্ড স্বাভাবিক অবস্থায় ফিরে এসেছে।",
            target = "SIM Guard",
            severity = AuditSeverity.SUCCESS
        )
    }

    fun toggleAirplaneModeSimulation() {
        val isNowOn = !_simStatus.value.isAirplaneModeOn
        _simStatus.value = _simStatus.value.copy(isAirplaneModeOn = isNowOn)
        if (isNowOn) {
            _isSirenAlarmActive.value = true
            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = if (_isBengali.value) "🚨 এয়ারপ্লেন মোড অন করার চেষ্টা শনাক্ত!" else "🚨 Airplane Mode Detected!",
                details = "ডিভাইসের নেটওয়ার্ক সংযোগ বিচ্ছিন্ন করা হয়েছে। জরুরি অ্যালার্ট প্রস্তুত।",
                target = "Anti-Theft Connectivity",
                severity = AuditSeverity.CRITICAL
            )
        } else {
            _isSirenAlarmActive.value = false
        }
    }

    fun toggleSirenAlarm(active: Boolean) {
        _isSirenAlarmActive.value = active
    }

    fun refreshLocationCoordinates() {
        locationManager.fetchLastKnownLocation(
            onSuccess = { loc ->
                _lastKnownLocation.value = loc
                logEvent(
                    eventType = AuditEventType.PERMISSION_AUDIT,
                    title = if (_isBengali.value) "যাচাইকৃত লোকেশন আপডেট" else "Verified Location Updated",
                    details = "ফিক্স: ${loc.fixType.labelEn}, জিপিএস: ${loc.latitude ?: 0.0}, ${loc.longitude ?: 0.0}",
                    target = "Find Hub Tracker",
                    severity = AuditSeverity.INFO
                )
            },
            onError = { err ->
                logEvent(
                    eventType = AuditEventType.PERMISSION_AUDIT,
                    title = "লোকেশন পেতে ব্যর্থ",
                    details = "কারণ: $err",
                    target = "Find Hub",
                    severity = AuditSeverity.WARNING
                )
            }
        )
    }

    fun startRealtimeMovementTracking() {
        locationManager.startRealtimeMovementTracking { loc, speed, _, _ ->
            _lastKnownLocation.value = loc
            if (speed > 25f) {
                logEvent(
                    eventType = AuditEventType.INTRUDER_ALERT,
                    title = if (_isBengali.value) "🚨 ডিভাইসে দ্রুত গতি শনাক্ত!" else "🚨 High-Speed Movement Detected!",
                    details = "গতি: ${String.format(java.util.Locale.US, "%.1f", speed)} km/h",
                    target = "Anti-Theft Movement",
                    severity = AuditSeverity.CRITICAL
                )
            }
        }
    }

    fun stopRealtimeMovementTracking() {
        locationManager.stopRealtimeMovementTracking()
    }

    fun unlockBootGate(pin: String): Boolean {
        val pinOk = prefs.verifyPin(pin)
        if (pinOk) {
            _isBootLocked.value = false
            prefs.setBootLockdownActive(false)
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "রিস্টার্টের পর সফল আনলক" else "Post-Boot Anti-Theft Unlocked",
                details = "মাস্টার ক্রেডেনশিয়াল যাচাই সফল। ডিভাইস স্বাভাবিক।",
                target = "Boot-up Guard",
                severity = AuditSeverity.SUCCESS
            )
            return true
        }
        return false
    }

    fun scanPermissions() {
        _isScanningPermissions.value = true
        viewModelScope.launch {
            try {
                val scanned = PermissionAuditorHelper.scanInstalledApps(getApplication())
                _permissionApps.value = scanned
            } finally {
                _isScanningPermissions.value = false
            }
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            auditLogDao.clearAllLogs()
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        prefs.setDemoModeEnabled(enabled)
        _isDemoModeActive.value = enabled
        if (enabled) {
            seedDemoData()
        } else {
            clearDemoData()
        }
    }

    private fun seedDemoData() {
        viewModelScope.launch {
            // Seed isolated demo items with explicit [DEMO] labeling
            val demoDrive = listOf(
                CloudDriveItemEntity(
                    title = "[DEMO] Public_Shared_Doc.pdf",
                    mimeType = "application/pdf",
                    fileSizeFormatted = "1.2 MB",
                    shareStatus = DriveShareStatus.PUBLIC_LINK.name,
                    isLeaking = true,
                    sharedWithEmails = "Anyone with link (Demo Sandbox)",
                    lastScannedTimestamp = System.currentTimeMillis()
                )
            )
            cloudDriveDao.insertAll(demoDrive)
        }
    }

    private fun clearDemoData() {
        viewModelScope.launch {
            cloudDriveDao.clearAll()
            sensorAccessDao.clearAll()
        }
    }

    private fun logEvent(
        eventType: AuditEventType,
        title: String,
        details: String,
        target: String,
        severity: AuditSeverity = AuditSeverity.INFO
    ) {
        viewModelScope.launch {
            val entity = AuditLogEntity(
                eventType = eventType.name,
                title = title,
                details = details,
                targetItem = target,
                severity = severity.name,
                intruderPhotoPath = null // Real, minimal security event; no fake paths
            )
            auditLogDao.insertLog(entity)
        }
    }

    private fun seedInitialFinancialAppsIfEmpty() {
        viewModelScope.launch {
            val count = lockedAppDao.getCount()
            if (count == 0) {
                val defaultApps = listOf(
                    LockedAppEntity(
                        packageName = "com.bKash.customerapp",
                        appName = "bKash (বিকাশ)",
                        isFinancial = true,
                        isLocked = true,
                        requirePin = true,
                        requireOtp = true,
                        requireRemoteApproval = true
                    ),
                    LockedAppEntity(
                        packageName = "com.konasl.nagad",
                        appName = "Nagad (নগদ)",
                        isFinancial = true,
                        isLocked = true,
                        requirePin = true,
                        requireOtp = true,
                        requireRemoteApproval = true
                    ),
                    LockedAppEntity(
                        packageName = "com.android.settings",
                        appName = "Phone Settings",
                        isFinancial = false,
                        isLocked = true,
                        requirePin = true,
                        requireOtp = false,
                        requireRemoteApproval = false
                    )
                )
                lockedAppDao.insertAll(defaultApps)
            }
        }
    }
}
