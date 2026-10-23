package com.example.attendly.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Onboarding : Screen("onboarding", "Welcome")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Attendance : Screen("attendance", "Attendance", Icons.Default.InsertChart)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.CalendarToday)
    object Settings : Screen("settings", "Settings")
    object ImportTimetable : Screen("import_timetable", "Import Timetable")
    object SubjectDetails : Screen("subject_details/{subjectId}", "Details") {
        fun createRoute(subjectId: Long) = "subject_details/$subjectId"
    }
}
