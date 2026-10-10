package com.example.data.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "secureshield_remote_approval"
    private const val CHANNEL_NAME = "রিমোট ডিভাইস অনুমোদন ও নিরাপত্তা"
    private const val EMERGENCY_CHANNEL_ID = "secureshield_antitheft_emergency"
    private const val EMERGENCY_CHANNEL_NAME = "এন্টি-থিফ ও সিম ইমার্জেন্সি অ্যালার্ট"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "অন্য ফোন থেকে অ্যাপ খোলার অনুমোদন ও রিমোট পুশ সতর্কতা"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)

            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                EMERGENCY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "সিম খোলা বা জোরপূর্বক পাওয়ার অফ চেষ্টার হাই-প্রায়োরিটি সাইরেন অ্যালার্ট"
                enableVibration(true)
            }
            manager.createNotificationChannel(emergencyChannel)
        }
    }

    fun showRemoteApprovalNotification(
        context: Context,
        appName: String,
        otp: String,
        requestId: String,
        isBn: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_REQUEST_ID", requestId)
            putExtra("OPEN_SCREEN", "remote")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBn) "🚨 রিমোট অনুমোদন অনুরোধ: $appName" else "🚨 Remote Approval Request: $appName"
        val message = if (isBn) {
            "ডিভাইস থেকে $appName এ প্রবেশের চেষ্টা হচ্ছে! ওটিপি: $otp। অনুমোদন করবেন কি?"
        } else {
            "Access attempt to $appName from paired device! OTP: $otp. Approve or reject?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        try {
            manager.notify(requestId.hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun showAntiTheftEmergencyNotification(
        context: Context,
        title: String,
        message: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "findhub")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9991,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .build()

        try {
            manager.notify(9991, notification)
        } catch (_: SecurityException) {
        }
    }
}
