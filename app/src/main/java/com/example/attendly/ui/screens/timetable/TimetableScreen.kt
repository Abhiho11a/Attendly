package com.example.attendly.ui.screens.timetable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendly.AttendlyApplication
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.entity.TimetableClassEntity
import com.example.attendly.ui.components.TopNotificationHost
import com.example.attendly.ui.viewmodel.TimetableViewModel
import com.example.attendly.ui.viewmodel.ViewModelFactory
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToImport: () -> Unit,
    viewModel: TimetableViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddClassDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<TimetableClassEntity?>(null) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

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

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddClassDialog = true },
                    modifier = Modifier.padding(bottom = 85.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.large,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Class", modifier = Modifier.size(28.dp))
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timetable",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onNavigateToImport,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨ Import", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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

                DaySelector(
                    selectedDay = uiState.selectedDay,
                    onDaySelected = { viewModel.selectDay(it) }
                )
                
                Crossfade(
                    targetState = uiState.isLoading,
                    animationSpec = tween(180),
                    label = "timetableLoadingFade"
                ) { isLoading ->
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.attendly.ui.components.SkeletonCard(height = 90.dp)
                            com.example.attendly.ui.components.SkeletonCard(height = 90.dp)
                            com.example.attendly.ui.components.SkeletonCard(height = 90.dp)
                        }
                    } else {
                        val classes = uiState.timetableByDay[uiState.selectedDay] ?: emptyList()
                        if (classes.isEmpty()) {
                            EmptyTimetableState(onImportClick = onNavigateToImport)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = classes,
                                    key = { tClass -> tClass.id }
                                ) { tClass ->
                                    val subject = uiState.subjects.find { it.id == tClass.subjectId }
                                    TimetableClassCard(
                                        tClass = tClass,
                                        subjectName = subject?.name ?: "Unknown",
                                        onEdit = { classToEdit = tClass },
                                        onDelete = { viewModel.deleteClass(tClass) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddClassDialog) {
        AddClassDialog(
            subjects = uiState.subjects,
            selectedDay = uiState.selectedDay,
            onDismiss = { showAddClassDialog = false },
            onSave = { subjectId, day, start, end, room ->
                viewModel.addClass(subjectId, day, start, end, room)
                showAddClassDialog = false
            },
            onAddSubject = { viewModel.addSubject(it) }
        )
    }

    if (classToEdit != null) {
        AddClassDialog(
            subjects = uiState.subjects,
            selectedDay = classToEdit!!.dayOfWeek,
            initialClass = classToEdit,
            onDismiss = { classToEdit = null },
            onSave = { subjectId, day, start, end, room ->
                viewModel.updateClass(classToEdit!!.id, subjectId, day, start, end, room)
                classToEdit = null
            },
            onAddSubject = { viewModel.addSubject(it) }
        )
    }
}

@Composable
fun DaySelector(selectedDay: DayOfWeek, onDaySelected: (DayOfWeek) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedDay.ordinal,
        edgePadding = 20.dp,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        DayOfWeek.values().forEach { day ->
            Tab(
                selected = selectedDay == day,
                onClick = { onDaySelected(day) },
                text = {
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        fontWeight = if (selectedDay == day) FontWeight.Black else FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun TimetableClassCard(
    tClass: TimetableClassEntity, 
    subjectName: String, 
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val timeRange = "${tClass.startTime.format(timeFormatter)} - ${tClass.endTime.format(timeFormatter)}"
    
    val classDesc = buildString {
        append(subjectName)
        append(", $timeRange")
        if (!tClass.room.isNullOrBlank()) append(", Room ${tClass.room}")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = classDesc
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                if (!tClass.room.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Room ${tClass.room}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit $subjectName",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete $subjectName",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClassDialog(
    subjects: List<SubjectEntity>,
    selectedDay: DayOfWeek,
    initialClass: TimetableClassEntity? = null,
    onDismiss: () -> Unit,
    onSave: (Long, DayOfWeek, LocalTime, LocalTime, String?) -> Unit,
    onAddSubject: (String) -> Unit
) {
    var subjectId by remember { mutableStateOf<Long?>(initialClass?.subjectId) }
    var startTime by remember { mutableStateOf(initialClass?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(initialClass?.endTime ?: LocalTime.of(10, 0)) }
    var room by remember { mutableStateOf(initialClass?.room ?: "") }
    var showNewSubjectField by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (initialClass == null) "Schedule New Class" else "Modify Class", 
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                // Subject Selection
                if (!showNewSubjectField) {
                    var expanded by remember { mutableStateOf(false) }
                    Column {
                        Text("Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
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
                                        subjects.find { it.id == subjectId }?.name ?: "Select a subject",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(24.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = expanded, 
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface)
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
                                    onAddSubject(newSubjectName)
                                    showNewSubjectField = false
                                    newSubjectName = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }

                // Time Pickers
                Column {
                    Text("Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Starts At", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(startTime.format(DateTimeFormatter.ofPattern("hh:mm a")), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Surface(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ends At", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(endTime.format(DateTimeFormatter.ofPattern("hh:mm a")), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Location (Optional)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    subjectId?.let { onSave(it, selectedDay, startTime, endTime, room.ifBlank { null }) }
                },
                enabled = subjectId != null,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(if (initialClass == null) "Add to Schedule" else "Update Schedule", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text("Discard Changes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showStartTimePicker) {
        val state = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute)
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = LocalTime.of(state.hour, state.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = LocalTime.of(state.hour, state.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = state)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm, shape = MaterialTheme.shapes.medium) { Text("Set Time", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Bold) }
        },
        text = { 
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun EmptyTimetableState(onImportClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📅", style = MaterialTheme.typography.displaySmall)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No classes scheduled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the + button below to build\nyour weekly timetable manually.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onImportClick,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✨ Let AI set up your schedule", fontWeight = FontWeight.Black)
            }
        }
    }
}
