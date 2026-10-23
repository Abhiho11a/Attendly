package com.example.attendly.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.attendly.MainActivity
import com.example.attendly.data.database.AttendlyDatabase
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class AttendanceReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val appContext = context.applicationContext
        val action = intent.action

        Log.d(TAG, "[RECEIVER] RECEIVED_ACTION=$action RECEIVED_AT=${System.currentTimeMillis()}")

        // 1. System events (BOOT, TIME_SET, TIMEZONE_CHANGED, DATE_CHANGED) -> reschedule only
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            Log.d(TAG, "[RECEIVER] SYSTEM_EVENT=$action -> RESCHEDULING_ALARM")
            AttendanceReminderScheduler.schedule(appContext)
            return
        }

        // 2. Test Alarm Trigger
        if (action == AttendanceReminderScheduler.TEST_ALARM_ACTION) {
            Log.d(TAG, "[RECEIVER] TEST_RECEIVER_RECEIVED action=$action")
            showTestNotification(appContext)
            return
        }

        // 3. Production Attendance Reminder Trigger
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                processProductionReminder(appContext, isManualTest = false)
            } catch (e: Exception) {
                Log.e(TAG, "[PROD] ERROR in production receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ATTENDLY_PROD_REMINDER"

        suspend fun processProductionReminder(appContext: Context, isManualTest: Boolean = false) {
            Log.d(TAG, "[PROD] PRODUCTION_REMINDER_RECEIVED=true IS_MANUAL_TEST=$isManualTest")

            val prefs = UserPreferencesRepository(appContext)

            if (!isManualTest && !prefs.isRemindersEnabled()) {
                Log.d(TAG, "[PROD] REMINDER_ENABLED=false -> SKIPPED")
                return
            }

            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val todayStr = today.toString()
            val hour = prefs.getReminderHour()
            val minute = prefs.getReminderMinute()

            Log.d(TAG, "[PROD] TODAY=$todayStr CONFIGURED_TIME=${String.format("%02d:%02d", hour, minute)}")

            // Prevent duplicate notifications on the same date (unless manual test)
            if (!isManualTest && prefs.getLastReminderSentDate() == todayStr) {
                Log.d(TAG, "[PROD] NOTIFICATION_ALREADY_PROCESSED_TODAY=$todayStr")
                AttendanceReminderScheduler.schedule(appContext)
                return
            }

            val database = AttendlyDatabase.getDatabase(appContext)
            val dao = database.attendlyDao()
            val dayOfWeek = today.dayOfWeek

            val timetableClasses = dao.getTimetableClassesForDay(dayOfWeek).first()
            Log.d(TAG, "[PROD] CLASSES_FOUND=${timetableClasses.size}")

            if (timetableClasses.isEmpty()) {
                Log.d(TAG, "[PROD] CLASSES_FOUND=0 -> NOTIFICATION_REQUIRED=false")
                if (!isManualTest) {
                    prefs.setLastReminderSentDate(todayStr)
                    AttendanceReminderScheduler.schedule(appContext)
                }
                return
            }

            val attendanceRecords = dao.getAttendanceForDate(today).first()
            Log.d(TAG, "[PROD] ATTENDANCE_RECORDS_FOUND=${attendanceRecords.size}")

            val attendanceMap = attendanceRecords.associateBy { it.timetableClassId }

            var incompleteCount = 0
            timetableClasses.forEach { tClass ->
                val record = attendanceMap[tClass.id]
                val isComplete = record != null && record.status != AttendanceStatus.PENDING
                if (!isComplete) incompleteCount++
                Log.d(TAG, "[PROD] CLASS=${tClass.id} STATUS=${record?.status ?: "MISSING"} COMPLETE=$isComplete")
            }

            Log.d(TAG, "[PROD] INCOMPLETE_CLASS_COUNT=$incompleteCount")

            val notificationRequired = incompleteCount > 0
            Log.d(TAG, "[PROD] NOTIFICATION_REQUIRED=$notificationRequired")

            if (notificationRequired) {
                showReminderNotification(appContext)
                Log.d(TAG, "[PROD] NOTIFICATION_POSTED=true")
            } else {
                Log.d(TAG, "[PROD] NOTIFICATION_POSTED=false REASON=ALL_ATTENDANCE_COMPLETE")
            }

            if (!isManualTest) {
                prefs.setLastReminderSentDate(todayStr)
                AttendanceReminderScheduler.schedule(appContext)
                Log.d(TAG, "[PROD] NEXT_REMINDER=${AttendanceReminderScheduler.getNextScheduledTimeFormatted(appContext)}")
            }
        }

        @SuppressLint("MissingPermission")
        fun showTestNotification(context: Context) {
            AttendanceReminderScheduler.createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9998,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, AttendanceReminderScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Scheduled Test Notification")
                .setContentText("Scheduled AlarmManager + Receiver pipeline works 100%! 🎉")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(9998, notification)
                Log.d(TAG, "[NOTIFICATION] TEST_NOTIFICATION_POSTED=true notificationId=9998")
            } catch (e: Exception) {
                Log.e(TAG, "[NOTIFICATION] TEST_NOTIFICATION_ERROR posted=false", e)
            }
        }

        @SuppressLint("MissingPermission")
        fun showReminderNotification(context: Context) {
            AttendanceReminderScheduler.createNotificationChannel(context)

            val notifManager = NotificationManagerCompat.from(context)
            if (!notifManager.areNotificationsEnabled()) {
                Log.w(TAG, "[NOTIFICATION] NOTIFICATION_PERMISSION_ENABLED=false -> cannot post notification")
                return
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, AttendanceReminderScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Attendance Reminder")
                .setContentText("Don't forget to update today's attendance.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                notifManager.notify(1001, notification)
                Log.d(TAG, "[NOTIFICATION] PRODUCTION_NOTIFICATION_POSTED=true notificationId=1001")
            } catch (e: Exception) {
                Log.e(TAG, "[NOTIFICATION] PRODUCTION_NOTIFICATION_ERROR posted=false", e)
            }
        }
    }
}
