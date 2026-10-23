package com.example.attendly.ui.screens.attendance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendly.AttendlyApplication
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.ui.components.TopNotificationHost
import com.example.attendly.ui.state.UiState
import com.example.attendly.ui.theme.StatusAbsent
import com.example.attendly.ui.theme.StatusAttended
import com.example.attendly.ui.theme.StatusCancelled
import com.example.attendly.ui.theme.StatusPending
import com.example.attendly.ui.viewmodel.AttendanceData
import com.example.attendly.ui.viewmodel.AttendanceViewModel
import com.example.attendly.ui.viewmodel.AttendanceViewMode
import com.example.attendly.ui.viewmodel.BunkStatus
import com.example.attendly.ui.viewmodel.DayClassStatusItem
import com.example.attendly.ui.viewmodel.DayWiseAttendanceItem
import com.example.attendly.ui.viewmodel.SubjectAttendanceItem
import com.example.attendly.ui.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateToSubjectDetails: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AttendanceViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository
        )
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var selectedDayForDialog by remember { mutableStateOf<DayWiseAttendanceItem?>(null) }
    var subjectToEditCounts by remember { mutableStateOf<SubjectAttendanceItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            notificationMessage = message
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TopNotificationHost(
            message = notificationMessage,
            onDismiss = { notificationMessage = null }
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "attendanceStateTransition"
            ) { targetState ->
                when (targetState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.5.dp
                            )
                        }
                    }
                    is UiState.Empty -> {
                        EmptyAttendanceState()
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = targetState.message,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is UiState.Success -> {
                        val data = targetState.data

                        // Synchronize selected day item for dialog if open
                        if (selectedDayForDialog != null) {
                            val updatedItem = data.dayWiseAttendance.find { it.date == selectedDayForDialog!!.date }
                            if (updatedItem != null) {
                                selectedDayForDialog = updatedItem
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Attendance",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    IconButton(
                                        onClick = onNavigateToSettings,
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings, 
                                            contentDescription = "Settings", 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // --- VIEW MODE TOGGLE (SUBJECT-WISE vs DAY-WISE) ---
                            item {
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    SegmentedButton(
                                        selected = data.viewMode == AttendanceViewMode.SUBJECT_WISE,
                                        onClick = { viewModel.setViewMode(AttendanceViewMode.SUBJECT_WISE) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                    ) {
                                        Text("Subject-wise", fontWeight = FontWeight.Bold)
                                    }
                                    SegmentedButton(
                                        selected = data.viewMode == AttendanceViewMode.DAY_WISE,
                                        onClick = { viewModel.setViewMode(AttendanceViewMode.DAY_WISE) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                    ) {
                                        Text("Day-wise", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (data.viewMode == AttendanceViewMode.SUBJECT_WISE) {
                                if (data.subjectsAttendance.isEmpty()) {
                                    item {
                                        EmptyAttendanceState()
                                    }
                                } else {
                                    items(
                                        items = data.subjectsAttendance,
                                        key = { it.subject.id }
                                    ) { item ->
                                        SubjectAttendanceCard(
                                            item = item, 
                                            onClick = { onNavigateToSubjectDetails(item.subject.id) },
                                            onAddGrace = { viewModel.addGraceClass(it) },
                                            onEditCounts = { subjectToEditCounts = it }
                                        )
                                    }
                                }
                            } else {
                                if (data.dayWiseAttendance.isEmpty()) {
                                    item {
                                        EmptyAttendanceState()
                                    }
                                } else {
                                    items(
                                        items = data.dayWiseAttendance,
                                        key = { it.date.toString() }
                                    ) { dayItem ->
                                        DayWiseAttendanceCard(
                                            dayItem = dayItem,
                                            onClick = { selectedDayForDialog = dayItem }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedDayForDialog != null) {
            DayDetailsDialog(
                dayItem = selectedDayForDialog!!,
                onDismiss = { selectedDayForDialog = null },
                onUpdateStatus = { record, newStatus ->
                    viewModel.updateHistoricalRecordStatus(record, selectedDayForDialog!!.date, newStatus)
                }
            )
        }

        if (subjectToEditCounts != null) {
            EditCountsDialog(
                item = subjectToEditCounts!!,
                onDismiss = { subjectToEditCounts = null },
                onSave = { attended, conducted, grace ->
                    viewModel.updateManualCounts(subjectToEditCounts!!.subject.id, attended, conducted, grace)
                    subjectToEditCounts = null
                }
            )
        }
    }
}

@Composable
fun SubjectAttendanceCard(
    item: SubjectAttendanceItem, 
    onClick: () -> Unit,
    onAddGrace: (Long) -> Unit,
    onEditCounts: (SubjectAttendanceItem) -> Unit
) {
    val target = item.subject.targetAttendance
    val percentage = item.attendancePercentage
    
    val statusColor = remember(percentage, target) {
        when {
            percentage == null -> Color.Gray
            percentage >= target -> StatusAttended
            percentage >= (target - 5) -> Color(0xFFF59E0B) // Amber
            else -> StatusAbsent
        }
    }

    val cardDesc = buildString {
        append(item.subject.name)
        append(", Attendance: ${item.percentageText}")
        append(", Target Goal: $target%")
        append(", ${item.attendedCount} of ${item.conductedCount} classes attended")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = cardDesc
            },
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subject.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target Goal: $target%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditCounts(item) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Counts",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.percentageText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.attendedCount} / ${item.conductedCount} classes attended",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { onAddGrace(item.subject.id) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("+ Grace", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (percentage ?: 0f) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem("${item.attendedCount} Attended", StatusAttended)
                    StatItem("${item.absentCount} Absent", StatusAbsent)
                    StatItem("${item.subject.graceCount} Grace", MaterialTheme.colorScheme.tertiary)
                }
                
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    BunkInfoBadge(item.bunkStatus)
                }
            }
        }
    }
}

@Composable
fun EditCountsDialog(
    item: SubjectAttendanceItem,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var attendedText by remember { mutableStateOf(item.attendedCount.toString()) }
    var conductedText by remember { mutableStateOf(item.conductedCount.toString()) }
    var graceText by remember { mutableStateOf(item.subject.graceCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Counts: ${item.subject.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You are manually editing the attendance counts and grace classes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = attendedText,
                    onValueChange = { attendedText = it.filter { char -> char.isDigit() } },
                    label = { Text("Attended Classes", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = conductedText,
                    onValueChange = { conductedText = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Conducted Classes", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = graceText,
                    onValueChange = { graceText = it.filter { char -> char.isDigit() } },
                    label = { Text("Grace Classes", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val attended = attendedText.toIntOrNull() ?: item.attendedCount
                    val conducted = conductedText.toIntOrNull() ?: item.conductedCount
                    val grace = graceText.toIntOrNull() ?: item.subject.graceCount
                    onSave(attended, conducted, grace)
                },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun DayWiseAttendanceCard(
    dayItem: DayWiseAttendanceItem,
    onClick: () -> Unit
) {
    val totalCount = dayItem.records.size
    val attendedCount = dayItem.records.count { it.status == AttendanceStatus.ATTENDED }
    val absentCount = dayItem.records.count { it.status == AttendanceStatus.ABSENT }
    val cancelledCount = dayItem.records.count { it.status == AttendanceStatus.CANCELLED }
    val isHoliday = dayItem.records.isNotEmpty() && dayItem.records.all { it.status == AttendanceStatus.CANCELLED }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayItem.dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isHoliday) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "🌴 Holiday",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem("Total", totalCount.toString(), MaterialTheme.colorScheme.onSurface)
                SummaryMetricItem("Attended", attendedCount.toString(), StatusAttended)
                SummaryMetricItem("Absent", absentCount.toString(), StatusAbsent)
                SummaryMetricItem("Cancelled", cancelledCount.toString(), StatusCancelled)
            }
        }
    }
}

@Composable
fun SummaryMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DayDetailsDialog(
    dayItem: DayWiseAttendanceItem,
    onDismiss: () -> Unit,
    onUpdateStatus: (DayClassStatusItem, AttendanceStatus) -> Unit
) {
    val isHoliday = dayItem.records.isNotEmpty() && dayItem.records.all { it.status == AttendanceStatus.CANCELLED }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dayItem.dateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxHeightPx = maxHeight
                val constrainedMaxHeight = (maxHeightPx * 0.65f).coerceIn(240.dp, 460.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = constrainedMaxHeight)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isHoliday) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌴 Marked as Holiday / All Classes Cancelled",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Class Status Breakdown (Tap to Change)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = dayItem.records,
                            key = { it.recordId }
                        ) { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = record.subjectName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (record.isExtra) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = MaterialTheme.shapes.extraSmall
                                                ) {
                                                    Text(
                                                        text = "EXTRA",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        DayStatusBadge(record.status)
                                    }

                                    // Compact Interactive Status Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatusSelectorButton(
                                            modifier = Modifier.weight(1f),
                                            label = "Attended",
                                            selected = record.status == AttendanceStatus.ATTENDED,
                                            color = StatusAttended,
                                            onClick = { onUpdateStatus(record, AttendanceStatus.ATTENDED) }
                                        )
                                        StatusSelectorButton(
                                            modifier = Modifier.weight(1f),
                                            label = "Absent",
                                            selected = record.status == AttendanceStatus.ABSENT,
                                            color = StatusAbsent,
                                            onClick = { onUpdateStatus(record, AttendanceStatus.ABSENT) }
                                        )
                                        StatusSelectorButton(
                                            modifier = Modifier.weight(1f),
                                            label = "Cancelled",
                                            selected = record.status == AttendanceStatus.CANCELLED,
                                            color = StatusCancelled,
                                            onClick = { onUpdateStatus(record, AttendanceStatus.CANCELLED) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", fontWeight = FontWeight.Black)
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun StatusSelectorButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DayStatusBadge(status: AttendanceStatus) {
    val (text, color) = when (status) {
        AttendanceStatus.ATTENDED -> "Attended" to StatusAttended
        AttendanceStatus.ABSENT -> "Absent" to StatusAbsent
        AttendanceStatus.CANCELLED -> "Cancelled" to StatusCancelled
        else -> "Pending" to StatusPending
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun BunkInfoBadge(status: BunkStatus) {
    val (text, color) = when (status) {
        BunkStatus.NoData -> "" to Color.Transparent
        is BunkStatus.CanBunk -> {
            if (status.count > 0) {
                "Safe to bunk: ${status.count}" to StatusAttended
            } else {
                "Critical: Don't bunk!" to Color(0xFFF59E0B)
            }
        }
        is BunkStatus.MustAttend -> "Required classes: ${status.count}" to StatusAbsent
    }
    
    if (text.isNotEmpty()) {
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.medium,
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun StatItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(7.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun EmptyAttendanceState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📊", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No stats yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mark your attendance on the Home\nscreen to calculate your budget.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
