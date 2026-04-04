package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.ActiveWorkoutViewModel
import com.strengthtracker.ui.viewmodel.ExerciseProgressItem
import com.strengthtracker.ui.viewmodel.SetStatus
import com.strengthtracker.ui.viewmodel.WorkoutScreenState

@OptIn(ExperimentalMaterial3Api::class)
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
    val showSheet by viewModel.showProgressSheet.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is WorkoutScreenState.Finished) onWorkoutFinished()
    }

    // Build progress items when sheet is open — recalculated on each open
    val progressItems = remember(showSheet, state) {
        if (showSheet) viewModel.buildProgressItems() else emptyList()
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
            isSetCompletable = viewModel.isSetCompletable(),
            onOpenProgress = viewModel::openProgressSheet
        )
        is WorkoutScreenState.Resting -> RestTimerScreen(
            state = s,
            onSkip = viewModel::skipRest,
            onOpenProgress = viewModel::openProgressSheet
        )
        is WorkoutScreenState.Finished -> LoadingScreen()
    }

    // ── Progress overview sheet ─────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeProgressSheet,
            sheetState = rememberModalBottomSheetState(),
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
            WorkoutProgressSheet(items = progressItems)
        }
    }
}

// ---------------------------------------------------------------------------
// Progress Sheet content
// ---------------------------------------------------------------------------

@Composable
private fun WorkoutProgressSheet(items: List<ExerciseProgressItem>) {
    val completedExercises = items.count { ex ->
        ex.sets.all { it.status == SetStatus.COMPLETED || it.status == SetStatus.SKIPPED }
    }
    val remainingExercises = items.size - completedExercises
    val completedSets = items.sumOf { ex -> ex.sets.count { it.status == SetStatus.COMPLETED } }
    val totalSets = items.sumOf { ex -> ex.sets.size }
    val remainingSets = totalSets - completedSets

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WORKOUT PROGRESS",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        // ── Summary row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressSummaryChip(
                label = "Sets done",
                value = "$completedSets / $totalSets",
                modifier = Modifier.weight(1f)
            )
            ProgressSummaryChip(
                label = "Remaining",
                value = "$remainingSets sets",
                modifier = Modifier.weight(1f)
            )
            ProgressSummaryChip(
                label = "Exercises left",
                value = "$remainingExercises",
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

        // ── Exercise list ────────────────────────────────────────────────────
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 480.dp)
        ) {
            items(items) { item ->
                ExerciseProgressRow(item = item)
            }
        }
    }
}

@Composable
private fun ProgressSummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExerciseProgressRow(item: ExerciseProgressItem) {
    val isTimed = item.exercise.exerciseType == ExerciseType.TIMED
    val allDone = item.sets.all {
        it.status == SetStatus.COMPLETED || it.status == SetStatus.SKIPPED
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = when {
            item.isCurrentExercise -> MaterialTheme.colorScheme.surfaceVariant
            allDone -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.background
        },
        shape = MaterialTheme.shapes.medium,
        border = if (item.isCurrentExercise) ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(MaterialTheme.colorScheme.primary)
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Exercise header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.exercise.name.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (allDone) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isCurrentExercise) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                val doneCount = item.sets.count {
                    it.status == SetStatus.COMPLETED || it.status == SetStatus.SKIPPED
                }
                Text(
                    text = "$doneCount / ${item.sets.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Set dots row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.sets.forEach { set ->
                    SetDot(set = set, isTimed = isTimed)
                }
            }
        }
    }
}

@Composable
private fun SetDot(set: com.strengthtracker.ui.viewmodel.SetProgressItem, isTimed: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Dot with status styling
        Box(
            modifier = Modifier
                .size(28.dp)
                .drawBehind {
                    when (set.status) {
                        SetStatus.COMPLETED -> {
                            // Filled circle
                            drawCircle(color = primary)
                        }
                        SetStatus.CURRENT -> {
                            // Outlined pulsing circle
                            drawCircle(
                                color = primary,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }
                        SetStatus.SKIPPED -> {
                            // Dimmed filled circle
                            drawCircle(color = secondary)
                        }
                        SetStatus.PENDING -> {
                            // Empty circle
                            drawCircle(
                                color = surface,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (set.status) {
                    SetStatus.COMPLETED, SetStatus.SKIPPED -> onSurface
                    SetStatus.CURRENT -> primary
                    SetStatus.PENDING -> secondary
                }
            )
        }

        // Logged value under the dot — only for completed sets
        if (set.status == SetStatus.COMPLETED && set.value != null) {
            val label = if (isTimed) {
                val m = (set.value) / 60
                val s = (set.value) % 60
                if (m > 0) "${m}:${"%02d".format(s)}" else "${s}s"
            } else {
                "${set.value}"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.secondary
            )
            set.weightKg?.takeIf { it > 0f }?.let { kg ->
                Text(
                    text = if (kg % 1 == 0f) "${kg.toInt()}kg" else "%.1fkg".format(kg),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
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
// Active Set Screen — now with progress button
// ---------------------------------------------------------------------------

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
    isSetCompletable: Boolean,
    onOpenProgress: () -> Unit
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
        // ── Top: Exercise info + progress button ────────────────────────────
        Column(modifier = Modifier.padding(top = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EXERCISE ${state.exerciseIndex + 1} / ${state.totalExercises}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(8.dp))
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
                // Progress overview button
                IconButton(onClick = onOpenProgress) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "Workout progress",
                        tint = MaterialTheme.colorScheme.secondary
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

        // ── Bottom: Complete / Skip ─────────────────────────────────────────
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

// ---------------------------------------------------------------------------
// Rest Timer Screen — now with progress button
// ---------------------------------------------------------------------------

@Composable
fun RestTimerScreen(
    state: WorkoutScreenState.Resting,
    onSkip: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val progress = state.secondsRemaining.toFloat() / state.totalSeconds.toFloat()
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 800,
            easing = androidx.compose.animation.core.LinearEasing
        ),
        label = "timer_arc"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Top: label + progress button ────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(48.dp)) // balance the icon on the right
                Text(
                    text = "REST",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = onOpenProgress) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "Workout progress",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Next: ${state.nextExercise.name.uppercase()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Middle: arc countdown ───────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .drawBehind {
                    val strokeWidth = 16.dp.toPx()
                    val inset = strokeWidth / 2
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = surfaceColor, startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor, startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
        ) {
            Text(
                text = state.secondsRemaining.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        // ── Bottom: Skip ────────────────────────────────────────────────────
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).height(64.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.secondary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(MaterialTheme.colorScheme.outline)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("SKIP REST", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared composables
// ---------------------------------------------------------------------------

@Composable
private fun SetProgressRow(currentSet: Int, totalSets: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "SET $currentSet / $totalSets",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.width(8.dp))
        repeat(totalSets) { index ->
            Surface(
                modifier = Modifier.size(if (index == currentSet - 1) 12.dp else 8.dp),
                shape = MaterialTheme.shapes.small,
                color = if (index <= currentSet - 1) MaterialTheme.colorScheme.primary
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
            Text(display, style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outline))
                ) { Text("RESET", style = MaterialTheme.typography.labelLarge) }
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary,
                        contentColor = if (isRunning) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text(if (isRunning) "PAUSE" else "START", style = MaterialTheme.typography.labelLarge) }
                if (!isRunning) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(MaterialTheme.colorScheme.outline))
                    ) { Text("EDIT", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }
    }

    if (showEditDialog) {
        TimerEditDialog(
            currentSeconds = seconds,
            onConfirm = { onEditSeconds(it); showEditDialog = false },
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
        title = { Text("EDIT TIME", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter total seconds", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary)
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
            TextButton(onClick = { if (isValid) onConfirm(parsed!!) }, enabled = isValid) {
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

private val androidx.compose.ui.unit.TextUnit.sp get() = this