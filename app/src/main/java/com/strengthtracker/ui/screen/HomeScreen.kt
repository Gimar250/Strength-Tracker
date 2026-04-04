package com.strengthtracker.ui.screen

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.HomeViewModel
import com.strengthtracker.util.CsvManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: WorkoutRepository,
    onStartWorkout: (Long) -> Unit,
    onEditWorkout: (Long) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewWorkoutDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }
    var showCsvMenu by remember { mutableStateOf(false) }

    // Show snackbar when message arrives
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // ── SAF Launchers ───────────────────────────────────────────────────────

    val exportWorkoutsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val (workouts, exercisesMap) = viewModel.getDataForWorkoutExport()
                CsvManager.exportWorkouts(context, uri, workouts, exercisesMap)
                viewModel.setSnackbarMessage("Workouts exported")
            } catch (e: Exception) {
                viewModel.setSnackbarMessage("Export failed: ${e.message}")
            }
        }
    }

    val exportLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val workouts = uiState.workouts
                val exercisesMap = repository.getAllExercisesGrouped()
                val logs = repository.getAllLogs()
                CsvManager.exportLogs(context, uri, logs, workouts, exercisesMap)
                viewModel.setSnackbarMessage("${logs.size} log entries exported")
            } catch (e: Exception) {
                viewModel.setSnackbarMessage("Export failed: ${e.message}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        // Accept any text file — some file managers don't tag CSVs correctly
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val imported = CsvManager.importWorkouts(context, uri)
                withContext(Dispatchers.Main) {
                    viewModel.importWorkouts(imported)
                }
            } catch (e: Exception) {
                viewModel.setSnackbarMessage("Import failed: ${e.message}")
            }
        }
    }

    // ── Scaffold ────────────────────────────────────────────────────────────

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "EDIT WORKOUTS" else "WORKOUTS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (uiState.isEditMode) {
                        // CSV overflow menu
                        Box {
                            IconButton(onClick = { showCsvMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "CSV options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
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
                                        importLauncher.launch(arrayOf("text/*", "*/*"))
                                    }
                                )
                            }
                        }
                        // Done button
                        IconButton(onClick = { viewModel.exitEditMode() }) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = "Done",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleEditMode() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit workouts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState.isEditMode) {
                FloatingActionButton(
                    onClick = { showNewWorkoutDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add workout")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.workouts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isEditMode) "No workouts yet.\nTap + to create one."
                    else "No workouts.\nTap the pencil to add some.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.workouts, key = { _, w -> w.id }) { index, workout ->
                    if (uiState.isEditMode) {
                        WorkoutEditCard(
                            workout = workout,
                            isFirst = index == 0,
                            isLast = index == uiState.workouts.lastIndex,
                            onMoveUp = { viewModel.moveWorkoutUp(workout) },
                            onMoveDown = { viewModel.moveWorkoutDown(workout) },
                            onEditExercises = {
                                viewModel.exitEditMode()
                                onEditWorkout(workout.id)
                            },
                            onDelete = { workoutToDelete = workout }
                        )
                    } else {
                        WorkoutCard(workout = workout, onClick = { onStartWorkout(workout.id) })
                    }
                }
                // Bottom padding so FAB doesn't cover last item
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // CSV busy indicator
        if (uiState.isCsvBusy) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────

    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text("Delete Workout?", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text("\"${workout.name}\" and all its exercises will be permanently deleted.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWorkout(workout.id); workoutToDelete = null }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    if (showNewWorkoutDialog) {
        NewWorkoutDialog(
            onConfirm = { name ->
                viewModel.createWorkout(name) { newId ->
                    showNewWorkoutDialog = false
                    viewModel.exitEditMode()
                    onEditWorkout(newId)
                }
            },
            onDismiss = { showNewWorkoutDialog = false }
        )
    }
}

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text(
                text = workout.name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WorkoutEditCard(
    workout: Workout,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditExercises: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workout.name.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, "Move up",
                        tint = if (isFirst) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, "Move down",
                        tint = if (isLast) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface)
                }
            }
            IconButton(onClick = onEditExercises) {
                Icon(Icons.Default.Edit, "Edit exercises",
                    tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete workout",
                    tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun NewWorkoutDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text("NEW WORKOUT", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Workout name") },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("CREATE", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MaterialTheme.colorScheme.secondary) }
        }
    )
}