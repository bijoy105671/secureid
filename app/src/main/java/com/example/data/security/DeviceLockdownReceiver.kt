package com.example.data.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ui.screens.LockdownActivity

class DeviceLockdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = SecurityPreferences(context)

        when (action) {
            Intent.ACTION_SHUTDOWN,
            "android.intent.action.QUICKBOOT_POWEROFF",
            "com.htc.intent.action.QUICKBOOT_POWEROFF" -> {
                if (prefs.isAntiPowerOffEnabled()) {
                    DeviceLockdownService.triggerLockdown(
                        context,
                        reason = "Static System Shutdown Intercepted ($action)"
                    )
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Ensure lockdown service starts upon boot
                if (prefs.isAntiPowerOffEnabled()) {
                    DeviceLockdownService.start(context)
                }

                // If previous power-off was forced or unverified, immediately lock device post-boot
                if (prefs.isBootLockdownActive()) {
                    val lockdownIntent = Intent(context, LockdownActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("EXTRA_REASON", "Post-Boot Security Lockdown (OTP Required)")
                    }
                    context.startActivity(lockdownIntent)
                }
            }
        }
    }
}
