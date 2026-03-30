package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.EditRoutineViewModel
import com.strengthtracker.ui.viewmodel.ExerciseSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineScreen(
    repository: WorkoutRepository,
    workoutId: Long,
    onBack: () -> Unit
) {
    val viewModel: EditRoutineViewModel = viewModel(
        factory = EditRoutineViewModel.Factory(repository, workoutId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Bottom sheet for add/edit exercise
    val sheetState = rememberModalBottomSheetState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Inline editable workout name
                    OutlinedTextField(
                        value = state.workoutNameInput,
                        onValueChange = viewModel::onWorkoutNameChanged,
                        textStyle = MaterialTheme.typography.titleLarge,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.saveWorkoutName()
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveWorkoutName()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddExerciseSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exercises yet.\nTap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    // Extra bottom padding so FAB doesn't cover last item
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = state.exercises,
                    key = { _, exercise -> exercise.id }
                ) { index, exercise ->
                    ExerciseEditCard(
                        exercise = exercise,
                        isFirst = index == 0,
                        isLast = index == state.exercises.lastIndex,
                        onMoveUp = { viewModel.moveExerciseUp(exercise) },
                        onMoveDown = { viewModel.moveExerciseDown(exercise) },
                        onEdit = { viewModel.openEditExerciseSheet(exercise) },
                        onDelete = { viewModel.deleteExercise(exercise) }
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Add / Edit Exercise Bottom Sheet
    // ---------------------------------------------------------------------------

    val currentSheet = state.sheetState
    if (currentSheet != ExerciseSheetState.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp),
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.extraLarge
                ) {}
            }
        ) {
            val existingExercise =
                (currentSheet as? ExerciseSheetState.Editing)?.exercise

            ExerciseEditSheet(
                existing = existingExercise,
                onSave = { name, sets, rest, targetWeight, targetReps ->
                    viewModel.saveExercise(name, sets, rest, targetWeight, targetReps, existingExercise)
                },
                onDismiss = { viewModel.dismissSheet() }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Exercise card — shows name, sets, rest, and controls
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseEditCard(
    exercise: Exercise,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Exercise info ───────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${exercise.numberOfSets} sets  •  ${exercise.restInSeconds}s rest",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // ── Order controls ──────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (isFirst)
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (isLast)
                            MaterialTheme.colorScheme.outline
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Edit button ─────────────────────────────────────────────────
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Add, // will swap below
                    contentDescription = "Edit exercise",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Delete button ───────────────────────────────────────────────
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete exercise",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text(
                    text = "Delete Exercise?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "\"${exercise.name}\" will be permanently removed from this routine.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        text = "DELETE",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(
                        text = "CANCEL",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Add / Edit Exercise Sheet content
// ---------------------------------------------------------------------------

@Composable
private fun ExerciseEditSheet(
    existing: Exercise?,
    onSave: (name: String, sets: Int, rest: Int, targetWeight: Float?, targetReps: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var name        by remember { mutableStateOf(existing?.name ?: "") }
    var sets        by remember { mutableStateOf(existing?.numberOfSets?.toString() ?: "3") }
    var rest        by remember { mutableStateOf(existing?.restInSeconds?.toString() ?: "90") }
    // Empty string means "no target set"
    var targetWeight by remember {
        mutableStateOf(existing?.targetWeightKg?.let {
            if (it % 1 == 0f) it.toInt().toString() else it.toString()
        } ?: "")
    }
    var targetReps  by remember { mutableStateOf(existing?.targetReps?.toString() ?: "") }

    val focusRequester = remember { FocusRequester() }
    val isValid = name.isNotBlank()
            && (sets.toIntOrNull() ?: 0) > 0
            && (rest.toIntOrNull() ?: 0) >= 0

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .windowInsetsPadding(WindowInsets.ime),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (existing != null) "EDIT EXERCISE" else "NEW EXERCISE",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Exercise name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise Name") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = sheetTextFieldColors()
        )

        // Sets and Rest
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = sets,
                onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) sets = it },
                label = { Text("Sets") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                colors = sheetTextFieldColors()
            )
            OutlinedTextField(
                value = rest,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) rest = it },
                label = { Text("Rest (sec)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                colors = sheetTextFieldColors()
            )
        }

        // Divider + target section label
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = "TARGET (optional)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        // Target weight and reps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = targetWeight,
                onValueChange = {
                    if (it.matches(Regex("^\\d{0,4}(\\.\\d{0,2})?\$"))) targetWeight = it
                },
                label = { Text("Target KG") },
                placeholder = { Text("—", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                colors = sheetTextFieldColors()
            )
            OutlinedTextField(
                value = targetReps,
                onValueChange = {
                    if (it.length <= 3 && it.all(Char::isDigit)) targetReps = it
                },
                label = { Text("Target Reps") },
                placeholder = { Text("—", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                colors = sheetTextFieldColors()
            )
        }

        Button(
            onClick = {
                onSave(
                    name,
                    sets.toIntOrNull() ?: 3,
                    rest.toIntOrNull() ?: 90,
                    targetWeight.toFloatOrNull(),
                    targetReps.toIntOrNull()
                )
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.secondary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (existing != null) "SAVE CHANGES" else "ADD EXERCISE",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// Shared colors for sheet text fields
@Composable
private fun sheetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary
)