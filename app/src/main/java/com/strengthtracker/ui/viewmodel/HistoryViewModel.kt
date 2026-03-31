package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Display models
// ---------------------------------------------------------------------------

data class SessionDisplayItem(
    val workoutId: Long,
    val workoutName: String,
    val timestamp: Long,
    val dateLabel: String,
    val exerciseNames: List<String>,
    val totalSets: Int
)

data class ExerciseDisplayItem(
    val exerciseId: Long,
    val exerciseName: String,
    val workoutName: String,
    val exerciseType: ExerciseType,
    val lastPerformed: Long,
    val lastPerformedLabel: String,
    val totalSessions: Int
)

data class HistoryUiState(
    val recentSessions: List<SessionDisplayItem> = emptyList(),
    val exerciseSummaries: List<ExerciseDisplayItem> = emptyList(),
    val isLoading: Boolean = true
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class HistoryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val shortFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val cutoff14Days = { System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000 }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllLogsFlow(),
        repository.getAllWorkouts(),
        repository.getAllExercisesFlow()
    ) { logs, workouts, exercises ->
        if (logs.isEmpty()) {
            HistoryUiState(isLoading = false)
        } else {
            val workoutMap = workouts.associateBy { it.id }
            val exerciseMap = exercises.associateBy { it.id }
            HistoryUiState(
                recentSessions = buildSessions(logs, workoutMap, exerciseMap)
                    .filter { it.timestamp >= cutoff14Days() },
                exerciseSummaries = buildExerciseSummaries(logs, workoutMap, exerciseMap),
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    // Groups raw logs into sessions by (workoutId, calendar day)
    private fun buildSessions(
        logs: List<HistoryLog>,
        workoutMap: Map<Long, Workout>,
        exerciseMap: Map<Long, Exercise>
    ): List<SessionDisplayItem> = logs
        .groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            Triple(log.workoutId, cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
        }
        .map { (key, sessionLogs) ->
            val ts = sessionLogs.maxOf { it.timestamp }
            SessionDisplayItem(
                workoutId = key.first,
                workoutName = workoutMap[key.first]?.name ?: "Unknown",
                timestamp = ts,
                dateLabel = formatRelativeDate(ts),
                exerciseNames = sessionLogs.mapNotNull { exerciseMap[it.exerciseId]?.name }.distinct(),
                totalSets = sessionLogs.size
            )
        }
        .sortedByDescending { it.timestamp }

    // One entry per exercise that has at least one log
    private fun buildExerciseSummaries(
        logs: List<HistoryLog>,
        workoutMap: Map<Long, Workout>,
        exerciseMap: Map<Long, Exercise>
    ): List<ExerciseDisplayItem> = logs
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, exerciseLogs) ->
            val exercise = exerciseMap[exerciseId] ?: return@mapNotNull null
            val lastTs = exerciseLogs.maxOf { it.timestamp }
            val sessions = exerciseLogs
                .groupBy { log ->
                    val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }.size
            ExerciseDisplayItem(
                exerciseId = exerciseId,
                exerciseName = exercise.name,
                workoutName = workoutMap[exercise.workoutId]?.name ?: "Unknown",
                exerciseType = exercise.exerciseType,
                lastPerformed = lastTs,
                lastPerformedLabel = formatRelativeDate(lastTs),
                totalSessions = sessions
            )
        }
        .sortedByDescending { it.lastPerformed }

    private fun formatRelativeDate(ts: Long): String {
        val session = Calendar.getInstance().apply { timeInMillis = ts }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            isSameDay(session, today) -> "Today"
            isSameDay(session, yesterday) -> "Yesterday"
            else -> shortFmt.format(Date(ts))
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}