package com.example.attendly.ui.screens.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendly.AttendlyApplication
import com.example.attendly.data.entity.AttendanceRecordEntity
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.ui.viewmodel.SubjectDetailsViewModel
import com.example.attendly.ui.viewmodel.ViewModelFactory
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import com.example.attendly.ui.theme.StatusAttended
import com.example.attendly.ui.theme.StatusAbsent
import com.example.attendly.ui.theme.StatusCancelled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailsScreen(
    subjectId: Long,
    onBack: () -> Unit,
    viewModel: SubjectDetailsViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(subjectId) {
        viewModel.loadSubjectDetails(subjectId)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = uiState.subject?.name ?: "Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                item {
                    SubjectStatsCard(uiState)
                }
                item {
                    BunkSummaryCard(uiState)
                }
                item {
                    Text(
                        text = "Attendance History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (uiState.history.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = "No records found for this subject.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    items(uiState.history, key = { it.id }) { record ->
                        HistoryItem(record)
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectStatsCard(uiState: com.example.attendly.ui.viewmodel.SubjectDetailsUiState) {
    val percentageText = uiState.attendancePercentage?.let { String.format("%.1f%%", it) } ?: "—"
    val target = uiState.subject?.targetAttendance ?: 75
    val statusColor = when {
        uiState.attendancePercentage == null -> Color.Gray
        uiState.attendancePercentage >= target -> StatusAttended
        uiState.attendancePercentage >= (target - 5) -> Color(0xFFF59E0B)
        else -> StatusAbsent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentageText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "OVERALL ATTENDANCE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatDetail("Total", uiState.conductedCount.toString())
                StatDetail("Attended", uiState.attendedCount.toString())
                StatDetail("Absent", uiState.absentCount.toString())
                StatDetail("Target", "$target%")
            }
        }
    }
}

@Composable
fun BunkSummaryCard(uiState: com.example.attendly.ui.viewmodel.SubjectDetailsUiState) {
    val target = uiState.subject?.targetAttendance ?: 75
    val conducted = uiState.conductedCount
    val attended = uiState.attendedCount
    val percentage = uiState.attendancePercentage

    if (conducted == 0 || percentage == null) return

    val (message, color) = if (percentage >= target) {
        val safe = floor((100.0 * attended / target) - conducted).toInt().coerceAtLeast(0)
        if (safe > 0) {
            "You can skip the next $safe classes safely." to StatusAttended
        } else {
            "You're on the edge! Don't miss the next class." to Color(0xFFF59E0B)
        }
    } else {
        val required = if (target < 100) {
            ceil((target.toDouble() * conducted - 100.0 * attended) / (100.0 - target)).toInt().coerceAtLeast(1)
        } else 999
        "Attend the next $required classes to reach your $target% goal." to StatusAbsent
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun StatDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HistoryItem(record: com.example.attendly.data.entity.AttendanceRecordEntity) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
    val statusColor = when (record.status) {
        AttendanceStatus.ATTENDED -> StatusAttended
        AttendanceStatus.ABSENT -> StatusAbsent
        AttendanceStatus.CANCELLED -> StatusCancelled
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = record.date.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier.size(44.dp).background(statusColor.copy(alpha = 0.15f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                if (record.status == AttendanceStatus.ATTENDED) {
                    Text("✓", color = statusColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                } else if (record.status == AttendanceStatus.ABSENT) {
                    Text("✕", color = statusColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                } else if (record.status == AttendanceStatus.CANCELLED) {
                    Text("—", color = statusColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
