package com.pulseforge.wear.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.pulseforge.wear.presentation.MainActivity

class WearReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pulseforge_bp_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BP Check Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic reminders to take blood pressure readings"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_START_MEASURE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PulseForge Check")
            .setContentText("Time for your Blood Pressure reading! Tap to measure.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Vibrate watch
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                vibrator.vibrate(500)
            }
        } catch (_: Exception) {}

        // Schedule next reminder
        val reminderManager = WearReminderManager(context)
        reminderManager.scheduleNextReminder()
    }

    companion object {
        const val NOTIFICATION_ID = 8202
    }
}
