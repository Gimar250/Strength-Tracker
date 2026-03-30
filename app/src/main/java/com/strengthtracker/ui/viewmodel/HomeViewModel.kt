package com.strengthtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = repository
        .getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Returns the new workout's ID so we can navigate straight to the edit screen
    fun createWorkout(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertWorkout(Workout(name = name.trim()))
            onCreated(id)
        }
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}