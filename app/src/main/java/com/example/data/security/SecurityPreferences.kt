package com.example.data.security

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("secureshield_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_IS_SETUP_DONE = "is_setup_done"
        private const val KEY_LANGUAGE = "app_language" // "bn" or "en"
        private const val KEY_DISGUISE_CALCULATOR = "disguise_calculator"
        private const val KEY_SECONDARY_PHONE_NAME = "secondary_phone_name"
        private const val KEY_SECONDARY_PHONE_CODE = "secondary_phone_code"
        private const val KEY_SECONDARY_PAIRED = "secondary_paired"
        private const val KEY_INTRUDER_CAPTURE = "intruder_capture"
        private const val KEY_BIOMETRIC_SIMULATION = "biometric_simulation"
        private const val KEY_ANTI_POWER_OFF = "anti_power_off"
        private const val KEY_BOOT_LOCKDOWN = "boot_lockdown"
        private const val KEY_SIM_EJECT_PROTECTION = "sim_eject_protection"
        private const val KEY_LIVE_TRACKING = "live_tracking"
        private const val KEY_GOOGLE_DRIVE_DLP = "google_drive_dlp"
        private const val KEY_DEFAULT_PIN = "1234"
    }

    init {
        // If first run, set default PIN to 1234
        if (!prefs.contains(KEY_PIN_HASH)) {
            setMasterPin(KEY_DEFAULT_PIN)
        }
    }

    fun isSetupDone(): Boolean = prefs.getBoolean(KEY_IS_SETUP_DONE, true)
    fun setSetupDone(done: Boolean) = prefs.edit().putBoolean(KEY_IS_SETUP_DONE, done).apply()

    fun getMasterPinHash(): String = prefs.getString(KEY_PIN_HASH, "") ?: ""
    fun setMasterPin(pin: String) {
        val hash = EncryptionHelper.hashSecret(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        return EncryptionHelper.verifySecret(pin, getMasterPinHash())
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "bn") ?: "bn"
    fun setLanguage(lang: String) = prefs.edit().putString(KEY_LANGUAGE, lang).apply()

    fun isDisguiseCalculatorEnabled(): Boolean =
        prefs.getBoolean(KEY_DISGUISE_CALCULATOR, false)
    fun setDisguiseCalculatorEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_DISGUISE_CALCULATOR, enabled).apply()

    fun getSecondaryPhoneName(): String =
        prefs.getString(KEY_SECONDARY_PHONE_NAME, "Samsung Galaxy S24 Ultra") ?: "Samsung Galaxy S24 Ultra"
    fun setSecondaryPhoneName(name: String) =
        prefs.edit().putString(KEY_SECONDARY_PHONE_NAME, name).apply()

    fun getSecondaryPhoneCode(): String {
        var code = prefs.getString(KEY_SECONDARY_PHONE_CODE, "")
        if (code.isNullOrEmpty()) {
            code = "SEC-8821"
            prefs.edit().putString(KEY_SECONDARY_PHONE_CODE, code).apply()
        }
        return code
    }
    fun setSecondaryPhoneCode(code: String) =
        prefs.edit().putString(KEY_SECONDARY_PHONE_CODE, code).apply()

    fun isSecondaryPaired(): Boolean = prefs.getBoolean(KEY_SECONDARY_PAIRED, true)
    fun setSecondaryPaired(paired: Boolean) =
        prefs.edit().putBoolean(KEY_SECONDARY_PAIRED, paired).apply()

    fun isIntruderCaptureEnabled(): Boolean =
        prefs.getBoolean(KEY_INTRUDER_CAPTURE, true)
    fun setIntruderCaptureEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_INTRUDER_CAPTURE, enabled).apply()

    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_SIMULATION, true)
    fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BIOMETRIC_SIMULATION, enabled).apply()

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
        prefs.getBoolean(KEY_LIVE_TRACKING, true)
    fun setLiveTrackingEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_LIVE_TRACKING, enabled).apply()

    fun isGoogleDriveDlpEnabled(): Boolean =
        prefs.getBoolean(KEY_GOOGLE_DRIVE_DLP, true)
    fun setGoogleDriveDlpEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_GOOGLE_DRIVE_DLP, enabled).apply()
}
