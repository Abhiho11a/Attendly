package com.example.attendly.ui.screens.import

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.attendly.AttendlyApplication
import com.example.attendly.ai.ExtractedClass
import com.example.attendly.ui.viewmodel.ImportStep
import com.example.attendly.ui.viewmodel.ImportViewModel
import com.example.attendly.ui.viewmodel.ViewModelFactory
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTimetableScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportViewModel = viewModel(
        factory = ViewModelFactory(
            repository = (LocalContext.current.applicationContext as AttendlyApplication).repository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Image Picker Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageUri?.let { viewModel.onImageSelected(it) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required to take photo", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.step) {
        if (uiState.step == ImportStep.COMPLETED) {
            Toast.makeText(context, "Timetable imported successfully! 🎉", Toast.LENGTH_LONG).show()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Timetable", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = uiState.step, label = "ImportStepTransition") { step ->
                when (step) {
                    ImportStep.SELECT_IMAGE -> SelectImageStep(
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                    ImportStep.PREVIEW_IMAGE -> PreviewImageStep(
                        imageUri = uiState.imageUri,
                        error = uiState.error,
                        onRetake = { viewModel.reset() },
                        onAnalyze = { viewModel.analyzeTimetable(context) }
                    )
                    ImportStep.ANALYZING -> AnalyzingStep(progress = uiState.analyzingProgress)
                    ImportStep.REVIEWING -> ReviewingStep(
                        classes = uiState.extractedClasses.map { it.extracted },
                        reviewableClasses = uiState.extractedClasses,
                        onSetAction = { index, action -> viewModel.setImportAction(index, action) },
                        onUpdateClass = { index, updated -> viewModel.updateClass(index, updated) },
                        onRemoveClass = { viewModel.removeClass(index = it) },
                        onConfirm = { viewModel.importConfirmedClasses() }
                    )
                    ImportStep.COMPLETED -> Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun SelectImageStep(onGalleryClick: () -> Unit, onCameraClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "✨", style = MaterialTheme.typography.displayMedium)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "AI Timetable Import",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Upload a photo or screenshot of your weekly schedule, and we'll set it up for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onCameraClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Take Photo", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Choose from Gallery", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PreviewImageStep(
    imageUri: Uri?,
    error: String?,
    onRetake: () -> Unit,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Timetable Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "⚠ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Make sure the timetable is clearly visible.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Retake", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onAnalyze,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Analyze Timetable", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnalyzingStep(progress: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            Text(
                text = when(progress) {
                    0 -> "📁"
                    1 -> "🔍"
                    2 -> "🧠"
                    else -> "✨"
                },
                style = MaterialTheme.typography.headlineLarge
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressItem(text = "Image uploaded", isCompleted = progress >= 0, isCurrent = progress == 0)
            ProgressItem(text = "Reading timetable", isCompleted = progress >= 1, isCurrent = progress == 1)
            ProgressItem(text = "Detecting classes", isCompleted = progress >= 2, isCurrent = progress == 2)
            ProgressItem(text = "Preparing schedule", isCompleted = progress >= 3, isCurrent = progress == 3)
        }
    }
}

@Composable
fun ProgressItem(text: String, isCompleted: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier.width(200.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompleted) {
            Icon(
                Icons.Default.CheckCircle, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else if (isCurrent) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isCompleted || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium
        )
    }
}

@Composable
fun ReviewingStep(
    classes: List<ExtractedClass>,
    reviewableClasses: List<com.example.attendly.ui.viewmodel.ReviewableClass>,
    onSetAction: (Int, com.example.attendly.ui.viewmodel.ImportAction) -> Unit,
    onUpdateClass: (Int, ExtractedClass) -> Unit,
    onRemoveClass: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    var classToEditIndex by remember { mutableStateOf<Int?>(null) }
    val selectedCount = reviewableClasses.count { it.isSelected }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Review Timetable",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "AI found ${classes.size} classes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "$selectedCount ready",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val groupedByDay = classes.mapIndexed { index, extracted -> index to extracted }
                .groupBy { it.second.day }
            
            DayOfWeek.values().forEach { day ->
                val dayClasses = groupedByDay[day] ?: return@forEach
                
                item {
                    Text(
                        text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                itemsIndexed(dayClasses) { _, (originalIndex, extracted) ->
                    val reviewable = reviewableClasses[originalIndex]
                    ExtractedClassCard(
                        extracted = extracted,
                        reviewable = reviewable,
                        onActionChange = { onSetAction(originalIndex, it) },
                        onEdit = { classToEditIndex = originalIndex },
                        onDelete = { onRemoveClass(originalIndex) }
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(16.dp),
                enabled = selectedCount > 0
            ) {
                Text("Add $selectedCount classes to timetable", fontWeight = FontWeight.Black)
            }
        }
    }

    if (classToEditIndex != null) {
        val index = classToEditIndex!!
        EditExtractedClassDialog(
            initialClass = classes[index],
            onDismiss = { classToEditIndex = null },
            onSave = { updated ->
                onUpdateClass(index, updated)
                classToEditIndex = null
            }
        )
    }
}

@Composable
fun ExtractedClassCard(
    extracted: ExtractedClass,
    reviewable: com.example.attendly.ui.viewmodel.ReviewableClass,
    onActionChange: (com.example.attendly.ui.viewmodel.ImportAction) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val timeRange = "${extracted.startTime.format(timeFormatter)} - ${extracted.endTime.format(timeFormatter)}"
    val isSelected = reviewable.isSelected
    
    val cardDescription = buildString {
        append(extracted.subject)
        append(", $timeRange")
        if (!extracted.room.isNullOrBlank()) append(", Room ${extracted.room}")
        if (reviewable.isDuplicate) append(", Duplicate class found")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = cardDescription
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extracted.subject,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!extracted.room.isNullOrBlank()) {
                        Text(
                            text = "Room ${extracted.room}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${extracted.subject}", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete ${extracted.subject}", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (reviewable.isDuplicate) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Possible duplicate found", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DuplicateOptionButton(
                            text = "Keep Existing",
                            isSelected = reviewable.action == com.example.attendly.ui.viewmodel.ImportAction.KEEP_EXISTING,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionChange(com.example.attendly.ui.viewmodel.ImportAction.KEEP_EXISTING) }
                        )
                        DuplicateOptionButton(
                            text = "Replace",
                            isSelected = reviewable.action == com.example.attendly.ui.viewmodel.ImportAction.REPLACE,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionChange(com.example.attendly.ui.viewmodel.ImportAction.REPLACE) }
                        )
                        DuplicateOptionButton(
                            text = "Add Anyway",
                            isSelected = reviewable.action == com.example.attendly.ui.viewmodel.ImportAction.ADD,
                            modifier = Modifier.weight(1f),
                            onClick = { onActionChange(com.example.attendly.ui.viewmodel.ImportAction.ADD) }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (extracted.confidence < 0.8f) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Needs review", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Looks good", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    TextButton(onClick = { onActionChange(if (isSelected) com.example.attendly.ui.viewmodel.ImportAction.KEEP_EXISTING else com.example.attendly.ui.viewmodel.ImportAction.ADD) }) {
                        Text(if (isSelected) "Skip" else "Keep", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateOptionButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExtractedClassDialog(
    initialClass: ExtractedClass,
    onDismiss: () -> Unit,
    onSave: (ExtractedClass) -> Unit
) {
    var subject by remember { mutableStateOf(initialClass.subject) }
    var day by remember { mutableStateOf(initialClass.day) }
    var startTime by remember { mutableStateOf(initialClass.startTime) }
    var endTime by remember { mutableStateOf(initialClass.endTime) }
    var room by remember { mutableStateOf(initialClass.room ?: "") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Extracted Class", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                var dayExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { dayExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault()), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        DayOfWeek.values().forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                                onClick = {
                                    day = d
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Starts", style = MaterialTheme.typography.labelSmall)
                            Text(startTime.format(DateTimeFormatter.ofPattern("hh:mm a")), fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ends", style = MaterialTheme.typography.labelSmall)
                            Text(endTime.format(DateTimeFormatter.ofPattern("hh:mm a")), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room (Optional)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initialClass.copy(
                        subject = subject,
                        day = day,
                        startTime = startTime,
                        endTime = endTime,
                        room = room.ifBlank { null }
                    ))
                },
                shape = MaterialTheme.shapes.large
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
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
        ) { TimePicker(state = state) }
    }

    if (showEndTimePicker) {
        val state = rememberTimePickerState(initialHour = endTime.hour, initialMinute = endTime.minute)
        TimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = LocalTime.of(state.hour, state.minute)
                showEndTimePicker = false
            }
        ) { TimePicker(state = state) }
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

fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("timetable_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        tempFile
    )
}
