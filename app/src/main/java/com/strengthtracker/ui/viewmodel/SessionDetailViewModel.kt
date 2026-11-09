package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SessionDetailUiState(
    val workoutName: String = "",
    val dateLabel: String = "",
    val durationSeconds: Int? = null,
    val notes: String = "",
    val completedSets: Int = 0,
    val totalSets: Int = 0,
    val exercises: List<Exercise> = emptyList(),
    val logsByExercise: Map<Long, List<HistoryLog>> = emptyMap(),
    val isLoading: Boolean = true
)

// Shows the detail of one session as it appears in the "last 14 days" list.
// The list items are keyed by (workoutId, day), so this view shows all logs
// of that workout recorded on that calendar day, plus the latest WorkoutSession
// row of that day for duration and notes.
class SessionDetailViewModel(
    private val repository: WorkoutRepository,
    private val workoutId: Long,
    private val sessionTimestamp: Long
) : ViewModel() {

    private val fullFmt = SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault())

    private val _state = MutableStateFlow(SessionDetailUiState())
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = sessionTimestamp }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24L * 60 * 60 * 1000

            val logs = repository.getLogsForWorkoutInRange(workoutId, dayStart, dayEnd)
            val exercises = repository.getExercisesForWorkout(workoutId)
            val workoutName = repository.getWorkoutById(workoutId)?.name ?: ""
            val session = repository.getSessionsForWorkoutOnce(workoutId)
                .filter { it.startTimestamp in dayStart until dayEnd }
                .maxByOrNull { it.startTimestamp }

            _state.value = SessionDetailUiState(
                workoutName = workoutName,
                dateLabel = fullFmt.format(Date(sessionTimestamp)),
                durationSeconds = session?.durationSeconds,
                notes = session?.notes ?: "",
                completedSets = logs.size,
                totalSets = exercises.sumOf { it.numberOfSets },
                exercises = exercises,
                logsByExercise = logs.groupBy { it.exerciseId },
                isLoading = false
            )
        }
    }

    class Factory(
        private val repository: WorkoutRepository,
        private val workoutId: Long,
        private val sessionTimestamp: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionDetailViewModel(repository, workoutId, sessionTimestamp) as T
    }
}
