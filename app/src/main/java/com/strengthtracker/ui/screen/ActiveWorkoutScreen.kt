package com.strengthtracker.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.ActiveWorkoutViewModel
import com.strengthtracker.ui.viewmodel.WorkoutScreenState
import com.strengthtracker.ui.screen.RestTimerScreen

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

    // React to Finished state — navigate back
    LaunchedEffect(state) {
        if (state is WorkoutScreenState.Finished) {
            onWorkoutFinished()
        }
    }

    when (val s = state) {
        is WorkoutScreenState.Loading -> LoadingScreen()
        is WorkoutScreenState.ActiveSet -> ActiveSetScreen(
            state = s,
            onWeightChanged = viewModel::onWeightChanged,
            onRepsChanged = viewModel::onRepsChanged,
            onCompleteSet = viewModel::completeSet
        )
        is WorkoutScreenState.Resting -> RestTimerScreen(
            state = s,
            onSkip = viewModel::skipRest
        )
        is WorkoutScreenState.Finished -> LoadingScreen() // Brief flash before nav
    }
}

// ---------------------------------------------------------------------------
// Loading
// ---------------------------------------------------------------------------

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ---------------------------------------------------------------------------
// Active Set Screen
// ---------------------------------------------------------------------------

@Composable
private fun ActiveSetScreen(
    state: WorkoutScreenState.ActiveSet,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onCompleteSet: () -> Unit
) {
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
            Text(
                text = "EXERCISE ${state.exerciseIndex + 1} / ${state.totalExercises}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.exercise.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            SetProgressRow(
                currentSet = state.currentSet,
                totalSets = state.totalSets
            )

            // ── Target banner — only shown if a target is configured ────────
            val hasTarget = state.exercise.targetWeightKg != null
                    || state.exercise.targetReps != null

            if (hasTarget) {
                Spacer(modifier = Modifier.height(16.dp))
                TargetBanner(exercise = state.exercise)
            }
        }

        // ── Middle: Input fields ────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WorkoutInputField(
                value = state.weightKg,
                onValueChange = onWeightChanged,
                label = "KG",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(weightFocusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            WorkoutInputField(
                value = state.reps,
                onValueChange = onRepsChanged,
                label = "REPS",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // ── Bottom: Complete Set button ─────────────────────────────────────
        Button(
            onClick = onCompleteSet,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .height(72.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "COMPLETE SET",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// Target banner — shows configured goal, dimmed so it doesn't distract
@Composable
private fun TargetBanner(exercise: Exercise) {
    val weightPart = exercise.targetWeightKg?.let {
        val formatted = if (it % 1 == 0f) it.toInt().toString() else it.toString()
        "$formatted kg"
    }
    val repsPart = exercise.targetReps?.let { "$it reps" }

    // Build label: "80 kg × 8 reps", "80 kg", or "8 reps"
    val label = listOfNotNull(weightPart, repsPart).joinToString(" × ")

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TARGET",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Set Progress Row — visual dots showing set progress
// ---------------------------------------------------------------------------

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
        Spacer(modifier = Modifier.width(8.dp))
        repeat(totalSets) { index ->
            val isCompleted = index < currentSet - 1
            val isCurrent = index == currentSet - 1
            Surface(
                modifier = Modifier.size(if (isCurrent) 12.dp else 8.dp),
                shape = MaterialTheme.shapes.small,
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {}
        }
    }
}

// ---------------------------------------------------------------------------
// Shared Input Field
// ---------------------------------------------------------------------------

@Composable
private fun WorkoutInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(80.dp),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            textAlign = TextAlign.Center
        ),
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

