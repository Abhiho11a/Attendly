package com.example.attendly.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import com.example.attendly.MainActivity
import com.example.attendly.AttendlyApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class AttendlyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as AttendlyApplication
        val repository = app.repository
        
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        val currentTime = LocalTime.now()

        val timetableClasses = try {
            repository.getTimetableClassesForDay(dayOfWeek).first().sortedBy { it.startTime }
        } catch (_: Exception) {
            emptyList()
        }
        val subjects = try {
            repository.allSubjects.first().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        val dayName = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
        val dateStr = "${today.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())} ${today.dayOfMonth}"
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF0F172A), Color(0xFF0F172A))) // Deep slate background
                        .cornerRadius(24.dp)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    // Header Row: App Title & Day, Date
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "Attendly",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF818CF8), Color(0xFF818CF8)), // Primary Indigo
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "TODAY • $dayName, $dateStr",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF94A3B8), Color(0xFF94A3B8)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    if (timetableClasses.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                            ) {
                                Text(
                                    text = "No classes scheduled today 🎉",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFFF8FAFC), Color(0xFFF8FAFC)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Enjoy your day!",
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF94A3B8), Color(0xFF94A3B8)),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    } else {
                        // Chronological list of TODAY'S CLASSES
                        LazyColumn(
                            modifier = GlanceModifier.fillMaxSize()
                        ) {
                            itemsIndexed(timetableClasses) { index, tClass ->
                                val subj = subjects[tClass.subjectId]
                                val isOngoing = !currentTime.isBefore(tClass.startTime) && !currentTime.isAfter(tClass.endTime)
                                val timeRangeStr = "${tClass.startTime.format(timeFormatter)} - ${tClass.endTime.format(timeFormatter)}"

                                Column(
                                    modifier = GlanceModifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .background(
                                                ColorProvider(
                                                    if (isOngoing) Color(0xFF1E1B4B) else Color(0xFF1E293B),
                                                    if (isOngoing) Color(0xFF1E1B4B) else Color(0xFF1E293B)
                                                )
                                            )
                                            .cornerRadius(14.dp)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Vertical.CenterVertically
                                    ) {
                                        Column(
                                            modifier = GlanceModifier.defaultWeight()
                                        ) {
                                            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                                                Text(
                                                    text = subj?.name ?: "Class",
                                                    style = TextStyle(
                                                        color = ColorProvider(Color(0xFFF8FAFC), Color(0xFFF8FAFC)),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                if (isOngoing) {
                                                    Spacer(modifier = GlanceModifier.width(6.dp))
                                                    Box(
                                                        modifier = GlanceModifier
                                                            .background(ColorProvider(Color(0xFF34D399), Color(0xFF34D399)))
                                                            .cornerRadius(4.dp)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "LIVE",
                                                            style = TextStyle(
                                                                color = ColorProvider(Color(0xFF0F172A), Color(0xFF0F172A)),
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = GlanceModifier.height(2.dp))

                                            Text(
                                                text = timeRangeStr + if (!tClass.room.isNullOrBlank()) " • Room ${tClass.room}" else "",
                                                style = TextStyle(
                                                    color = ColorProvider(
                                                        if (isOngoing) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                                                        if (isOngoing) Color(0xFFA5B4FC) else Color(0xFF94A3B8)
                                                    ),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }

                                    if (index < timetableClasses.lastIndex) {
                                        Spacer(
                                            modifier = GlanceModifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AttendlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendlyWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateWorker.schedule(context)
        WidgetUpdateWorker.updateNow(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateWorker.schedule(context)
        WidgetUpdateWorker.updateNow(context)
    }
}
