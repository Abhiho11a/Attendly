package com.example.attendly.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendly.AttendlyApplication
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.ui.components.TopNotificationHost
import com.example.attendly.ui.viewmodel.SettingsViewModel
import com.example.attendly.ui.viewmodel.ViewModelFactory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository,
            preferencesRepository = (LocalContext.current.applicationContext as AttendlyApplication).preferences
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setRemindersEnabled(true)
            com.example.attendly.notifications.AttendanceReminderScheduler.schedule(context)
        } else {
            viewModel.setRemindersEnabled(false)
            com.example.attendly.notifications.AttendanceReminderScheduler.cancel(context)
            notificationMessage = "Notification permission is required for attendance reminders ⚠️"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            notificationMessage = message
        }
    }

    // Launcher for creating a file (Export)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportData { json ->
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                viewModel.onBackupExportSuccessful()
            }
        }
    }

    // Launcher for opening a file (Import)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                viewModel.importData(reader.readText())
            }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Personalization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Display Name",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.displayName.ifBlank { "Not set" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showEditNameDialog = true },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Edit", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Manage Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Edit subject names or set their target attendance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (uiState.subjects.isEmpty()) {
                    item {
                        Text(
                            text = "No subjects added yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(uiState.subjects) { subject ->
                        SubjectSettingsItem(
                            subject = subject,
                            onTargetChange = { viewModel.updateSubjectTarget(subject, it) },
                            onEditClick = { subjectToEdit = subject },
                            onDeleteClick = { subjectToDelete = subject }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Notifications & Reminders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Attendance Reminder",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Remind me if I haven't updated today's class attendance.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.remindersEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    val granted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (granted) {
                                                        viewModel.setRemindersEnabled(true)
                                                        com.example.attendly.notifications.AttendanceReminderScheduler.schedule(context)
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                } else {
                                                    viewModel.setRemindersEnabled(true)
                                                    com.example.attendly.notifications.AttendanceReminderScheduler.schedule(context)
                                                }
                                            } else {
                                                viewModel.setRemindersEnabled(false)
                                                com.example.attendly.notifications.AttendanceReminderScheduler.cancel(context)
                                            }
                                        }
                                    )
                                }

                                if (uiState.remindersEnabled) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", uiState.reminderHour, uiState.reminderMinute)
                                    OutlinedButton(
                                        onClick = {
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    viewModel.setReminderTime(hour, minute)
                                                    com.example.attendly.notifications.AttendanceReminderScheduler.schedule(context)
                                                },
                                                uiState.reminderHour,
                                                uiState.reminderMinute,
                                                false
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Reminder Time: $formattedTime", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "JSON Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Last saved",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.lastBackupDateText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = { createDocumentLauncher.launch("attendly_backup.json") },
                                        modifier = Modifier.weight(1f),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Export JSON", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                                        modifier = Modifier.weight(1f),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Import JSON", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(text = "About Attendly", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Attendly is a modern attendance tracking app for students. Build for productivity and efficiency.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                item {
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("Reset All Data", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all subjects, timetable entries, and attendance records? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showResetDialog = false
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Subject Dialog
    subjectToEdit?.let { subject ->
        var newName by remember { mutableStateOf(subject.name) }
        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Edit Subject Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.updateSubjectName(subject, newName)
                        subjectToEdit = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Subject Dialog
    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${subject.name}'? This will also remove all its timetable entries and attendance history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(subject)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditNameDialog) {
        var editedName by remember { mutableStateOf(uiState.displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { if (it.length <= 50) editedName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editedName.isNotBlank()) {
                        viewModel.updateDisplayName(editedName)
                        showEditNameDialog = false
                    }
                }) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubjectSettingsItem(
    subject: SubjectEntity,
    onTargetChange: (Int) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var targetValue by remember(subject.targetAttendance) { mutableFloatStateOf(subject.targetAttendance.toFloat()) }

    val itemDesc = "Subject ${subject.name}, Target attendance ${targetValue.toInt()} percent"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = itemDesc
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${subject.name}", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "${targetValue.toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete ${subject.name}", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Slider(
                value = targetValue,
                onValueChange = { targetValue = it },
                onValueChangeFinished = { onTargetChange(targetValue.toInt()) },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
