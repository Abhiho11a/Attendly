package com.example.attendly.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.attendly.MainActivity
import com.example.attendly.data.repository.UserPreferencesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AttendanceReminderScheduler {
    private const val TAG = "ATTENDLY_PROD_REMINDER"
    const val ALARM_ACTION = "com.example.attendly.ACTION_ATTENDANCE_REMINDER"
    const val TEST_ALARM_ACTION = "com.example.attendly.ACTION_TEST_REMINDER"
    const val REQUEST_CODE = 2001
    const val TEST_REQUEST_CODE = 2002
    const val CHANNEL_ID = "attendance_reminders_channel"

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Attendance Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to update daily attendance status"
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "[NOTIFICATION] CHANNEL_ENABLED=true channelId=$CHANNEL_ID")
    }

    @SuppressLint("MissingPermission")
    fun sendDirectTestNotification(context: Context) {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Direct Test Notification")
            .setContentText("Direct notification pipeline is working perfectly! 🎉")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(9999, notification)
            Log.d(TAG, "[NOTIFICATION] DIRECT_NOTIFICATION_START posted=true notificationId=9999")
        } catch (e: Exception) {
            Log.e(TAG, "[NOTIFICATION] DIRECT_NOTIFICATION_ERROR posted=false", e)
        }
    }

    fun scheduleTestNotification(context: Context, delaySeconds: Long = 30L) {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(appContext, AttendanceReminderReceiver::class.java).apply {
            action = TEST_ALARM_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val zoneId = ZoneId.systemDefault()
        val triggerTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(triggerAtMillis), zoneId)

        Log.d(TAG, "[SCHEDULER] TEST_ALARM_SCHEDULED delaySeconds=$delaySeconds triggerAtMillis=$triggerAtMillis targetTime=$triggerTime")

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelTestNotification(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, AttendanceReminderReceiver::class.java).apply {
            action = TEST_ALARM_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "[SCHEDULER] TEST_ALARM_CANCELLED")
    }

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        createNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prefs = UserPreferencesRepository(appContext)

        val intent = Intent(appContext, AttendanceReminderReceiver::class.java).apply {
            action = ALARM_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!prefs.isRemindersEnabled()) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "[PROD] ENABLED=false -> ALARM_CANCELLED=true")
            return
        }

        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)

        val hour = prefs.getReminderHour()
        val minute = prefs.getReminderMinute()

        var targetDateTime = today.atTime(hour, minute)

        // If selected time has already passed today, schedule for tomorrow
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val triggerAtMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()

        Log.d(TAG, "[PROD] ENABLED=true SELECTED_TIME=${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)} CURRENT_LOCAL=$now TARGET_LOCAL=$targetDateTime TRIGGER_MILLIS=$triggerAtMillis TIMEZONE=$zoneId ALARM_SCHEDULED=true")

        scheduleExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "[PROD] ALARM_SCHEDULED=true (setAlarmClock)")
        } catch (e: Exception) {
            Log.w(TAG, "[PROD] setAlarmClock failed, falling back to allowWhileIdle", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, AttendanceReminderReceiver::class.java).apply {
            action = ALARM_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "[PROD] ALARM_CANCELLED=true REASON=user_disabled")
    }

    fun getNextScheduledTimeFormatted(context: Context): String {
        val prefs = UserPreferencesRepository(context)
        if (!prefs.isRemindersEnabled()) return "Disabled"

        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)

        val hour = prefs.getReminderHour()
        val minute = prefs.getReminderMinute()

        var targetDateTime = today.atTime(hour, minute)
        if (targetDateTime.isBefore(now) || targetDateTime.isEqual(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
        return targetDateTime.format(formatter)
    }
}
