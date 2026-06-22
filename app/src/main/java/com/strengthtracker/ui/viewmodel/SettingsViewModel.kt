package com.strengthtracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.util.CsvManager
import com.strengthtracker.util.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val volume: Int = SettingsRepository.DEFAULT_VOLUME,
    val beepDuration: Int = SettingsRepository.DEFAULT_BEEP_DURATION,
    val prepareBeepEnabled: Boolean = SettingsRepository.DEFAULT_PREPARE_BEEP,
    val endBeepEnabled: Boolean = SettingsRepository.DEFAULT_END_BEEP,
    val defaultRest: Int = SettingsRepository.DEFAULT_REST,
    val defaultRestString: String = SettingsRepository.DEFAULT_REST.toString(),
    val defaultSets: Int = SettingsRepository.DEFAULT_SETS,
    val defaultSetsString: String = SettingsRepository.DEFAULT_SETS.toString(),
    val defaultExerciseType: String = SettingsRepository.DEFAULT_EXERCISE_TYPE,
    val themeMode: String = SettingsRepository.DEFAULT_THEME,
    val fontSize: Int = SettingsRepository.DEFAULT_FONT_SIZE,
    val snackbarMessage: String? = null
)

class SettingsViewModel(
    private val repository: WorkoutRepository,
    private val context: Context
) : ViewModel() {

    private val _workouts = MutableStateFlow<List<com.strengthtracker.data.db.entity.Workout>>(emptyList())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isCsvBusy = MutableStateFlow(false)

    // ── Unsaved UI state (only persisted on SAVE) ────────────────────────────
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Workouts ─────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            repository.getAllWorkouts().collect { workouts ->
                _workouts.value = workouts
            }
        }
        viewModelScope.launch {
            val volume = SettingsRepository.volumeFlow(context).first()
            val beepDuration = SettingsRepository.beepDurationFlow(context).first()
            val prepareBeep = SettingsRepository.prepareBeepEnabledFlow(context).first()
            val endBeep = SettingsRepository.endBeepEnabledFlow(context).first()
            val defaultRest = SettingsRepository.defaultRestFlow(context).first()
            val defaultSets = SettingsRepository.defaultSetsFlow(context).first()
            val defaultExerciseType = SettingsRepository.defaultExerciseTypeFlow(context).first()
            val themeMode = SettingsRepository.themeModeFlow(context).first()
            val fontSize = SettingsRepository.fontSizeFlow(context).first()
            _uiState.value = SettingsUiState(
                volume = volume,
                beepDuration = beepDuration,
                prepareBeepEnabled = prepareBeep,
                endBeepEnabled = endBeep,
                defaultRest = defaultRest,
                defaultSets = defaultSets,
                defaultExerciseType = defaultExerciseType,
                themeMode = themeMode,
                fontSize = fontSize
            )
        }
    }

    // ── Sound ────────────────────────────────────────────────────────────────
    fun setVolume(volume: Int) {
        _uiState.value = _uiState.value.copy(volume = volume)
    }

    fun setBeepDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(beepDuration = duration)
    }

    fun setPrepareBeepEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(prepareBeepEnabled = enabled)
    }

    fun setEndBeepEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(endBeepEnabled = enabled)
    }

    // ── Workout defaults ─────────────────────────────────────────────────────
    fun setDefaultRest(seconds: Int) {
        _uiState.value = _uiState.value.copy(defaultRest = seconds, defaultRestString = seconds.toString())
    }

    fun setDefaultRestString(value: String) {
        _uiState.value = _uiState.value.copy(defaultRestString = value)
    }

    fun setDefaultSets(sets: Int) {
        _uiState.value = _uiState.value.copy(defaultSets = sets, defaultSetsString = sets.toString())
    }

    fun setDefaultSetsString(value: String) {
        _uiState.value = _uiState.value.copy(defaultSetsString = value)
    }

    fun setDefaultExerciseType(type: String) {
        _uiState.value = _uiState.value.copy(defaultExerciseType = type)
    }

    // ── Display ──────────────────────────────────────────────────────────────
    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setFontSize(sp: Int) {
        _uiState.value = _uiState.value.copy(fontSize = sp)
    }

   // ── Save all settings at once ────────────────────────────────────────────
    fun saveAllSettings() {
        val state = _uiState.value
        val defaultRestValue = state.defaultRestString.toIntOrNull()
        val defaultSetsValue = state.defaultSetsString.toIntOrNull()
        if (defaultRestValue == null || defaultRestValue <= 0) {
            _snackbarMessage.value = "Default rest must be greater than 0"
            return
        }
        if (defaultSetsValue == null || defaultSetsValue <= 0) {
            _snackbarMessage.value = "Default sets must be greater than 0"
            return
        }
        viewModelScope.launch {
            SettingsRepository.setVolume(context, state.volume)
            SettingsRepository.setBeepDuration(context, state.beepDuration)
            SettingsRepository.setPrepareBeepEnabled(context, state.prepareBeepEnabled)
            SettingsRepository.setEndBeepEnabled(context, state.endBeepEnabled)
            SettingsRepository.setDefaultRest(context, defaultRestValue)
            SettingsRepository.setDefaultSets(context, defaultSetsValue)
            SettingsRepository.setDefaultExerciseType(context, state.defaultExerciseType)
            SettingsRepository.setThemeMode(context, state.themeMode)
            SettingsRepository.setFontSize(context, state.fontSize)
            _snackbarMessage.value = "Settings saved"
        }
    }

    // ── Snackbar ─────────────────────────────────────────────────────────────
    fun setSnackbarMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // ── CSV ──────────────────────────────────────────────────────────────────
    suspend fun getDataForWorkoutExport(): Pair<List<com.strengthtracker.data.db.entity.Workout>, Map<Long, List<com.strengthtracker.data.db.entity.Exercise>>> {
        val workouts = _workouts.value
        val exercisesMap = repository.getAllExercisesGrouped()
        return Pair(workouts, exercisesMap)
    }

    suspend fun getDataForLogExport(): Triple<List<com.strengthtracker.data.db.entity.WorkoutSession>, List<com.strengthtracker.data.db.entity.Workout>, Map<Long, List<com.strengthtracker.data.db.entity.Exercise>>> {
        val sessions = repository.getAllSessionsList()
        val workouts = _workouts.value
        val exercisesMap = repository.getAllExercisesGrouped()
        return Triple(sessions, workouts, exercisesMap)
    }

    fun importWorkouts(imported: List<CsvManager.ImportedWorkout>) {
        if (imported.isEmpty()) {
            _snackbarMessage.value = "Nothing to import"
            return
        }
        _isCsvBusy.value = true
        viewModelScope.launch {
            val currentOffset = _workouts.value.size
            imported.forEachIndexed { index, importedWorkout ->
                val workoutId = repository.insertWorkout(
                    com.strengthtracker.data.db.entity.Workout(
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

    companion object {
        val ThemeOptions = listOf("system" to "System", "dark" to "Dark", "light" to "Light")
        val ExerciseTypeOptions = listOf("REPS" to "Reps", "TIMED" to "Timed")

        class Factory(private val repository: WorkoutRepository, private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(repository, context) as T
        }
    }
}
