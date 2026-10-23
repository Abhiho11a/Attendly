package com.example.attendly.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AttendlyWidget().updateAll(appContext)
            // Re-schedule midnight update for the next day transition
            scheduleMidnightUpdate(appContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "AttendlyWidgetUpdatePeriodic"
        private const val MIDNIGHT_WORK_TAG = "AttendlyWidgetUpdateMidnight"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // 1. Battery-conscious periodic refresh (every 1 hour)
            val periodicRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            // 2. Schedule precise midnight update for day transition
            scheduleMidnightUpdate(context)
        }

        fun scheduleMidnightUpdate(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayDuration = Duration.between(now, nextMidnight)

            val midnightRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delayDuration.toMillis(), TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniqueWork(
                MIDNIGHT_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                midnightRequest
            )
        }

        fun updateNow(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val immediateRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            workManager.enqueue(immediateRequest)
        }
    }
}
