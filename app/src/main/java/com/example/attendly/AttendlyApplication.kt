package com.example.attendly

import android.app.Application
import com.example.attendly.data.database.AttendlyDatabase
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.data.repository.UserPreferencesRepository
import com.example.attendly.notifications.AttendanceReminderScheduler
import com.example.attendly.widget.WidgetUpdateWorker

class AttendlyApplication : Application() {
    val database by lazy { AttendlyDatabase.getDatabase(this) }
    val repository by lazy { AttendlyRepository(database.attendlyDao(), this) }
    val preferences by lazy { UserPreferencesRepository(this) }

    override fun onCreate() {
        super.onCreate()
        WidgetUpdateWorker.schedule(this)
        WidgetUpdateWorker.updateNow(this)
        AttendanceReminderScheduler.schedule(this)
    }
}
