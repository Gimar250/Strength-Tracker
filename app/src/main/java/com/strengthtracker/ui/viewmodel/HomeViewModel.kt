package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.util.CsvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val workouts: List<Workout> = emptyList(),
    val isEditMode: Boolean = false,
    val snackbarMessage: String? = null,
    val isCsvBusy: Boolean = false
)

class HomeViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _isEditMode = MutableStateFlow(false)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isCsvBusy = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllWorkouts(),
        _isEditMode,
        _snackbarMessage,
        _isCsvBusy
    ) { workouts, isEditMode, message, busy ->
        HomeUiState(workouts = workouts, isEditMode = isEditMode, snackbarMessage = message, isCsvBusy = busy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun clearSnackbar() { _snackbarMessage.value = null }
    fun setSnackbarMessage(message: String) { _snackbarMessage.value = message }

    fun toggleEditMode() = _isEditMode.update { !it }
    fun exitEditMode() { _isEditMode.value = false }

    // ── Workouts CRUD ───────────────────────────────────────────────────────

    fun createWorkout(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertWorkout(
                Workout(name = name.trim(), orderIndex = uiState.value.workouts.size)
            )
            onCreated(id)
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch { repository.deleteWorkout(workoutId) }
    }

    fun moveWorkoutUp(workout: Workout) {
        val list = uiState.value.workouts.toMutableList()
        val index = list.indexOfFirst { it.id == workout.id }
        if (index <= 0) return
        list.removeAt(index); list.add(index - 1, workout)
        viewModelScope.launch { repository.updateWorkoutOrder(list) }
    }

    fun moveWorkoutDown(workout: Workout) {
        val list = uiState.value.workouts.toMutableList()
        val index = list.indexOfFirst { it.id == workout.id }
        if (index < 0 || index >= list.size - 1) return
        list.removeAt(index); list.add(index + 1, workout)
        viewModelScope.launch { repository.updateWorkoutOrder(list) }
    }

    // ── CSV — data fetchers called from the composable ──────────────────────

    // Returns data needed for the workouts CSV; the composable does the actual file write
    suspend fun getDataForWorkoutExport(): Pair<List<Workout>, Map<Long, List<Exercise>>> {
        val workouts = uiState.value.workouts
        val exercisesMap = repository.getAllExercisesGrouped()
        return Pair(workouts, exercisesMap)
    }

    suspend fun getDataForLogExport(): Triple<List<com.strengthtracker.data.db.entity.WorkoutSession>, List<Workout>, Map<Long, List<Exercise>>> {
        val sessions = repository.getAllSessionsList()
        val workouts = uiState.value.workouts
        val exercisesMap = repository.getAllExercisesGrouped()
        return Triple(sessions, workouts, exercisesMap)
    }

    // Called after the composable has parsed the CSV file
    fun importWorkouts(imported: List<CsvManager.ImportedWorkout>) {
        if (imported.isEmpty()) {
            _snackbarMessage.value = "Nothing to import"
            return
        }
        _isCsvBusy.value = true
        viewModelScope.launch {
            val currentOffset = uiState.value.workouts.size
            imported.forEachIndexed { index, importedWorkout ->
                val workoutId = repository.insertWorkout(
                    Workout(
                        name = importedWorkout.name,
                        orderIndex = currentOffset + index
                    )
                )
                importedWorkout.exercises.forEach { ex ->
                    repository.insertExercise(
                        com.strengthtracker.data.db.entity.Exercise(
                            workoutId = workoutId,
                            name = ex.name,
                            numberOfSets = ex.sets,
                            restInSeconds = ex.restSeconds,
                            orderIndex = ex.orderIndex,
                            targetWeightKg = ex.targetWeightKg,
                            targetReps = ex.targetReps,
                            exerciseType = ex.exerciseType
                        )
                    )
                }
            }
            _isCsvBusy.value = false
            _snackbarMessage.value = "Imported ${imported.size} workout(s)"
        }
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}