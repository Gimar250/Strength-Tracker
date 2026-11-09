package com.strengthtracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.viewmodel.SessionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    repository: WorkoutRepository,
    workoutId: Long,
    sessionTimestamp: Long,
    onBack: () -> Unit
) {
    val viewModel: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModel.Factory(repository, workoutId, sessionTimestamp)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                    Column {
                        Text(
                            text = state.workoutName.uppercase(),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = state.dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Duration + stats ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.durationSeconds?.let { dur ->
                        SummaryStat(
                            label = "DURATION",
                            value = formatDuration(dur),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    SummaryStat(
                        label = "SETS DONE",
                        value = "${state.completedSets} / ${state.totalSets}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Notes (only if present) ────────────────────────────────────
            if (state.notes.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "NOTES",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = state.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // ── Exercise log header ───────────────────────────────────────
            item {
                Text(
                    text = "EXERCISE LOG",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // ── Per-exercise cards ────────────────────────────────────────
            items(state.exercises) { exercise ->
                val logsForExercise = state.logsByExercise[exercise.id]
                if (logsForExercise != null) {
                    ExerciseSummaryCard(
                        exercise = exercise,
                        logs = logsForExercise
                    )
                }
            }
        }
    }
}
