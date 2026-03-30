package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.ActiveWorkoutViewModel
import com.strengthtracker.ui.viewmodel.WorkoutScreenState

@Composable
fun ActiveWorkoutScreen(
    repository: WorkoutRepository,
    workoutId: Long,
    onWorkoutFinished: () -> Unit
) {
    val viewModel: ActiveWorkoutViewModel = viewModel(
        factory = ActiveWorkoutViewModel.Factory(repository, workoutId)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is WorkoutScreenState.Finished) onWorkoutFinished()
    }

    when (val s = state) {
        is WorkoutScreenState.Loading -> LoadingScreen()
        is WorkoutScreenState.ActiveSet -> ActiveSetScreen(
            state = s,
            onWeightChanged = viewModel::onWeightChanged,
            onRepsChanged = viewModel::onRepsChanged,
            onCompleteSet = viewModel::completeSet,
            onSkipSet = viewModel::skipSet,
            onToggleStopwatch = viewModel::toggleStopwatch,
            onResetStopwatch = viewModel::resetStopwatch,
            onSetStopwatchSeconds = viewModel::setStopwatchSeconds,
            isSetCompletable = viewModel.isSetCompletable()
        )
        is WorkoutScreenState.Resting -> RestTimerScreen(state = s, onSkip = viewModel::skipRest)
        is WorkoutScreenState.Finished -> LoadingScreen()
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ActiveSetScreen(
    state: WorkoutScreenState.ActiveSet,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onCompleteSet: () -> Unit,
    onSkipSet: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onSetStopwatchSeconds: (Int) -> Unit,
    isSetCompletable: Boolean
) {
    val isTimed = state.exercise.exerciseType == ExerciseType.TIMED
    val weightFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Top: Exercise info ──────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EXERCISE ${state.exerciseIndex + 1} / ${state.totalExercises}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = if (isTimed) "TIMED" else "REPS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.exercise.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))
            SetProgressRow(currentSet = state.currentSet, totalSets = state.totalSets)

            val hasTarget = state.exercise.targetWeightKg != null || state.exercise.targetReps != null
            if (hasTarget) {
                Spacer(Modifier.height(16.dp))
                TargetBanner(exercise = state.exercise)
            }
        }

        // ── Middle: Inputs ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WorkoutInputField(
                value = state.weightKg,
                onValueChange = onWeightChanged,
                label = "KG",
                modifier = Modifier.fillMaxWidth().focusRequester(weightFocusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (isTimed) {
                TimedSetControls(
                    seconds = state.stopwatchSeconds,
                    isRunning = state.isStopwatchRunning,
                    onToggle = onToggleStopwatch,
                    onReset = onResetStopwatch,
                    onEditSeconds = onSetStopwatchSeconds
                )
            } else {
                WorkoutInputField(
                    value = state.reps,
                    onValueChange = onRepsChanged,
                    label = "REPS",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // ── Bottom: Complete / Skip Set ─────────────────────────────────────
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            if (isSetCompletable) {
                Button(
                    onClick = onCompleteSet,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("COMPLETE SET", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                OutlinedButton(
                    onClick = onSkipSet,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outline)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("SKIP SET", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Timed set controls ──────────────────────────────────────────────────────

@Composable
private fun TimedSetControls(
    seconds: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onEditSeconds: (Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val minutes = seconds / 60
    val secs = seconds % 60
    val display = if (minutes > 0) "%d:%02d".format(minutes, secs) else "${secs}s"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Time display
            Text(
                text = display,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset
                OutlinedButton(
                    onClick = onReset,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outline)
                    )
                ) {
                    Text("RESET", style = MaterialTheme.typography.labelLarge)
                }

                // Start / Pause
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        contentColor = if (isRunning) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (isRunning) "PAUSE" else "START",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Edit — only available when stopped
                if (!isRunning) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(MaterialTheme.colorScheme.outline)
                        )
                    ) {
                        Text("EDIT", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        TimerEditDialog(
            currentSeconds = seconds,
            onConfirm = { newSeconds ->
                onEditSeconds(newSeconds)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun TimerEditDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentSeconds.toString()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val parsed = input.toIntOrNull()
    val isValid = parsed != null && parsed >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                "EDIT TIME",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter total seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) input = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    suffix = { Text("sec", color = MaterialTheme.colorScheme.secondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (isValid) onConfirm(parsed!!) },
                enabled = isValid
            ) {
                Text("SET", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}

// ── Shared composables ──────────────────────────────────────────────────────

@Composable
private fun SetProgressRow(currentSet: Int, totalSets: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SET $currentSet / $totalSets",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(8.dp))
        repeat(totalSets) { index ->
            val isCurrent = index == currentSet - 1
            val isCompleted = index < currentSet - 1
            Surface(
                modifier = Modifier.size(if (isCurrent) 12.dp else 8.dp),
                shape = MaterialTheme.shapes.small,
                color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }
    }
}

@Composable
private fun WorkoutInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.height(80.dp),
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = keyboardOptions,
        singleLine = true,
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
}

@Composable
private fun TargetBanner(exercise: Exercise) {
    val isTimed = exercise.exerciseType == ExerciseType.TIMED
    val weightPart = exercise.targetWeightKg?.let {
        "${if (it % 1 == 0f) it.toInt().toString() else it.toString()} kg"
    }
    val secondaryPart = exercise.targetReps?.let { if (isTimed) "${it}s" else "$it reps" }
    val label = listOfNotNull(weightPart, secondaryPart).joinToString(" × ")

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TARGET", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}