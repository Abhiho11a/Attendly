package com.example.attendly.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendly.notifications.AttendanceReminderScheduler

class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetUpdateWorker.schedule(context)
                WidgetUpdateWorker.updateNow(context)
                AttendanceReminderScheduler.schedule(context)
            }
        }
    }
}
