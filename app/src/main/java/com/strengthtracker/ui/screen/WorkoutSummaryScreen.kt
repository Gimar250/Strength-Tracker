package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
