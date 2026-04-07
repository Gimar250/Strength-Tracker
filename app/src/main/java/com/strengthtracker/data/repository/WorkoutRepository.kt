package com.strengthtracker.data.repository

import com.strengthtracker.data.db.dao.ExerciseDao
import com.strengthtracker.data.db.dao.HistoryLogDao
import com.strengthtracker.data.db.dao.WorkoutDao
import com.strengthtracker.data.db.dao.WorkoutSessionDao
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.db.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val historyLogDao: HistoryLogDao,
    private val workoutSessionDao: WorkoutSessionDao
) {
    // --- Workouts ---
    fun getAllWorkouts(): Flow<List<Workout>> = workoutDao.getAllWorkouts()
    suspend fun getWorkoutById(id: Long): Workout? = workoutDao.getWorkoutById(id)
    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)
    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)
    suspend fun updateWorkoutOrder(workouts: List<Workout>) =
        workoutDao.updateAll(workouts.mapIndexed { i, w -> w.copy(orderIndex = i) })

    // --- Exercises ---
    fun getExercisesForWorkoutFlow(workoutId: Long): Flow<List<Exercise>> =
        exerciseDao.getExercisesForWorkout(workoutId)
    suspend fun getExercisesForWorkout(workoutId: Long): List<Exercise> =
        exerciseDao.getExercisesForWorkoutOnce(workoutId)
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insertExercise(exercise)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)
    suspend fun updateExerciseOrder(exercises: List<Exercise>) =
        exerciseDao.updateAll(exercises.mapIndexed { i, e -> e.copy(orderIndex = i) })
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)
    suspend fun getAllExercisesGrouped(): Map<Long, List<Exercise>> =
        exerciseDao.getAllExercises().groupBy { it.workoutId }
    fun getAllExercisesFlow(): Flow<List<Exercise>> = exerciseDao.getAllExercisesFlow()

    // --- History Logs ---
    suspend fun saveWorkoutSession(logs: List<HistoryLog>) = historyLogDao.insertAll(logs)
    fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForExercise(exerciseId)
    fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>> =
        historyLogDao.getLogsForWorkout(workoutId)
    suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog? =
        historyLogDao.getLastSessionForWorkout(workoutId)
    suspend fun getAllLogs(): List<HistoryLog> = historyLogDao.getAllLogs()
    fun getAllLogsFlow(): Flow<List<HistoryLog>> = historyLogDao.getAllLogsFlow()

    // --- Workout Sessions ---
    suspend fun insertWorkoutSession(session: WorkoutSession): Long =
        workoutSessionDao.insertSession(session)
    fun getAllSessionsFlow(): Flow<List<WorkoutSession>> =
        workoutSessionDao.getAllSessions()
    suspend fun getAllSessionsList(): List<WorkoutSession> =
        workoutSessionDao.getAllSessionsList()
}