package com.strengthtracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
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
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.SettingsViewModel
import com.strengthtracker.util.CsvManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun SettingsScreen(
    repository: WorkoutRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.strengthtracker.ui.viewmodel.SettingsViewModel.Companion.Factory(repository, context))
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCsvMenu by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    // Show snackbar when message arrives
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            settingsViewModel.clearSnackbar()
        }
    }

    // ── CSV exporters ────────────────────────────────────────────────────────

    val exportWorkoutsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val (workouts, exercisesMap) = settingsViewModel.getDataForWorkoutExport()
                CsvManager.exportWorkouts(context, it, workouts, exercisesMap)
            }
        }
    }

    val exportLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val (sessions, workouts, exercisesMap) = settingsViewModel.getDataForLogExport()
                val logs = repository.getAllLogs()
                CsvManager.exportLogs(context, it, logs, workouts, exercisesMap, sessions)
            }
        }
    }

    // ── CSV import ───────────────────────────────────────────────────────────

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val imported = CsvManager.importWorkouts(context, it)
                settingsViewModel.importWorkouts(imported)
            }
        }
    }

    // ── Screen ───────────────────────────────────────────────────────────────

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                settingsViewModel.saveAllSettings()
                                onBack()
                            }
                        }
                    ) {
                        Text("SAVE")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCategory(title = "Sound") {
                SettingSliderRow(
                    label = "Beep Volume",
                    value = uiState.volume,
                    valueRange = 0f..100f,
                    onValueChange = { settingsViewModel.setVolume(it.toInt()) },
                    valueFormatter = { "$it%" },
                    icon = Icons.Default.VolumeUp
                )
                SettingSliderRow(
                    label = "Beep Duration",
                    value = uiState.beepDuration,
                    valueRange = 200f..1000f,
                    onValueChange = { settingsViewModel.setBeepDuration(it.toInt()) },
                    valueFormatter = { "${it}ms" },
                    icon = Icons.Default.Timer
                )
                SettingToggleRow(
                    label = "Prepare Beep (10s)",
                    checked = uiState.prepareBeepEnabled,
                    onCheckedChange = { settingsViewModel.setPrepareBeepEnabled(it) },
                    icon = Icons.Default.Notifications
                )
                SettingToggleRow(
                    label = "End Beep (0s)",
                    checked = uiState.endBeepEnabled,
                    onCheckedChange = { settingsViewModel.setEndBeepEnabled(it) },
                    icon = Icons.Default.Audiotrack
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCategory(title = "Workout Defaults") {
                SettingTextFieldRowWithLabel(
                    label = "Default Rest Timer",
                    value = uiState.defaultRestString,
                    onValueChange = { settingsViewModel.setDefaultRestString(it) },
                    hint = "e.g. 90",
                    icon = Icons.Default.Schedule
                )
                SettingTextFieldRowWithLabel(
                    label = "Default Sets",
                    value = uiState.defaultSetsString,
                    onValueChange = { settingsViewModel.setDefaultSetsString(it) },
                    hint = "e.g. 3",
                    icon = Icons.Default.List
                )
                SettingSegmentedButtonRow(
                    label = "Default Exercise Type",
                    options = SettingsViewModel.ExerciseTypeOptions,
                    selectedValue = uiState.defaultExerciseType,
                    onValueChange = { settingsViewModel.setDefaultExerciseType(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCategory(title = "Display") {
                SettingSegmentedButtonRow(
                    label = "Theme",
                    options = SettingsViewModel.ThemeOptions,
                    selectedValue = uiState.themeMode,
                    onValueChange = { settingsViewModel.setThemeMode(it) }
                )
                SettingSliderRow(
                    label = "Font Size",
                    value = uiState.fontSize,
                    valueRange = 12f..24f,
                    onValueChange = { settingsViewModel.setFontSize(it.toInt()) },
                    valueFormatter = { "${it}sp" },
                    icon = Icons.Default.TextFields
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCategory(title = "Data") {
                SettingButtonRow(
                    label = "Export Workouts as CSV",
                    description = "Export workout names and exercises",
                    icon = Icons.Default.Share,
                    onClick = {
                        exportWorkoutsLauncher.launch("workouts_export.csv")
                    }
                )
                SettingButtonRow(
                    label = "Export Logs as CSV",
                    description = "Export all exercise history",
                    icon = Icons.Default.Share,
                    onClick = {
                        exportLogsLauncher.launch("logs_export.csv")
                    }
                )
                SettingButtonRow(
                    label = "Import Workouts from CSV",
                    description = "Add workouts from a CSV file",
                    icon = Icons.Default.Add,
                    onClick = {
                        importLauncher.launch("text/csv")
                    }
                )
                SettingButtonRow(
                    label = "Clear All Data",
                    description = "Delete all workouts, exercises, and history",
                    icon = Icons.Default.Delete,
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── About ──────────────────────────────────────────────────────

            AboutSection()

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── CSV dropdown menu ────────────────────────────────────────────
        if (showCsvMenu) {
            DropdownMenu(
                expanded = showCsvMenu,
                onDismissRequest = { showCsvMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export workouts as CSV") },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    onClick = {
                        showCsvMenu = false
                        exportWorkoutsLauncher.launch("workouts_export.csv")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export logs as CSV") },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                    onClick = {
                        showCsvMenu = false
                        exportLogsLauncher.launch("logs_export.csv")
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Import workouts from CSV") },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    },
                    onClick = {
                        showCsvMenu = false
                        importLauncher.launch("text/csv")
                    }
                )
            }
        }
    }

    // ── Clear data confirmation dialog ───────────────────────────────────

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Clear all data?") },
            text = { Text("This will permanently delete all workouts, exercises, and history logs. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        coroutineScope.launch {
                            repository.deleteAllWorkouts()
                            repository.deleteAllExercises()
                            repository.deleteAllHistoryLogs()
                            repository.deleteAllWorkoutSessions()
                        }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Preference components ────────────────────────────────────────────────────

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column {
              content()
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Int) -> String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueFormatter(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = hint ?: label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true
    )
}

@Composable
private fun SettingTextFieldRowWithLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingSegmentedButtonRow(
    label: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                SegmentedButton(
                    selected = selectedValue == option.first,
                    onClick = { onValueChange(option.first) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(0)
                ) {
                    Text(option.second)
                }
            }
        }
    }
}

@Composable
private fun SettingButtonRow(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "strengthtracker",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
