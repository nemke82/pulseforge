package com.pulseforge.wear.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build

class WearReminderManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pulseforge_wear_reminders", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun getIntervalHours(): Int {
        return prefs.getInt("reminder_interval_hours", 0) // 0 = off
    }

    fun setIntervalHours(hours: Int) {
        prefs.edit().putInt("reminder_interval_hours", hours).apply()
        if (hours > 0) {
            scheduleNextReminder(hours)
        } else {
            cancelReminders()
        }
    }

    fun scheduleNextReminder(hours: Int = getIntervalHours()) {
        if (hours <= 0) return

        val intent = Intent(context, WearReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = System.currentTimeMillis() + (hours * 3600 * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    }

    fun cancelReminders() {
        val intent = Intent(context, WearReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val REMINDER_REQUEST_CODE = 8201
    }
}
