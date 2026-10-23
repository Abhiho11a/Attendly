package com.example.attendly.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendly.AttendlyApplication
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.ui.components.TopNotificationHost
import com.example.attendly.ui.viewmodel.HomeViewModel
import com.example.attendly.ui.viewmodel.HomeClassItem
import com.example.attendly.ui.viewmodel.DayAttendanceItem
import com.example.attendly.ui.viewmodel.ViewModelFactory
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import com.example.attendly.ui.theme.StatusAttended
import com.example.attendly.ui.theme.StatusAbsent
import com.example.attendly.ui.theme.StatusCancelled
import com.example.attendly.ui.theme.StatusPending

@Composable
fun HomeScreen(
    onNavigateToTimetable: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository,
            preferencesRepository = (LocalContext.current.applicationContext as AttendlyApplication).preferences
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var showExtraClassDialog by remember { mutableStateOf(false) }
    var showCancelAllDialog by remember { mutableStateOf(false) }

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
            Crossfade(
                targetState = uiState.isLoading,
                animationSpec = tween(180),
                label = "homeLoadingFade"
            ) { isLoading ->
                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        com.example.attendly.ui.components.SkeletonBox(height = 32.dp, width = 160.dp)
                        com.example.attendly.ui.components.SkeletonCard(height = 110.dp)
                        com.example.attendly.ui.components.SkeletonCard(height = 80.dp)
                        com.example.attendly.ui.components.SkeletonCard(height = 80.dp)
                    }
                } else {
                    HomeContent(
                        uiState = uiState,
                        onStatusChange = { item, date, status ->
                            viewModel.markAttendance(item, date, status)
                        },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToTimetable = onNavigateToTimetable,
                        onAddExtraClass = { showExtraClassDialog = true },
                        onCancelAllClasses = { showCancelAllDialog = true },
                        onPrevDay = { viewModel.selectPreviousDay() },
                        onNextDay = { viewModel.selectNextDay() }
                    )
                }
            }
        }
    }

    if (showExtraClassDialog) {
        AddExtraClassDialog(
            subjects = uiState.subjects,
            onDismiss = { showExtraClassDialog = false },
            onSave = { subjectId, status ->
                viewModel.addExtraClass(subjectId, status)
                showExtraClassDialog = false
            },
            onAddSubject = { name ->
                viewModel.addSubject(name)
            }
        )
    }

    if (showCancelAllDialog) {
        val isToday = uiState.selectedDate == LocalDate.now()
        val targetDayText = if (isToday) "today" else "this day"

        AlertDialog(
            onDismissRequest = { showCancelAllDialog = false },
            title = {
                Text("Mark $targetDayText as Holiday?", fontWeight = FontWeight.Black)
            },
            text = {
                Text(
                    "This will mark all classes for $targetDayText as cancelled. Are you okay with that?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelAllClassesForSelectedDate()
                        showCancelAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Yes, Mark Holiday 🌴", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelAllDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
fun HomeContent(
    uiState: com.example.attendly.ui.viewmodel.HomeUiState,
    onStatusChange: (HomeClassItem, LocalDate, AttendanceStatus) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onAddExtraClass: () -> Unit,
    onCancelAllClasses: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val dayItem = uiState.selectedDayItem

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendly",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onAddExtraClass,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Extra Class", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        item {
            HeaderSection(uiState.greeting)
        }
        
        item {
            ProgressSection(uiState.markedClassesToday, uiState.totalClassesToday)
        }

        // --- DATE NAVIGATION & HOLIDAY ACTION ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevDay,
                            enabled = uiState.canNavigatePrevious
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Previous Day", 
                                tint = if (uiState.canNavigatePrevious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayItem?.dateText ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (dayItem?.isToday == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (dayItem?.isToday == false) {
                                Text(
                                    text = "HISTORY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNextDay,
                            enabled = uiState.canNavigateNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward, 
                                contentDescription = "Next Day", 
                                tint = if (uiState.canNavigateNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }

                    if (dayItem != null && dayItem.isToday && dayItem.classes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = onCancelAllClasses,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌴",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mark Today as Holiday",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dayItem == null || dayItem.classes.isEmpty()) {
            item {
                EmptyHomeState(onNavigateToTimetable)
            }
        } else {
            items(dayItem.classes, key = { "item_${dayItem.date}_${it.id}_${it.isExtra}" }) { item ->
                ClassCard(item, dayItem.date, onStatusChange)
            }
        }
    }
}

@Composable
fun HeaderSection(greeting: String) {
    val cleanGreeting = greeting.replace(Regex("[^\\w\\s,!]"), "").trim()
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = cleanGreeting
            }
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ProgressSection(marked: Int, total: Int) {
    val progress = if (total > 0) marked.toFloat() / total else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Today's Completion",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$marked of $total classes tracked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun ClassCard(
    item: HomeClassItem,
    date: LocalDate,
    onStatusChange: (HomeClassItem, LocalDate, AttendanceStatus) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val timeRange = if (item.startTime != null && item.endTime != null) {
        "${item.startTime.format(timeFormatter)} - ${item.endTime.format(timeFormatter)}"
    } else "Extra / Unscheduled"

    val cleanContentDesc = buildString {
        append(item.subject.name)
        if (item.isExtra) append(", Extra class")
        append(", $timeRange")
        if (!item.room.isNullOrBlank()) append(", Room ${item.room}")
        append(", Status: ${item.status.name.lowercase()}")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics(mergeDescendants = true) {
                contentDescription = cleanContentDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.isExtra) {
                            Spacer(modifier = Modifier.width(8.dp))
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (!item.room.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = "Room ${item.room}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                StatusBadge(item.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttendanceButton(
                    modifier = Modifier.weight(1f),
                    label = "Attended",
                    icon = Icons.Default.Check,
                    selected = item.status == AttendanceStatus.ATTENDED,
                    color = StatusAttended,
                    onClick = { onStatusChange(item, date, AttendanceStatus.ATTENDED) }
                )
                AttendanceButton(
                    modifier = Modifier.weight(1f),
                    label = "Absent",
                    icon = Icons.Default.Close,
                    selected = item.status == AttendanceStatus.ABSENT,
                    color = StatusAbsent,
                    onClick = { onStatusChange(item, date, AttendanceStatus.ABSENT) }
                )
                AttendanceButton(
                    modifier = Modifier.weight(1f),
                    label = "Cancelled",
                    icon = Icons.Default.Remove,
                    selected = item.status == AttendanceStatus.CANCELLED,
                    color = StatusCancelled,
                    onClick = { onStatusChange(item, date, AttendanceStatus.CANCELLED) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExtraClassDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, AttendanceStatus) -> Unit,
    onAddSubject: (String) -> Unit
) {
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf(AttendanceStatus.ATTENDED) }
    var expanded by remember { mutableStateOf(false) }
    var showNewSubjectField by remember { mutableStateOf(subjects.isEmpty()) }
    var newSubjectName by remember { mutableStateOf("") }

    LaunchedEffect(subjects) {
        if (subjectId == null && subjects.isNotEmpty()) {
            subjectId = subjects.last().id
            showNewSubjectField = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Unscheduled Class", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 8.dp)) {
                Column {
                    Text("Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!showNewSubjectField && subjects.isNotEmpty()) {
                        Box {
                            Surface(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        subjects.find { it.id == subjectId }?.name ?: "Select subject",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = expanded, 
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                subjects.forEach { subject ->
                                    DropdownMenuItem(
                                        text = { Text(subject.name, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            subjectId = subject.id
                                            expanded = false
                                        }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("+ Create New Subject", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) },
                                    onClick = {
                                        showNewSubjectField = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = { newSubjectName = it },
                            label = { Text("New Subject Name", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (newSubjectName.isNotBlank()) {
                                        onAddSubject(newSubjectName.trim())
                                        newSubjectName = ""
                                    }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }

                Column {
                    Text("Mark As", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.forEach { option ->
                            DialogStatusButton(
                                modifier = Modifier.weight(1f),
                                label = option.label,
                                selected = status == option.status,
                                color = option.color,
                                onClick = { status = option.status }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { subjectId?.let { onSave(it, status) } },
                enabled = subjectId != null,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Record", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DialogStatusButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private data class StatusOption(val status: AttendanceStatus, val label: String, val color: Color)
private val statusOptions = listOf(
    StatusOption(AttendanceStatus.ATTENDED, "Attended", StatusAttended),
    StatusOption(AttendanceStatus.ABSENT, "Absent", StatusAbsent),
    StatusOption(AttendanceStatus.CANCELLED, "Cancelled", StatusCancelled)
)

@Composable
fun StatusBadge(status: AttendanceStatus) {
    if (status == AttendanceStatus.PENDING) return

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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun AttendanceButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = if (selected) 6.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun EmptyHomeState(onNavigateToTimetable: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "🎉", style = MaterialTheme.typography.displaySmall)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No classes found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Set up your timetable or log\nan extra class to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToTimetable,
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Go to Timetable", fontWeight = FontWeight.Black)
        }
    }
}
