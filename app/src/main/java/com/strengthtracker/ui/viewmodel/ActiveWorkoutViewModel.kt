package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.util.SoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// State model — a sealed class makes all possible screens explicit and safe
// ---------------------------------------------------------------------------

sealed class WorkoutScreenState {
    data object Loading : WorkoutScreenState()

    data class ActiveSet(
        val exercise: Exercise,
        val exerciseIndex: Int,
        val totalExercises: Int,
        val currentSet: Int,       // 1-based for display
        val totalSets: Int,
        val weightKg: String = "", // String to bind directly to TextField
        val reps: String = ""
    ) : WorkoutScreenState()

    data class Resting(
        val exercise: Exercise,
        val exerciseIndex: Int,
        val totalExercises: Int,
        val secondsRemaining: Int,
        val totalSeconds: Int
    ) : WorkoutScreenState()

    data object Finished : WorkoutScreenState()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val workoutId: Long
) : ViewModel() {

    private val _state = MutableStateFlow<WorkoutScreenState>(WorkoutScreenState.Loading)
    val state: StateFlow<WorkoutScreenState> = _state.asStateFlow()

    // In-memory log — saved to DB all at once when the workout finishes
    private val sessionLogs = mutableListOf<HistoryLog>()

    // Flat list of all exercises loaded at session start
    private var exercises: List<Exercise> = emptyList()
    private var currentExerciseIndex = 0
    private var currentSet = 1

    // Handle to the active timer coroutine so we can cancel it if needed
    private var timerJob: Job? = null

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            exercises = repository.getExercisesForWorkout(workoutId)
            if (exercises.isEmpty()) {
                _state.value = WorkoutScreenState.Finished
                return@launch
            }
            emitActiveSetState()
        }
    }

    // ---------------------------------------------------------------------------
    // Public actions — called by the UI
    // ---------------------------------------------------------------------------

    fun onWeightChanged(value: String) {
        val current = _state.value as? WorkoutScreenState.ActiveSet ?: return
        // Only allow numeric input with a single decimal point
        if (value.matches(Regex("^\\d{0,4}(\\.\\d{0,2})?\$"))) {
            _state.update { current.copy(weightKg = value) }
        }
    }

    fun onRepsChanged(value: String) {
        val current = _state.value as? WorkoutScreenState.ActiveSet ?: return
        if (value.matches(Regex("^\\d{0,3}\$"))) {
            _state.update { current.copy(reps = value) }
        }
    }

    fun completeSet() {
        val current = _state.value as? WorkoutScreenState.ActiveSet ?: return
        val exercise = current.exercise

        // Record the set — weight defaults to 0 if left blank
        sessionLogs.add(
            HistoryLog(
                exerciseId = exercise.id,
                workoutId = workoutId,
                setNumber = current.currentSet,
                weightKg = current.weightKg.toFloatOrNull() ?: 0f,
                reps = current.reps.toIntOrNull() ?: 0
            )
        )

        // Transition to rest timer
        startRestTimer(exercise)
    }

    fun skipRest() {
        timerJob?.cancel()
        advanceWorkout()
    }

    // ---------------------------------------------------------------------------
    // Internal state machine
    // ---------------------------------------------------------------------------

    private fun startRestTimer(exercise: Exercise) {
        val restSeconds = exercise.restInSeconds

        _state.value = WorkoutScreenState.Resting(
            exercise = exercise,
            exerciseIndex = currentExerciseIndex,
            totalExercises = exercises.size,
            secondsRemaining = restSeconds,
            totalSeconds = restSeconds
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in restSeconds downTo 1) {
                _state.update {
                    (it as? WorkoutScreenState.Resting)?.copy(secondsRemaining = remaining) ?: it
                }
                delay(1_000)
            }
            // Timer hit zero
            SoundPlayer.playRestEndBeep()
            delay(500) // Brief pause so the beep is heard before screen changes
            advanceWorkout()
        }
    }

    private fun advanceWorkout() {
        val exercise = exercises[currentExerciseIndex]
        val isLastSet = currentSet >= exercise.numberOfSets
        val isLastExercise = currentExerciseIndex >= exercises.size - 1

        when {
            // More sets remaining in current exercise
            !isLastSet -> {
                currentSet++
                emitActiveSetState()
            }
            // Last set done, but more exercises remain
            !isLastExercise -> {
                currentExerciseIndex++
                currentSet = 1
                emitActiveSetState()
            }
            // All exercises and sets done — save and finish
            else -> {
                finishWorkout()
            }
        }
    }

    private fun emitActiveSetState() {
        val exercise = exercises[currentExerciseIndex]

        // Pre-fill weight/reps from the last log entry for this exercise
        // so the user doesn't have to re-enter the same values every set
        val lastLog = sessionLogs
            .filter { it.exerciseId == exercise.id }
            .lastOrNull()

        _state.value = WorkoutScreenState.ActiveSet(
            exercise = exercise,
            exerciseIndex = currentExerciseIndex,
            totalExercises = exercises.size,
            currentSet = currentSet,
            totalSets = exercise.numberOfSets,
            weightKg = lastLog?.weightKg?.let {
                if (it == 0f) "" else it.toString()
            } ?: "",
            reps = lastLog?.reps?.let {
                if (it == 0) "" else it.toString()
            } ?: ""
        )
    }

    private fun finishWorkout() {
        viewModelScope.launch {
            repository.saveWorkoutSession(sessionLogs)
            _state.value = WorkoutScreenState.Finished
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // ---------------------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------------------

    class Factory(
        private val repository: WorkoutRepository,
        private val workoutId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ActiveWorkoutViewModel(repository, workoutId) as T
        }
    }
}
