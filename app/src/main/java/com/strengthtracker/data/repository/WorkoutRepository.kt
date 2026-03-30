package com.strengthtracker.data.repository

import com.strengthtracker.data.db.dao.ExerciseDao
import com.strengthtracker.data.db.dao.HistoryLogDao
import com.strengthtracker.data.db.dao.WorkoutDao
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val historyLogDao: HistoryLogDao
) {

    // --- Workouts ---

    fun getAllWorkouts(): Flow<List<Workout>> =
        workoutDao.getAllWorkouts()

    suspend fun insertWorkout(workout: Workout): Long =
        workoutDao.insertWorkout(workout)

    // --- Exercises ---

    // One-shot load: called once when a workout session begins
    suspend fun getExercisesForWorkout(workoutId: Long): List<Exercise> =
        exerciseDao.getExercisesForWorkoutOnce(workoutId)

    // --- History ---

    // Saves all completed sets for a workout session atomically
    suspend fun saveWorkoutSession(logs: List<HistoryLog>) =
        historyLogDao.insertAll(logs)

    fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForExercise(exerciseId)

    fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForWorkout(workoutId)

    suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog? =
        historyLogDao.getLastSessionForWorkout(workoutId)
}
