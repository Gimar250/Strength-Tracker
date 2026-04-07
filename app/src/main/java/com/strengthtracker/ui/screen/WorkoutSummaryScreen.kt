package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.ui.viewmodel.WorkoutScreenState

@Composable
fun WorkoutSummaryScreen(
    state: WorkoutScreenState.Summary,
    onNotesChanged: (String) -> Unit,
    onSaveAndFinish: () -> Unit
) {
    val completedSets = state.logs.size
    val totalSets = state.exercises.sumOf { it.numberOfSets }
    val exercisesDone = state.logs.map { it.exerciseId }.distinct().size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        contentPadding = PaddingValues(
            start = 24.dp, end = 24.dp, top = 32.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────
        item {
            Column {
                Text(
                    text = "WORKOUT COMPLETE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.workoutName.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Duration + stats ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStat(
                    label = "DURATION",
                    value = formatDuration(state.durationSeconds),
                    modifier = Modifier.weight(1.2f)
                )
                SummaryStat(
                    label = "SETS DONE",
                    value = "$completedSets / $totalSets",
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    label = "EXERCISES",
                    value = "$exercisesDone",
                    modifier = Modifier.weight(0.8f)
                )
            }
        }

        // ── Exercise breakdown header ─────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXERCISE LOG",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Per-exercise cards ────────────────────────────────────────────
        items(state.exercises) { exercise ->
            val logsForExercise = state.logs.filter { it.exerciseId == exercise.id }
            if (logsForExercise.isNotEmpty()) {
                ExerciseSummaryCard(
                    exercise = exercise,
                    logs = logsForExercise
                )
            }
        }

        // ── Notes section ─────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "NOTES",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = {
                        Text(
                            "How did it feel? Any PRs? Things to adjust...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // ── Save button ───────────────────────────────────────────────────
        item {
            Button(
                onClick = onSaveAndFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SAVE & FINISH", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Exercise card ─────────────────────────────────────────────────────────────

@Composable
private fun ExerciseSummaryCard(exercise: Exercise, logs: List<HistoryLog>) {
    val isTimed = exercise.exerciseType == ExerciseType.TIMED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exercise name + type badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exercise.name.uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.background,
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )

            // Set rows
            logs.sortedBy { it.setNumber }.forEach { log ->
                SetLogRow(log = log, isTimed = isTimed)
            }
        }
    }
}

@Composable
private fun SetLogRow(log: HistoryLog, isTimed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${log.setNumber}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Performance value
        val performanceLabel = buildString {
            if (log.weightKg > 0f) {
                val kg = if (log.weightKg % 1 == 0f) log.weightKg.toInt().toString()
                else "%.1f".format(log.weightKg)
                append("${kg}kg")
            }
            val value = if (isTimed) formatSecondsShort(log.reps) else "${log.reps} reps"
            if (log.weightKg > 0f) append(" × ")
            append(value)
        }

        Text(
            text = performanceLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Stat chip ─────────────────────────────────────────────────────────────────

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatSecondsShort(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}