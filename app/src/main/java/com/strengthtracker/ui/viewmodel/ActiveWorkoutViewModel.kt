package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
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
// Progress summary — shown in the overlay sheet
// ---------------------------------------------------------------------------

enum class SetStatus { COMPLETED, SKIPPED, CURRENT, PENDING }

data class SetProgressItem(
    val setNumber: Int,         // 1-based
    val status: SetStatus,
    val weightKg: Float? = null,
    val value: Int? = null      // reps or seconds depending on type
)

data class ExerciseProgressItem(
    val exercise: Exercise,
    val sets: List<SetProgressItem>,
    val isCurrentExercise: Boolean
)

// ---------------------------------------------------------------------------
// Screen states — unchanged from before
// ---------------------------------------------------------------------------

sealed class WorkoutScreenState {
    data object Loading : WorkoutScreenState()
    data class ActiveSet(
        val exercise: Exercise,
        val exerciseIndex: Int,
        val totalExercises: Int,
        val currentSet: Int,
        val totalSets: Int,
        val weightKg: String = "",
        val reps: String = "",
        val stopwatchSeconds: Int = 0,
        val isStopwatchRunning: Boolean = false
    ) : WorkoutScreenState()
    data class Resting(
        val nextExercise: Exercise,
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

    // Sheet visibility — owned here so it survives recomposition
    private val _showProgressSheet = MutableStateFlow(false)
    val showProgressSheet: StateFlow<Boolean> = _showProgressSheet.asStateFlow()

    private val sessionLogs = mutableListOf<HistoryLog>()
    private var exercises: List<Exercise> = emptyList()
    private var currentExerciseIndex = 0
    private var currentSet = 1
    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null

    init { loadWorkout() }

    private fun loadWorkout() {
        viewModelScope.launch {
            exercises = repository.getExercisesForWorkout(workoutId)
            if (exercises.isEmpty()) { _state.value = WorkoutScreenState.Finished; return@launch }
            emitActiveSetState()
        }
    }

    // ---------------------------------------------------------------------------
    // Progress sheet
    // ---------------------------------------------------------------------------

    fun openProgressSheet() { _showProgressSheet.value = true }
    fun closeProgressSheet() { _showProgressSheet.value = false }

    // Computed on demand — called by the UI when the sheet opens
    fun buildProgressItems(): List<ExerciseProgressItem> {
        if (exercises.isEmpty()) return emptyList()

        // Index of the exercise we consider "current" regardless of screen state
        val activeExerciseIndex = when (val s = _state.value) {
            is WorkoutScreenState.ActiveSet -> s.exerciseIndex
            is WorkoutScreenState.Resting -> s.exerciseIndex
            else -> currentExerciseIndex
        }
        val activeSet = when (val s = _state.value) {
            is WorkoutScreenState.ActiveSet -> s.currentSet
            // During rest we have just completed currentSet, so the active
            // set from a display perspective is still currentSet
            else -> currentSet
        }

        return exercises.mapIndexed { exIndex, exercise ->
            val logsForExercise = sessionLogs.filter { it.exerciseId == exercise.id }

            val sets = (1..exercise.numberOfSets).map { setNum ->
                val log = logsForExercise.find { it.setNumber == setNum }
                val status = when {
                    log != null -> SetStatus.COMPLETED
                    exIndex == activeExerciseIndex && setNum == activeSet &&
                            _state.value is WorkoutScreenState.ActiveSet -> SetStatus.CURRENT
                    exIndex < activeExerciseIndex -> SetStatus.SKIPPED
                    exIndex == activeExerciseIndex && setNum < activeSet -> SetStatus.COMPLETED
                    else -> SetStatus.PENDING
                }
                SetProgressItem(
                    setNumber = setNum,
                    status = status,
                    weightKg = log?.weightKg,
                    value = log?.reps
                )
            }

            ExerciseProgressItem(
                exercise = exercise,
                sets = sets,
                isCurrentExercise = exIndex == activeExerciseIndex
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Input handlers
    // ---------------------------------------------------------------------------

    fun onWeightChanged(value: String) {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return
        if (value.matches(Regex("^\\d{0,4}(\\.\\d{0,2})?\$"))) _state.update { s.copy(weightKg = value) }
    }

    fun onRepsChanged(value: String) {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return
        if (value.matches(Regex("^\\d{0,3}\$"))) _state.update { s.copy(reps = value) }
    }

    // ---------------------------------------------------------------------------
    // Stopwatch
    // ---------------------------------------------------------------------------

    fun toggleStopwatch() {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return
        if (s.exercise.exerciseType != ExerciseType.TIMED) return
        if (s.isStopwatchRunning) {
            stopwatchJob?.cancel()
            _state.update { (it as? WorkoutScreenState.ActiveSet)?.copy(isStopwatchRunning = false) ?: it }
        } else {
            _state.update { (it as? WorkoutScreenState.ActiveSet)?.copy(isStopwatchRunning = true) ?: it }
            stopwatchJob = viewModelScope.launch {
                while (true) {
                    delay(1_000)
                    _state.update { st ->
                        (st as? WorkoutScreenState.ActiveSet)?.copy(stopwatchSeconds = st.stopwatchSeconds + 1) ?: st
                    }
                }
            }
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _state.update { s ->
            (s as? WorkoutScreenState.ActiveSet)?.copy(stopwatchSeconds = 0, isStopwatchRunning = false) ?: s
        }
    }

    fun setStopwatchSeconds(seconds: Int) {
        stopwatchJob?.cancel()
        _state.update { s ->
            (s as? WorkoutScreenState.ActiveSet)?.copy(
                stopwatchSeconds = seconds.coerceAtLeast(0),
                isStopwatchRunning = false
            ) ?: s
        }
    }

    // ---------------------------------------------------------------------------
    // Core actions
    // ---------------------------------------------------------------------------

    fun isSetCompletable(): Boolean {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return false
        return when (s.exercise.exerciseType) {
            ExerciseType.REPS -> (s.reps.toIntOrNull() ?: 0) > 0
            ExerciseType.TIMED -> s.stopwatchSeconds > 0
        }
    }

    fun completeSet() {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return
        if (!isSetCompletable()) { skipSet(); return }
        stopwatchJob?.cancel()

        val loggedValue = when (s.exercise.exerciseType) {
            ExerciseType.REPS -> s.reps.toIntOrNull() ?: 0
            ExerciseType.TIMED -> s.stopwatchSeconds
        }

        sessionLogs.add(
            HistoryLog(
                exerciseId = s.exercise.id,
                workoutId = workoutId,
                setNumber = currentSet,
                weightKg = s.weightKg.toFloatOrNull() ?: 0f,
                reps = loggedValue
            )
        )

        val isLastSet = currentSet >= s.exercise.numberOfSets
        val isLastExercise = currentExerciseIndex >= exercises.size - 1
        if (isLastSet && isLastExercise) { finishWorkout(); return }
        startRestTimer(s.exercise)
    }

    fun skipSet() {
        val s = _state.value as? WorkoutScreenState.ActiveSet ?: return
        stopwatchJob?.cancel()
        val isLastSet = currentSet >= s.exercise.numberOfSets
        val isLastExercise = currentExerciseIndex >= exercises.size - 1
        if (isLastSet && isLastExercise) { finishWorkout(); return }
        startRestTimer(s.exercise)
    }

    fun skipRest() {
        timerJob?.cancel()
        advanceWorkout()
    }

    // ---------------------------------------------------------------------------
    // Internal state machine
    // ---------------------------------------------------------------------------

    private fun startRestTimer(currentExercise: Exercise) {
        val nextExercise = if (currentSet < currentExercise.numberOfSets)
            currentExercise
        else exercises[currentExerciseIndex + 1]

        _state.value = WorkoutScreenState.Resting(
            nextExercise = nextExercise,
            exerciseIndex = currentExerciseIndex,
            totalExercises = exercises.size,
            secondsRemaining = currentExercise.restInSeconds,
            totalSeconds = currentExercise.restInSeconds
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in currentExercise.restInSeconds downTo 1) {
                _state.update {
                    (it as? WorkoutScreenState.Resting)?.copy(secondsRemaining = remaining) ?: it
                }
                delay(1_000)
            }
            SoundPlayer.playRestEndBeep()
            delay(500)
            advanceWorkout()
        }
    }

    private fun advanceWorkout() {
        val exercise = exercises[currentExerciseIndex]
        when {
            currentSet < exercise.numberOfSets -> { currentSet++; emitActiveSetState() }
            currentExerciseIndex < exercises.size - 1 -> {
                currentExerciseIndex++; currentSet = 1; emitActiveSetState()
            }
            else -> finishWorkout()
        }
    }

    private fun emitActiveSetState() {
        stopwatchJob?.cancel()
        val exercise = exercises[currentExerciseIndex]
        val lastLog = sessionLogs.filter { it.exerciseId == exercise.id }.lastOrNull()
        _state.value = WorkoutScreenState.ActiveSet(
            exercise = exercise,
            exerciseIndex = currentExerciseIndex,
            totalExercises = exercises.size,
            currentSet = currentSet,
            totalSets = exercise.numberOfSets,
            weightKg = lastLog?.weightKg?.let { if (it == 0f) "" else it.toString() } ?: "",
            reps = if (exercise.exerciseType == ExerciseType.REPS)
                lastLog?.reps?.let { if (it == 0) "" else it.toString() } ?: ""
            else "",
            stopwatchSeconds = 0,
            isStopwatchRunning = false
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
        stopwatchJob?.cancel()
    }

    class Factory(
        private val repository: WorkoutRepository,
        private val workoutId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ActiveWorkoutViewModel(repository, workoutId) as T
    }
}