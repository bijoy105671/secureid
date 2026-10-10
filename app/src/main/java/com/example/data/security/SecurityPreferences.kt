package com.example.data.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Hardened Security Preferences for SHIELD X.
 *
 * Implements:
 * - NO hardcoded default credentials (PIN 1234 and SEC-8821 completely eliminated).
 * - First-run credential setup enforcement (isSetupDone defaults to false).
 * - Persistent failed-attempt throttling with exponential backoff delay.
 * - Reauthentication requirement before changing credentials.
 * - Trusted device pairing management with public key storage and revocation.
 */
class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shieldx_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_IS_SETUP_DONE = "is_setup_done"
        private const val KEY_LANGUAGE = "app_language" // "bn" or "en"
        private const val KEY_DISGUISE_CALCULATOR = "disguise_calculator"
        private const val KEY_SECONDARY_PHONE_NAME = "secondary_phone_name"
        private const val KEY_SECONDARY_DEVICE_ID = "secondary_device_id"
        private const val KEY_SECONDARY_PUBLIC_KEY = "secondary_public_key"
        private const val KEY_SECONDARY_PAIRED = "secondary_paired"
        private const val KEY_INTRUDER_CAPTURE = "intruder_capture"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_ANTI_POWER_OFF = "anti_power_off"
        private const val KEY_BOOT_LOCKDOWN = "boot_lockdown"
        private const val KEY_SIM_EJECT_PROTECTION = "sim_eject_protection"
        private const val KEY_LIVE_TRACKING = "live_tracking"
        private const val KEY_GOOGLE_DRIVE_DLP = "google_drive_dlp"
        private const val KEY_DEMO_MODE = "demo_mode_enabled"

        // Throttling keys
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts_count"
        private const val KEY_LOCKOUT_UNTIL_TIMESTAMP = "lockout_until_timestamp"
    }

    /**
     * Fresh installations must complete credential setup. Defaults to false.
     */
    fun isSetupDone(): Boolean = prefs.getBoolean(KEY_IS_SETUP_DONE, false)
    fun setSetupDone(done: Boolean) = prefs.edit().putBoolean(KEY_IS_SETUP_DONE, done).apply()

    fun getMasterPinHash(): String = prefs.getString(KEY_PIN_HASH, "") ?: ""

    /**
     * Sets master PIN/passphrase using PBKDF2 with unique salt.
     */
    fun setMasterPin(pin: String) {
        val hash = EncryptionHelper.hashSecret(pin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_IS_SETUP_DONE, true)
            .apply()
    }

    /**
     * Changes master passphrase only after verifying current credential.
     */
    fun changeMasterPin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) {
            return false
        }
        setMasterPin(newPin)
        return true
    }

    /**
     * Verifies entered PIN/passphrase with persistent throttling.
     */
    fun verifyPin(pin: String): Boolean {
        if (getRemainingLockoutSeconds() > 0) {
            return false
        }

        val storedHash = getMasterPinHash()
        if (storedHash.isEmpty()) {
            return false
        }

        val isMatch = EncryptionHelper.verifySecret(pin, storedHash)
        if (isMatch) {
            // Reset throttle counters upon successful authentication
            resetThrottling()
            return true
        } else {
            recordFailedAttempt()
            return false
        }
    }

    // ------------------------------------------------------------------------
    // Persistent Throttling & Lockout Delays
    // ------------------------------------------------------------------------

    fun getFailedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun getRemainingLockoutSeconds(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            (lockoutUntil - now) / 1000L
        } else {
            0L
        }
    }

    private fun recordFailedAttempt() {
        val currentFailed = getFailedAttempts() + 1
        val now = System.currentTimeMillis()

        val lockoutDelaySeconds: Long = when {
            currentFailed >= 7 -> 600L // 10 minutes
            currentFailed >= 5 -> 300L // 5 minutes
            currentFailed >= 3 -> 30L  // 30 seconds
            else -> 0L
        }

        val lockoutUntil = if (lockoutDelaySeconds > 0) now + (lockoutDelaySeconds * 1000L) else 0L

        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentFailed)
            .putLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, lockoutUntil)
            .apply()
    }

    fun resetThrottling() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_TIMESTAMP, 0L)
            .apply()
    }

    // ------------------------------------------------------------------------
    // Trusted Device Pairing
    // ------------------------------------------------------------------------

    fun isSecondaryPaired(): Boolean = prefs.getBoolean(KEY_SECONDARY_PAIRED, false)

    fun getSecondaryPhoneName(): String =
        prefs.getString(KEY_SECONDARY_PHONE_NAME, "অসংযুক্ত (No Paired Device)") ?: "অসংযুক্ত"

    fun getSecondaryDeviceId(): String = prefs.getString(KEY_SECONDARY_DEVICE_ID, "") ?: ""

    fun getSecondaryPublicKey(): String = prefs.getString(KEY_SECONDARY_PUBLIC_KEY, "") ?: ""

    fun savePairedDevice(name: String, deviceId: String, publicKeyBase64: String) {
        prefs.edit()
            .putString(KEY_SECONDARY_PHONE_NAME, name)
            .putString(KEY_SECONDARY_DEVICE_ID, deviceId)
            .putString(KEY_SECONDARY_PUBLIC_KEY, publicKeyBase64)
            .putBoolean(KEY_SECONDARY_PAIRED, true)
            .apply()
    }

    fun revokePairing() {
        prefs.edit()
            .remove(KEY_SECONDARY_PHONE_NAME)
            .remove(KEY_SECONDARY_DEVICE_ID)
            .remove(KEY_SECONDARY_PUBLIC_KEY)
            .putBoolean(KEY_SECONDARY_PAIRED, false)
            .apply()
    }

    // ------------------------------------------------------------------------
    // General Preferences
    // ------------------------------------------------------------------------

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
    fun setLanguage(lang: String) = prefs.edit().putString(KEY_LANGUAGE, lang).apply()

    fun isDisguiseCalculatorEnabled(): Boolean =
        prefs.getBoolean(KEY_DISGUISE_CALCULATOR, false)
    fun setDisguiseCalculatorEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_DISGUISE_CALCULATOR, enabled).apply()

    fun isIntruderCaptureEnabled(): Boolean =
        prefs.getBoolean(KEY_INTRUDER_CAPTURE, false)
    fun setIntruderCaptureEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_INTRUDER_CAPTURE, enabled).apply()

    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()

    fun isAntiPowerOffEnabled(): Boolean =
        prefs.getBoolean(KEY_ANTI_POWER_OFF, true)
    fun setAntiPowerOffEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_ANTI_POWER_OFF, enabled).apply()

    fun isBootLockdownActive(): Boolean =
        prefs.getBoolean(KEY_BOOT_LOCKDOWN, false)
    fun setBootLockdownActive(active: Boolean) =
        prefs.edit().putBoolean(KEY_BOOT_LOCKDOWN, active).apply()

    fun isSimEjectProtectionEnabled(): Boolean =
        prefs.getBoolean(KEY_SIM_EJECT_PROTECTION, true)
    fun setSimEjectProtectionEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_SIM_EJECT_PROTECTION, enabled).apply()

    fun isLiveTrackingEnabled(): Boolean =
        prefs.getBoolean(KEY_LIVE_TRACKING, false)
    fun setLiveTrackingEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_LIVE_TRACKING, enabled).apply()

    fun isGoogleDriveDlpEnabled(): Boolean =
        prefs.getBoolean(KEY_GOOGLE_DRIVE_DLP, false)
    fun setGoogleDriveDlpEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_DLP, enabled).apply()

    fun isDemoModeEnabled(): Boolean =
        prefs.getBoolean(KEY_DEMO_MODE, false)
    fun setDemoModeEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_DEMO_MODE, enabled).apply()
}
