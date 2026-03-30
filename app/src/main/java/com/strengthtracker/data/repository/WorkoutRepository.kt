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

    suspend fun getWorkoutById(id: Long): Workout? =
        workoutDao.getWorkoutById(id)

    suspend fun insertWorkout(workout: Workout): Long =
        workoutDao.insertWorkout(workout)

    suspend fun updateWorkout(workout: Workout) =
        workoutDao.updateWorkout(workout)

    suspend fun deleteWorkout(workoutId: Long) =
        workoutDao.deleteWorkout(workoutId)

    // --- Exercises ---

    fun getExercisesForWorkoutFlow(workoutId: Long): Flow<List<Exercise>> =
        exerciseDao.getExercisesForWorkout(workoutId)

    suspend fun getExercisesForWorkout(workoutId: Long): List<Exercise> =
        exerciseDao.getExercisesForWorkoutOnce(workoutId)

    suspend fun insertExercise(exercise: Exercise): Long =
        exerciseDao.insertExercise(exercise)

    suspend fun updateExercise(exercise: Exercise) =
        exerciseDao.updateExercise(exercise)

    // Persists a full reordered list — called after any move up/down action
    suspend fun updateExerciseOrder(exercises: List<Exercise>) =
        exerciseDao.updateAll(
            exercises.mapIndexed { index, exercise ->
                exercise.copy(orderIndex = index)
            }
        )

    suspend fun deleteExercise(exercise: Exercise) =
        exerciseDao.deleteExercise(exercise)

    // --- History ---

    suspend fun saveWorkoutSession(logs: List<HistoryLog>) =
        historyLogDao.insertAll(logs)

    fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForExercise(exerciseId)

    fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForWorkout(workoutId)

    suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog? =
        historyLogDao.getLastSessionForWorkout(workoutId)
}