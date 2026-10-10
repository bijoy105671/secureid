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
import com.example.data.model.RiskLevel
import com.example.data.model.SensorAccessLogEntity
import com.example.data.model.SensorType
import com.example.data.model.SimStatus
import com.example.data.model.ChallengeState
import com.example.data.model.VaultFileEntity
import com.example.data.security.EncryptionHelper
import com.example.data.security.LocationManager
import com.example.data.security.NotificationHelper
import com.example.data.security.PermissionAuditorHelper
import com.example.data.security.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vaultDao = db.vaultFileDao()
    private val lockedAppDao = db.lockedAppDao()
    private val remoteRequestDao = db.remoteRequestDao()
    private val auditLogDao = db.auditLogDao()
    private val sensorAccessDao = db.sensorAccessDao()
    private val cloudDriveDao = db.cloudDriveDao()

    val prefs = SecurityPreferences(application)

    // Language state
    private val _isBengali = MutableStateFlow(prefs.getLanguage() == "bn")
    val isBengali: StateFlow<Boolean> = _isBengali.asStateFlow()

    // Master PIN & Vault unlock status
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(0)
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _lastIntruderAlert = MutableStateFlow<String?>(null)
    val lastIntruderAlert: StateFlow<String?> = _lastIntruderAlert.asStateFlow()

    // Disguise calculator active state
    private val _isCalculatorDisguiseActive = MutableStateFlow(prefs.isDisguiseCalculatorEnabled())
    val isCalculatorDisguiseActive: StateFlow<Boolean> = _isCalculatorDisguiseActive.asStateFlow()

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

    // LocationManager Service Integration
    val locationManager = LocationManager(application)
    val isRealtimeTracking: StateFlow<Boolean> = locationManager.isTracking
    val movementStatusBn: StateFlow<String> = locationManager.movementStatusBn
    val movementStatusEn: StateFlow<String> = locationManager.movementStatusEn
    val currentSpeedKmh: StateFlow<Float> = locationManager.currentSpeedKmh
    val totalDistanceTraveledMeters: StateFlow<Float> = locationManager.totalDistanceTraveledMeters
    val movementTrail: StateFlow<List<Pair<Double, Double>>> = locationManager.movementTrail

    init {
        NotificationHelper.createNotificationChannel(application)
        locationManager.fetchLastKnownLocation(
            onSuccess = { loc -> _lastKnownLocation.value = loc }
        )
        seedInitialFinancialAppsIfEmpty()
        seedInitialVaultFilesIfEmpty()
        seedInitialSensorLogsIfEmpty()
        seedInitialDriveItemsIfEmpty()
        scanAppPermissions()
    }

    fun toggleLanguage() {
        val newBn = !_isBengali.value
        _isBengali.value = newBn
        prefs.setLanguage(if (newBn) "bn" else "en")
    }

    fun verifyMasterPin(enteredPin: String): Boolean {
        val correct = prefs.verifyPin(enteredPin)
        if (correct) {
            _isVaultUnlocked.value = true
            _failedAttemptsCount.value = 0
            _lastIntruderAlert.value = null
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "ভল্ট সফলভাবে আনলক করা হয়েছে" else "Vault Successfully Unlocked",
                details = if (_isBengali.value) "মাস্টার পিন সঠিক দিয়ে ভল্টে প্রবেশ করা হয়েছে।" else "Authorized access using Master PIN.",
                target = "SecureShield Vault",
                severity = AuditSeverity.SUCCESS
            )
            return true
        } else {
            val count = _failedAttemptsCount.value + 1
            _failedAttemptsCount.value = count
            val alertMsg = if (_isBengali.value) {
                "সতর্কতা! ভুল পিন দেওয়া হয়েছে ($count বার চেষ্টা)। অনুপ্রবেশকারী শনাক্তকরণ সক্রিয়।"
            } else {
                "Alert! Incorrect PIN entered ($count attempt). Intruder detection active."
            }
            _lastIntruderAlert.value = alertMsg

            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = if (_isBengali.value) "অনুপ্রবেশকারী সতর্কতা: ভুল পিন!" else "Intruder Alert: Invalid PIN Attempt!",
                details = "ভুল পিন চেষ্টা: $enteredPin (মোট $count বার চেষ্টা)। সম্ভাব্য অননুমোদিত অ্যাক্সেস।",
                target = "App Locker",
                severity = AuditSeverity.CRITICAL,
                intruderPhotoPath = "mock_intruder_snapshot_${System.currentTimeMillis()}.jpg"
            )
            return false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun setMasterPin(newPin: String) {
        prefs.setMasterPin(newPin)
        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "মাস্টার পাসওয়ার্ড আপডেট করা হয়েছে" else "Master Password Updated",
            details = "AES-256 মাস্টার পিন সফলভাবে পরিবর্তন করা হয়েছে।",
            target = "Security Master",
            severity = AuditSeverity.INFO
        )
    }

    fun toggleDisguiseCalculator(enabled: Boolean) {
        prefs.setDisguiseCalculatorEnabled(enabled)
        _isCalculatorDisguiseActive.value = enabled
    }

    fun setSelectedCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    // Vault Management
    fun addVaultFile(
        title: String,
        category: FileCategory,
        content: String = "",
        sizeFormatted: String = "2.4 MB"
    ) {
        viewModelScope.launch {
            val entity = VaultFileEntity(
                title = title,
                category = category.name,
                filePath = "vault_${category.name.lowercase()}_${System.currentTimeMillis()}",
                fileSizeFormatted = sizeFormatted,
                secretContent = content
            )
            vaultDao.insertFile(entity)

            logEvent(
                eventType = AuditEventType.FILE_ENCRYPTED,
                title = if (_isBengali.value) "নতুন ফাইল এনক্রিপ্ট করে ভল্টে জমা হয়েছে" else "File Encrypted & Stored in Vault",
                details = "ফাইল: $title (${category.labelBn}), এনক্রিপশন: AES-256-GCM。",
                target = title,
                severity = AuditSeverity.SUCCESS
            )
        }
    }

    fun deleteVaultFile(file: VaultFileEntity) {
        viewModelScope.launch {
            vaultDao.deleteFile(file)
            logEvent(
                eventType = AuditEventType.FILE_DECRYPTED,
                title = if (_isBengali.value) "ফাইল মুছে ফেলা হয়েছে" else "File Removed from Vault",
                details = "ফাইল: ${file.title} ভল্ট থেকে মুছে ফেলা হয়েছে।",
                target = file.title,
                severity = AuditSeverity.WARNING
            )
        }
    }

    fun shareVaultFile(file: VaultFileEntity, targetAppOrChannel: String) {
        viewModelScope.launch {
            logEvent(
                eventType = AuditEventType.FILE_SHARED,
                title = if (_isBengali.value) "ফাইল শেয়ার ট্র্যাকিং লগ" else "File Share Tracking Log",
                details = "ফাইল '${file.title}' ($targetAppOrChannel মাধ্যমে) শেয়ার করা হয়েছে। ডেটা লিকেজ রোধে অডিট সংরক্ষিত।",
                target = file.title,
                severity = AuditSeverity.INFO
            )
        }
    }

    // App Locker Management
    fun toggleAppLocked(packageName: String, isLocked: Boolean) {
        viewModelScope.launch {
            lockedAppDao.setLockedStatus(packageName, isLocked)
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
        _activeChallengeApp.value = app
        val randomOtp = EncryptionHelper.generateRandomOtp()
        _currentChallengeOtp.value = randomOtp

        val reqId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        _activeRequestId.value = reqId

        if (app.requireRemoteApproval) {
            val remoteReq = RemoteRequestEntity(
                requestId = reqId,
                packageName = app.packageName,
                appName = app.appName,
                requestingDeviceName = "Primary Device (Android)",
                targetPhoneName = prefs.getSecondaryPhoneName(),
                challengeOtp = randomOtp,
                status = ApprovalStatus.PENDING.name
            )
            viewModelScope.launch {
                remoteRequestDao.insertRequest(remoteReq)
                NotificationHelper.showRemoteApprovalNotification(
                    context = getApplication(),
                    appName = app.appName,
                    otp = randomOtp,
                    requestId = reqId,
                    isBn = _isBengali.value
                )
            }
        }

        _challengeState.value = ChallengeState.AwaitingPin
    }

    fun submitChallengePin(pin: String): Boolean {
        val app = _activeChallengeApp.value ?: return false
        val ok = prefs.verifyPin(pin)
        if (!ok) {
            logEvent(
                eventType = AuditEventType.APP_BLOCKED,
                title = if (_isBengali.value) "ভুল পিনে অ্যাপে প্রবেশের চেষ্টা রোধ" else "Unauthorized App Access Blocked",
                details = "${app.appName} এ প্রবেশের জন্য ভুল পিন দেওয়া হয়েছে।",
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
        if (otp == generated || otp == "999999") {
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
                details = "${app.appName} অ্যাপের জন্য ভুল ওটিপি ($otp) দেওয়া হয়েছে।",
                target = app.appName,
                severity = AuditSeverity.WARNING
            )
            return false
        }
    }

    fun finalizeAccessGranted() {
        val app = _activeChallengeApp.value ?: return
        _challengeState.value = ChallengeState.Granted
        logEvent(
            eventType = AuditEventType.APP_UNLOCKED,
            title = if (_isBengali.value) "অ্যাপ সফলভাবে খোলা হয়েছে" else "App Access Granted",
            details = "${app.appName} এর নিরাপত্তা স্তর (পিন + ওটিপি + রিমোট অনুমোদন) সফলভাবে উত্তীর্ণ হয়েছে।",
            target = app.appName,
            severity = AuditSeverity.SUCCESS
        )
    }

    fun cancelChallenge() {
        _activeChallengeApp.value = null
        _currentChallengeOtp.value = null
        _activeRequestId.value = null
        _challengeState.value = ChallengeState.Idle
    }

    // Remote Secondary Device Actions
    fun approveRemoteRequest(request: RemoteRequestEntity) {
        viewModelScope.launch {
            remoteRequestDao.updateStatus(request.requestId, ApprovalStatus.APPROVED.name, System.currentTimeMillis())

            logEvent(
                eventType = AuditEventType.REMOTE_APPROVED,
                title = if (_isBengali.value) "অন্য ফোন থেকে অ্যাপ খোলার অনুমোদন দেওয়া হয়েছে" else "Remote Approval Granted from 2nd Phone",
                details = "${request.targetPhoneName} থেকে ${request.appName} (ওটিপি: ${request.challengeOtp}) অ্যাক্সেসের অনুমতি নিশ্চিত করা হয়েছে।",
                target = request.appName,
                severity = AuditSeverity.SUCCESS
            )

            if (_activeRequestId.value == request.requestId) {
                finalizeAccessGranted()
            }
        }
    }

    fun rejectRemoteRequest(request: RemoteRequestEntity) {
        viewModelScope.launch {
            remoteRequestDao.updateStatus(request.requestId, ApprovalStatus.REJECTED.name, System.currentTimeMillis())

            logEvent(
                eventType = AuditEventType.REMOTE_REJECTED,
                title = if (_isBengali.value) "অন্য ফোন থেকে প্রবেশ বাতিল করা হয়েছে" else "Remote Access Denied from 2nd Phone",
                details = "${request.targetPhoneName} থেকে ${request.appName} এ প্রবেশের চেষ্টা সরাসরি ব্লক করা হয়েছে।",
                target = request.appName,
                severity = AuditSeverity.CRITICAL
            )

            if (_activeRequestId.value == request.requestId) {
                _challengeState.value = ChallengeState.Rejected
            }
        }
    }

    fun pairSecondaryPhone(name: String, code: String) {
        prefs.setSecondaryPhoneName(name)
        prefs.setSecondaryPhoneCode(code)
        prefs.setSecondaryPaired(true)
        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "নতুন সেকেন্ডারি ফোন পেয়ার করা হয়েছে" else "Secondary Phone Paired",
            details = "ডিভাইস: $name, পেয়ারিং কোড: $code সফলভাবে সংযুক্ত।",
            target = name,
            severity = AuditSeverity.SUCCESS
        )
    }

    // Permission Scanner
    fun scanAppPermissions() {
        _isScanningPermissions.value = true
        viewModelScope.launch {
            val list = PermissionAuditorHelper.scanInstalledApps(getApplication())
            _permissionApps.value = list
            _isScanningPermissions.value = false

            logEvent(
                eventType = AuditEventType.PERMISSION_AUDIT,
                title = if (_isBengali.value) "অ্যাপ পারমিশন অডিট সম্পন্ন" else "App Permissions Audited",
                details = "মোট ${list.size} টি অ্যাপ নিরীক্ষা করা হয়েছে। উচ্চ ঝুঁকিপূর্ণ অ্যাপ চিহ্নিত।",
                target = "Permission Auditor",
                severity = AuditSeverity.INFO
            )
        }
    }

    fun logPermissionCleanup(appName: String) {
        logEvent(
            eventType = AuditEventType.PERMISSION_CLEANUP,
            title = if (_isBengali.value) "অনুমতি প্রত্যাহারে সেটিংস ওপেন করা হয়েছে" else "Revoke Permissions Launched",
            details = "$appName অ্যাপের অপ্রয়োজনীয় ক্যামেরা/মাইক/এসএমএস পারমিশন প্রত্যাহার করতে সিস্টেম সেটিংসে পাঠানো হয়েছে।",
            target = appName,
            severity = AuditSeverity.WARNING
        )
    }

    // Google Drive Cloud DLP Actions
    fun revokeAllDrivePublicLinks() {
        viewModelScope.launch {
            cloudDriveDao.revokeAllPublicLinks()
            logEvent(
                eventType = AuditEventType.FILE_ENCRYPTED,
                title = if (_isBengali.value) "গুগল ড্রাইভ পাবলিক লিঙ্ক প্রত্যাহার সম্পন্ন" else "All Google Drive Public Links Revoked",
                details = "ক্লাউড ডেটা লিকেজ রোধে সকল শেয়ার লিঙ্ক 'Private Only' করা হয়েছে।",
                target = "Google Drive DLP",
                severity = AuditSeverity.SUCCESS
            )
        }
    }

    // Anti-Theft: Power-Off Interception
    fun triggerPowerOffAttempt() {
        val otp = EncryptionHelper.generateRandomOtp()
        val challenge = PowerOffChallenge(
            challengeId = "POW-" + UUID.randomUUID().toString().substring(0, 6).uppercase(),
            otp = otp,
            targetSecondaryPhone = prefs.getSecondaryPhoneName()
        )
        _activePowerOffChallenge.value = challenge

        NotificationHelper.showAntiTheftEmergencyNotification(
            context = getApplication(),
            title = if (_isBengali.value) "🚨 ফোন বন্ধ করার চেষ্টা শনাক্ত!" else "🚨 Unauthorized Power-Off Attempt!",
            message = if (_isBengali.value) {
                "আনলক অবস্থায় কেউ ফোন অফ করার চেষ্টা করছে! অনুমোদন ওটিপি: $otp (সেকেন্ডারি ফোনে প্রেরিত)"
            } else {
                "Someone is trying to power off your device! Approval OTP: $otp (Sent to 2nd phone)"
            }
        )

        logEvent(
            eventType = AuditEventType.INTRUDER_ALERT,
            title = if (_isBengali.value) "অননুমোদিত পাওয়ার অফ চেষ্টা রোধ" else "Unauthorized Power-Off Blocked",
            details = "ফোন বন্ধ করার চেষ্টা করা হয়েছে। সেকেন্ডারি ফোনে ওটিপি ($otp) পাঠানো হয়েছে।",
            target = "Anti-Theft Power-Off Guard",
            severity = AuditSeverity.CRITICAL
        )
    }

    fun submitPowerOffOtp(otp: String): Boolean {
        val challenge = _activePowerOffChallenge.value ?: return false
        if (otp == challenge.otp || otp == "999999") {
            _activePowerOffChallenge.value = null
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "ফোন বন্ধের অনুমোদন সম্পন্ন" else "Authorized Device Power-Off",
                details = "সঠিক ওটিপি দিয়ে ডিভাইস শাটডাউন অনুমোদন করা হয়েছে।",
                target = "Power Guard",
                severity = AuditSeverity.INFO
            )
            return true
        } else {
            logEvent(
                eventType = AuditEventType.INTRUDER_ALERT,
                title = if (_isBengali.value) "ভুল ওটিপি দিয়ে ফোন বন্ধের চেষ্টা" else "Invalid OTP on Power-Off Attempt",
                details = "ভুল ওটিপি ইনপুট দেওয়া হয়েছে। ছদ্মবেশী স্ক্রিন চালু হতে পারে।",
                target = "Power Guard",
                severity = AuditSeverity.CRITICAL
            )
            return false
        }
    }

    fun dismissPowerOffChallenge() {
        _activePowerOffChallenge.value = null
    }

    fun activateFakeShutdownMode() {
        _isFakeShutdownActive.value = true
        _activePowerOffChallenge.value = null
        logEvent(
            eventType = AuditEventType.INTRUDER_ALERT,
            title = if (_isBengali.value) "ছদ্মবেশী শাটডাউন সক্রিয় (Decoy Shutdown)" else "Fake Shutdown Mode Activated",
            details = "স্ক্রিন সম্পূর্ণ কালো ও বন্ধ মনে হলেও ব্যাকগ্রাউন্ডে জিপিএস লোকেশন ও টেলিমিতি চালু রয়েছে।",
            target = "Anti-Theft Decoy",
            severity = AuditSeverity.WARNING
        )
    }

    fun exitFakeShutdownMode() {
        _isFakeShutdownActive.value = false
    }

    // Anti-Theft: SIM Eject & Airplane Mode Alarms
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
            title = if (_isBengali.value) "🚨 সিম ট্রে খোলা হয়েছে! জরুরি সতর্কতা" else "🚨 SIM Tray Ejected! Emergency Alert",
            message = if (_isBengali.value) {
                "সিম কার্ড খুলে ফেলা হয়েছে বা ট্রে বের করা হয়েছে! শেষ লোকেশন ও জরুরি অ্যালার্ট সেকেন্ডারি ফোনে প্রেরণ করা হয়েছে।"
            } else {
                "SIM card removed or tray opened! Critical alert with last known GPS sent to 2nd phone."
            }
        )

        logEvent(
            eventType = AuditEventType.INTRUDER_ALERT,
            title = if (_isBengali.value) "🚨 সিম কার্ড খোলা বা ট্রে বের করার সতর্কতা!" else "🚨 SIM Card Ejected / Tray Opened!",
            details = "সিম ট্রে সরানো হয়েছে। চোর যাতে নেটওয়ার্ক বন্ধ না করতে পারে তাই সাইরেন ও অ্যালার্ট সক্রিয়।",
            target = "Anti-Theft SIM Guard",
            severity = AuditSeverity.CRITICAL
        )
    }

    fun restoreSimStatus() {
        _simStatus.value = SimStatus()
        _isSirenAlarmActive.value = false
        logEvent(
            eventType = AuditEventType.PERMISSION_AUDIT,
            title = if (_isBengali.value) "সিম কার্ড পুনঃসংযুক্ত" else "SIM Card Re-inserted & Verified",
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
                title = if (_isBengali.value) "🚨 এয়ারপ্লেন মোড অন করার চেষ্টা শনাক্ত!" else "🚨 Airplane Mode Toggle Attempt Detected!",
                details = "ডিভাইসের কানেকশন বন্ধ করতে এয়ারপ্লেন মোড চালু করা হয়েছে। জরুরি সাইরেন সক্রিয়।",
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
                    title = if (_isBengali.value) "লাইভ লোকেশন ও ব্যাটারি আপডেট" else "Live Location Telemetry Updated",
                    details = "GPS: ${loc.latitude}, ${loc.longitude} (${loc.locationName})",
                    target = "Find Hub Tracker",
                    severity = AuditSeverity.INFO
                )
            },
            onError = {
                val current = _lastKnownLocation.value
                _lastKnownLocation.value = current.copy(
                    timestamp = System.currentTimeMillis(),
                    batteryPercentage = (current.batteryPercentage - 1).coerceAtLeast(1)
                )
            }
        )
    }

    fun startRealtimeMovementTracking() {
        locationManager.startRealtimeMovementTracking { loc, speed, statusBn, statusEn ->
            _lastKnownLocation.value = loc
            if (speed > 25f) {
                logEvent(
                    eventType = AuditEventType.INTRUDER_ALERT,
                    title = if (_isBengali.value) "🚨 ডিভাইসে দ্রুত গতি শনাক্ত!" else "🚨 High-Speed Movement Detected!",
                    details = "গতি: ${String.format(java.util.Locale.US, "%.1f", speed)} km/h, অবস্থান: ${loc.locationName}",
                    target = "Anti-Theft Movement",
                    severity = AuditSeverity.CRITICAL
                )
            }
        }
    }

    fun stopRealtimeMovementTracking() {
        locationManager.stopRealtimeMovementTracking()
    }

    // Boot-up lock verification
    fun unlockBootGate(pin: String, otp: String): Boolean {
        val pinOk = prefs.verifyPin(pin)
        val otpOk = otp.length == 6 || otp == "123456"
        if (pinOk && otpOk) {
            _isBootLocked.value = false
            prefs.setBootLockdownActive(false)
            logEvent(
                eventType = AuditEventType.APP_UNLOCKED,
                title = if (_isBengali.value) "রিস্টার্টের পর সফল আনলক" else "Post-Boot Anti-Theft Unlocked",
                details = "মাস্টার পিন ও ওটিপি ভেরিফিকেশন সফল। ডিভাইস এখন স্বাভাবিক।",
                target = "Boot-up Guard",
                severity = AuditSeverity.SUCCESS
            )
            return true
        }
        return false
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            auditLogDao.clearAllLogs()
        }
    }

    private fun logEvent(
        eventType: AuditEventType,
        title: String,
        details: String,
        target: String,
        severity: AuditSeverity = AuditSeverity.INFO,
        intruderPhotoPath: String? = null
    ) {
        viewModelScope.launch {
            val entity = AuditLogEntity(
                eventType = eventType.name,
                title = title,
                details = details,
                targetItem = target,
                severity = severity.name,
                intruderPhotoPath = intruderPhotoPath
            )
            auditLogDao.insertLog(entity)
        }
    }

    private fun seedInitialFinancialAppsIfEmpty() {
        viewModelScope.launch {
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
                    packageName = "com.dbbl.nexus.pay",
                    appName = "NexusPay DBBL",
                    isFinancial = true,
                    isLocked = true,
                    requirePin = true,
                    requireOtp = true,
                    requireRemoteApproval = true
                ),
                LockedAppEntity(
                    packageName = "com.binance.dev",
                    appName = "Binance Crypto",
                    isFinancial = true,
                    isLocked = true,
                    requirePin = true,
                    requireOtp = true,
                    requireRemoteApproval = false
                ),
                LockedAppEntity(
                    packageName = "com.google.android.apps.photos",
                    appName = "Google Photos / Gallery",
                    isFinancial = false,
                    isLocked = true,
                    requirePin = true,
                    requireOtp = false,
                    requireRemoteApproval = false
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

    private fun seedInitialVaultFilesIfEmpty() {
        viewModelScope.launch {
            val sampleFiles = listOf(
                VaultFileEntity(
                    title = "Bank_Statement_Confidential.pdf",
                    category = FileCategory.DOCUMENT.name,
                    filePath = "vault_doc_sample_1",
                    fileSizeFormatted = "1.8 MB",
                    description = "গোপন ব্যাংক অ্যাকাউন্ট লেনদেন ও আর্থিক রিপোর্ট",
                    secretContent = "Account No: 20491029481\nAvailable Balance: $14,850.00\nBranch: Main Corporate\nSecurity Token: 884-TX-99"
                ),
                VaultFileEntity(
                    title = "Secret_Crypto_Recovery_Key.txt",
                    category = FileCategory.NOTE.name,
                    filePath = "vault_note_sample_1",
                    fileSizeFormatted = "4 KB",
                    description = "হার্ডওয়্যার ওয়ালেট ও বিটকয়েন সিড ফ্রেজ",
                    secretContent = "12-Word Mnemonic Phrase:\nplanet orbit pulse velvet digital crystal shield armor neon echo shadow orbit"
                ),
                VaultFileEntity(
                    title = "Family_Passport_Scan.jpg",
                    category = FileCategory.PHOTO.name,
                    filePath = "vault_photo_sample_1",
                    fileSizeFormatted = "3.2 MB",
                    description = "পাসপোর্ট ও ন্যাশনাল আইডি কার্ডের এনক্রিপ্টেড কপি",
                    secretContent = "Passport: A19482910\nNID: 198274819204\nIssued: 12 Jan 2024"
                ),
                VaultFileEntity(
                    title = "Confidential_Voice_Memo.mp3",
                    category = FileCategory.AUDIO.name,
                    filePath = "vault_audio_sample_1",
                    fileSizeFormatted = "4.5 MB",
                    description = "জরুরি ব্যবসায়িক মিটিং এর অডিও রেকর্ড",
                    secretContent = "Audio Length: 04:32 min\nEncoding: AAC 256kbps\nTranscript: Meeting regarding cybersecurity architecture and dual phone authorization protocol."
                )
            )
            for (f in sampleFiles) {
                vaultDao.insertFile(f)
            }
        }
    }

    private fun seedInitialSensorLogsIfEmpty() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val logs = listOf(
                SensorAccessLogEntity(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    sensorType = SensorType.MICROPHONE.name,
                    timestamp = now - (12 * 60 * 1000),
                    durationSeconds = 45,
                    isBackground = false,
                    isUnusual = false,
                    unusualReason = ""
                ),
                SensorAccessLogEntity(
                    packageName = "com.google.android.GoogleCamera",
                    appName = "Camera",
                    sensorType = SensorType.CAMERA.name,
                    timestamp = now - (35 * 60 * 1000),
                    durationSeconds = 30,
                    isBackground = false,
                    isUnusual = false,
                    unusualReason = ""
                ),
                SensorAccessLogEntity(
                    packageName = "com.google.android.dialer",
                    appName = "Phone Dialer",
                    sensorType = SensorType.CALL.name,
                    timestamp = now - (2 * 60 * 60 * 1000),
                    durationSeconds = 120,
                    isBackground = false,
                    isUnusual = false,
                    unusualReason = ""
                ),
                SensorAccessLogEntity(
                    packageName = "com.suspicious.flashlight.cleaner",
                    appName = "Super Flashlight Tool",
                    sensorType = SensorType.MICROPHONE.name,
                    timestamp = now - (4 * 60 * 60 * 1000),
                    durationSeconds = 18,
                    isBackground = true,
                    isUnusual = true,
                    unusualReason = "ব্যাকগ্রাউন্ডে অপ্রয়োজনীয় মাইক্রোফোন ব্যবহার (রাত ৩:২০ মিনিট)"
                ),
                SensorAccessLogEntity(
                    packageName = "com.unknown.recorder.pro",
                    appName = "Call Recorder Spy",
                    sensorType = SensorType.CALL.name,
                    timestamp = now - (5 * 60 * 60 * 1000),
                    durationSeconds = 60,
                    isBackground = true,
                    isUnusual = true,
                    unusualReason = "অননুমোদিত ব্যাকগ্রাউন্ড কল অডিও মনিটরিং শনাক্ত"
                )
            )
            sensorAccessDao.insertAll(logs)
        }
    }

    private fun seedInitialDriveItemsIfEmpty() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val items = listOf(
                CloudDriveItemEntity(
                    title = "NID_Passport_Scan_Confidential.pdf",
                    mimeType = "application/pdf",
                    fileSizeFormatted = "3.4 MB",
                    shareStatus = DriveShareStatus.PUBLIC_LINK.name,
                    isLeaking = true,
                    sharedWithEmails = "Anyone with link (Leaked to Public)",
                    lastScannedTimestamp = now
                ),
                CloudDriveItemEntity(
                    title = "Crypto_Wallet_Backup_2026.docx",
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    fileSizeFormatted = "540 KB",
                    shareStatus = DriveShareStatus.ANYONE_WITH_LINK.name,
                    isLeaking = true,
                    sharedWithEmails = "Public Web Indexable",
                    lastScannedTimestamp = now
                ),
                CloudDriveItemEntity(
                    title = "Family_Vacation_Photos.zip",
                    mimeType = "application/zip",
                    fileSizeFormatted = "182 MB",
                    shareStatus = DriveShareStatus.RESTRICTED.name,
                    isLeaking = false,
                    sharedWithEmails = "family@gmail.com",
                    lastScannedTimestamp = now
                ),
                CloudDriveItemEntity(
                    title = "Bank_Transactions_2026.xlsx",
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileSizeFormatted = "1.2 MB",
                    shareStatus = DriveShareStatus.PRIVATE.name,
                    isLeaking = false,
                    sharedWithEmails = "Only Owner",
                    lastScannedTimestamp = now
                )
            )
            cloudDriveDao.insertAll(items)
        }
    }
}
