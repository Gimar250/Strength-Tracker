package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Sheet state — controls whether the add/edit bottom sheet is visible
// ---------------------------------------------------------------------------

sealed class ExerciseSheetState {
    data object Hidden : ExerciseSheetState()
    data object AddNew : ExerciseSheetState()
    data class Editing(val exercise: Exercise) : ExerciseSheetState()
}

// ---------------------------------------------------------------------------
// Screen state
// ---------------------------------------------------------------------------

data class EditRoutineState(
    val workout: Workout? = null,
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val workoutNameInput: String = "",
    val sheetState: ExerciseSheetState = ExerciseSheetState.Hidden
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class EditRoutineViewModel(
    private val repository: WorkoutRepository,
    private val workoutId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(EditRoutineState())
    val state: StateFlow<EditRoutineState> = _state.asStateFlow()

    init {
        observeRoutine()
    }

    // Observe exercises via Flow so the list reacts to any DB change instantly
    private fun observeRoutine() {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId) ?: return@launch
            _state.update {
                it.copy(
                    workout = workout,
                    workoutNameInput = workout.name,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            repository.getExercisesForWorkoutFlow(workoutId).collect { exercises ->
                _state.update { it.copy(exercises = exercises) }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Workout name
    // ---------------------------------------------------------------------------

    fun onWorkoutNameChanged(name: String) {
        _state.update { it.copy(workoutNameInput = name) }
    }

    fun saveWorkoutName() {
        val current = _state.value
        val workout = current.workout ?: return
        val trimmed = current.workoutNameInput.trim()
        if (trimmed.isBlank() || trimmed == workout.name) return

        viewModelScope.launch {
            repository.updateWorkout(workout.copy(name = trimmed))
            _state.update { it.copy(workout = workout.copy(name = trimmed)) }
        }
    }

    // ---------------------------------------------------------------------------
    // Exercise ordering — simple up/down, one-hand friendly
    // ---------------------------------------------------------------------------

    fun moveExerciseUp(exercise: Exercise) {
        val list = _state.value.exercises.toMutableList()
        val index = list.indexOf(exercise)
        if (index <= 0) return
        list.removeAt(index)
        list.add(index - 1, exercise)
        persistOrder(list)
    }

    fun moveExerciseDown(exercise: Exercise) {
        val list = _state.value.exercises.toMutableList()
        val index = list.indexOf(exercise)
        if (index < 0 || index >= list.size - 1) return
        list.removeAt(index)
        list.add(index + 1, exercise)
        persistOrder(list)
    }

    private fun persistOrder(reordered: List<Exercise>) {
        // Optimistically update UI first for instant feedback
        _state.update { it.copy(exercises = reordered) }
        viewModelScope.launch {
            repository.updateExerciseOrder(reordered)
        }
    }

    // ---------------------------------------------------------------------------
    // Exercise CRUD
    // ---------------------------------------------------------------------------

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            // Flow will automatically update the list
        }
    }

    fun openAddExerciseSheet() {
        _state.update { it.copy(sheetState = ExerciseSheetState.AddNew) }
    }

    fun openEditExerciseSheet(exercise: Exercise) {
        _state.update { it.copy(sheetState = ExerciseSheetState.Editing(exercise)) }
    }

    fun dismissSheet() {
        _state.update { it.copy(sheetState = ExerciseSheetState.Hidden) }
    }

    fun saveExercise(
        name: String,
        sets: Int,
        restSeconds: Int,
        existing: Exercise?
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || sets <= 0) return

        viewModelScope.launch {
            if (existing != null) {
                repository.updateExercise(
                    existing.copy(
                        name = trimmedName,
                        numberOfSets = sets,
                        restInSeconds = restSeconds
                    )
                )
            } else {
                val nextIndex = _state.value.exercises.size
                repository.insertExercise(
                    Exercise(
                        workoutId = workoutId,
                        name = trimmedName,
                        numberOfSets = sets,
                        restInSeconds = restSeconds,
                        orderIndex = nextIndex
                    )
                )
            }
            dismissSheet()
        }
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
            return EditRoutineViewModel(repository, workoutId) as T
        }
    }
}