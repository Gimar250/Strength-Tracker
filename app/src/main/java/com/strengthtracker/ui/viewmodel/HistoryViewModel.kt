package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.db.entity.WorkoutSession
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SessionDisplayItem(
    val workoutId: Long,
    val workoutName: String,
    val timestamp: Long,
    val dateLabel: String,
    val exerciseNames: List<String>,
    val totalSets: Int,
    val durationSeconds: Int? = null,
    val notes: String = ""
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

class HistoryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val shortFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val cutoff14Days = { System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000 }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllLogsFlow(),
        repository.getAllWorkouts(),
        repository.getAllExercisesFlow(),
        repository.getAllSessionsFlow()
    ) { logs, workouts, exercises, sessions ->
        if (logs.isEmpty() && sessions.isEmpty()) {
            HistoryUiState(isLoading = false)
        } else {
            val workoutMap = workouts.associateBy { it.id }
            val exerciseMap = exercises.associateBy { it.id }
            // Index sessions by "workoutId-year-dayOfYear" for O(1) lookup
            val sessionIndex = sessions.associateBy { session ->
                val cal = Calendar.getInstance().apply { timeInMillis = session.startTimestamp }
                "${session.workoutId}-${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            HistoryUiState(
                recentSessions = buildSessions(logs, workoutMap, exerciseMap, sessionIndex)
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

    private fun buildSessions(
        logs: List<HistoryLog>,
        workoutMap: Map<Long, Workout>,
        exerciseMap: Map<Long, Exercise>,
        sessionIndex: Map<String, WorkoutSession>
    ): List<SessionDisplayItem> = logs
        .groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            Triple(log.workoutId, cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
        }
        .map { (key, sessionLogs) ->
            val ts = sessionLogs.maxOf { it.timestamp }
            val lookupKey = "${key.first}-${key.second}-${key.third}"
            val session = sessionIndex[lookupKey]
            SessionDisplayItem(
                workoutId = key.first,
                workoutName = workoutMap[key.first]?.name
                    ?: session?.workoutName
                    ?: "Unknown",
                timestamp = ts,
                dateLabel = formatRelativeDate(ts),
                exerciseNames = sessionLogs
                    .mapNotNull { exerciseMap[it.exerciseId]?.name }
                    .distinct(),
                totalSets = sessionLogs.size,
                durationSeconds = session?.durationSeconds,
                notes = session?.notes ?: ""
            )
        }
        .sortedByDescending { it.timestamp }

    private fun buildExerciseSummaries(
        logs: List<HistoryLog>,
        workoutMap: Map<Long, Workout>,
        exerciseMap: Map<Long, Exercise>
    ): List<ExerciseDisplayItem> = logs
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, exerciseLogs) ->
            val exercise = exerciseMap[exerciseId] ?: return@mapNotNull null
            val lastTs = exerciseLogs.maxOf { it.timestamp }
            val sessions = exerciseLogs.groupBy { log ->
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