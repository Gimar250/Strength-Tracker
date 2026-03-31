package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ---------------------------------------------------------------------------
// Display models
// ---------------------------------------------------------------------------

data class ProgressPoint(
    val timestamp: Long,
    val dateLabel: String,
    val maxWeight: Float,
    val maxValue: Int,   // max reps or max seconds depending on type
    val totalSets: Int
)

data class ExerciseStats(
    val bestWeight: Float?,       // null if all sets were bodyweight (0kg)
    val bestValue: Int,           // best reps or best seconds
    val totalSets: Int,
    val totalSessions: Int
)

data class ExerciseHistoryUiState(
    val exercise: Exercise? = null,
    val workoutName: String = "",
    val progressPoints: List<ProgressPoint> = emptyList(),
    val stats: ExerciseStats? = null,
    val allLogs: List<HistoryLog> = emptyList(),
    val isLoading: Boolean = true
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class ExerciseHistoryViewModel(
    private val repository: WorkoutRepository,
    private val exerciseId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseHistoryUiState())
    val state: StateFlow<ExerciseHistoryUiState> = _state.asStateFlow()

    private val shortFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    private val MAX_CHART_POINTS = 15

    init {
        viewModelScope.launch {
            // One-time fetch for exercise metadata
            val exercise = repository.getExerciseById(exerciseId)
            val workoutName = exercise?.let { repository.getWorkoutById(it.workoutId)?.name } ?: ""

            // Observe logs reactively
            repository.getLogsForExercise(exerciseId).collect { logs ->
                _state.value = ExerciseHistoryUiState(
                    exercise = exercise,
                    workoutName = workoutName,
                    progressPoints = buildProgressPoints(logs, exercise?.exerciseType),
                    stats = buildStats(logs),
                    allLogs = logs,
                    isLoading = false
                )
            }
        }
    }

    // Groups logs by calendar day, takes the most recent MAX_CHART_POINTS sessions
    private fun buildProgressPoints(
        logs: List<HistoryLog>,
        type: ExerciseType?
    ): List<ProgressPoint> = logs
        .groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }
        .map { (_, dayLogs) ->
            val ts = dayLogs.maxOf { it.timestamp }
            ProgressPoint(
                timestamp = ts,
                dateLabel = shortFmt.format(java.util.Date(ts)),
                maxWeight = dayLogs.maxOf { it.weightKg },
                maxValue = dayLogs.maxOf { it.reps },
                totalSets = dayLogs.size
            )
        }
        .sortedBy { it.timestamp }
        .takeLast(MAX_CHART_POINTS)

    private fun buildStats(logs: List<HistoryLog>): ExerciseStats {
        val sessions = logs.groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.size
        return ExerciseStats(
            bestWeight = logs.mapNotNull { it.weightKg.takeIf { w -> w > 0f } }.maxOrNull(),
            bestValue = logs.maxOfOrNull { it.reps } ?: 0,
            totalSets = logs.size,
            totalSessions = sessions
        )
    }

    class Factory(
        private val repository: WorkoutRepository,
        private val exerciseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExerciseHistoryViewModel(repository, exerciseId) as T
    }
}