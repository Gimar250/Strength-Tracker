package com.strengthtracker.ui.screen

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
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
                            Icons.Default.ArrowBack,
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
                modifier = Modifier.fillMaxSize().padding(padding),
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.exercises, key = { _, e -> e.id }) { index, exercise ->
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

    // ── Bottom Sheet ────────────────────────────────────────────────────────
    val currentSheet = state.sheetState
    if (currentSheet != ExerciseSheetState.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp).size(width = 40.dp, height = 4.dp),
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.extraLarge
                ) {}
            }
        ) {
            val existingExercise = (currentSheet as? ExerciseSheetState.Editing)?.exercise
            ExerciseEditSheet(
                existing = existingExercise,
                onSave = { name, sets, rest, targetWeight, targetReps, type ->
                    viewModel.saveExercise(name, sets, rest, targetWeight, targetReps, type, existingExercise)
                },
                onDismiss = { viewModel.dismissSheet() }
            )
        }
    }
}

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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Type badge
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = if (exercise.exerciseType == ExerciseType.TIMED) "TIMED" else "REPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val targetLabel = buildString {
                    append("${exercise.numberOfSets} sets  •  ${exercise.restInSeconds}s rest")
                    val tw = exercise.targetWeightKg?.let {
                        if (it % 1 == 0f) it.toInt().toString() else it.toString()
                    }
                    val tr = exercise.targetReps?.let {
                        if (exercise.exerciseType == ExerciseType.TIMED) "${it}s" else "${it} reps"
                    }
                    val target = listOfNotNull(tw?.let { "$it kg" }, tr).joinToString(" × ")
                    if (target.isNotEmpty()) append("  •  $target")
                }
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, "Move up",
                        tint = if (isFirst) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, "Move down",
                        tint = if (isLast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit exercise", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, "Delete exercise", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Text("Delete Exercise?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "\"${exercise.name}\" will be permanently removed from this routine.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("DELETE", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}

@Composable
private fun ExerciseEditSheet(
    existing: Exercise?,
    onSave: (name: String, sets: Int, rest: Int, targetWeight: Float?, targetReps: Int?, type: ExerciseType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var sets by remember { mutableStateOf(existing?.numberOfSets?.toString() ?: "3") }
    var rest by remember { mutableStateOf(existing?.restInSeconds?.toString() ?: "90") }
    var targetWeight by remember {
        mutableStateOf(existing?.targetWeightKg?.let {
            if (it % 1 == 0f) it.toInt().toString() else it.toString()
        } ?: "")
    }
    var targetReps by remember { mutableStateOf(existing?.targetReps?.toString() ?: "") }
    var exerciseType by remember { mutableStateOf(existing?.exerciseType ?: ExerciseType.REPS) }

    val focusRequester = remember { FocusRequester() }
    val isValid = name.isNotBlank() && (sets.toIntOrNull() ?: 0) > 0

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
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = sheetTextFieldColors()
        )

        // Exercise type toggle
        Text(
            text = "TYPE",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExerciseType.entries.forEach { type ->
                val isSelected = exerciseType == type
                if (isSelected) {
                    Button(
                        onClick = { exerciseType = type },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(type.name, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = { exerciseType = type },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.outline
                            )
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(type.name, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                colors = sheetTextFieldColors()
            )
            OutlinedTextField(
                value = rest,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) rest = it },
                label = { Text("Rest (sec)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                colors = sheetTextFieldColors()
            )
        }

        // Target section
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = "TARGET (optional)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { if (it.matches(Regex("^\\d{0,4}(\\.\\d{0,2})?\$"))) targetWeight = it },
                label = { Text("Target KG") },
                placeholder = { Text("—", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                colors = sheetTextFieldColors()
            )
            OutlinedTextField(
                value = targetReps,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) targetReps = it },
                // Label changes based on type
                label = { Text(if (exerciseType == ExerciseType.TIMED) "Target Secs" else "Target Reps") },
                placeholder = { Text("—", color = MaterialTheme.colorScheme.outline) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
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
                    targetReps.toIntOrNull(),
                    exerciseType
                )
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth().height(64.dp),
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