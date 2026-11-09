package com.strengthtracker

import com.strengthtracker.data.db.dao.ExerciseDao
import com.strengthtracker.data.db.dao.HistoryLogDao
import com.strengthtracker.data.db.dao.WorkoutDao
import com.strengthtracker.data.db.dao.WorkoutSessionDao
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.db.entity.WorkoutSession
import com.strengthtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory implementations of the Room DAOs for JVM unit tests.
 * A real [WorkoutRepository] delegates to them, so tests observe the same
 * code path the app uses.
 */

class InMemoryWorkoutDao : WorkoutDao {
    private val workouts = mutableListOf<Workout>()
    private var nextId = 1L

    override fun getAllWorkouts(): Flow<List<Workout>> =
        flowOf(workouts.sortedBy { it.orderIndex })

    override suspend fun getWorkoutById(id: Long): Workout? =
        workouts.find { it.id == id }

    override suspend fun insertWorkout(workout: Workout): Long {
        val id = if (workout.id == 0L) nextId++ else workout.id
        workouts.add(workout.copy(id = id))
        return id
    }

    override suspend fun insertAll(workouts: List<Workout>): List<Long> =
        workouts.map { insertWorkout(it) }

    override suspend fun updateWorkout(workout: Workout) {
        val i = this.workouts.indexOfFirst { it.id == workout.id }
        if (i >= 0) this.workouts[i] = workout
    }

    override suspend fun updateAll(workouts: List<Workout>) {
        workouts.forEach { updateWorkout(it) }
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        workouts.removeAll { it.id == workoutId }
    }

    override suspend fun deleteAll() {
        workouts.clear()
    }
}

class InMemoryExerciseDao : ExerciseDao {
    private val exercises = mutableListOf<Exercise>()
    private var nextId = 1L

    override fun getExercisesForWorkout(workoutId: Long): Flow<List<Exercise>> =
        flowOf(exercises.filter { it.workoutId == workoutId }.sortedBy { it.orderIndex })

    override suspend fun getExercisesForWorkoutOnce(workoutId: Long): List<Exercise> =
        exercises.filter { it.workoutId == workoutId }.sortedBy { it.orderIndex }

    override suspend fun getAllExercises(): List<Exercise> =
        exercises.sortedBy { it.orderIndex }

    override fun getAllExercisesFlow(): Flow<List<Exercise>> =
        flowOf(exercises.sortedBy { it.orderIndex })

    override suspend fun getExerciseById(id: Long): Exercise? =
        exercises.find { it.id == id }

    override suspend fun insertExercise(exercise: Exercise): Long {
        val id = if (exercise.id == 0L) nextId++ else exercise.id
        exercises.add(exercise.copy(id = id))
        return id
    }

    override suspend fun insertAll(exercises: List<Exercise>): List<Long> =
        exercises.map { insertExercise(it) }

    override suspend fun updateExercise(exercise: Exercise) {
        val i = this.exercises.indexOfFirst { it.id == exercise.id }
        if (i >= 0) this.exercises[i] = exercise
    }

    override suspend fun updateAll(exercises: List<Exercise>) {
        exercises.forEach { updateExercise(it) }
    }

    override suspend fun deleteExercise(exercise: Exercise) {
        exercises.removeAll { it.id == exercise.id }
    }

    override suspend fun deleteExercisesForWorkout(workoutId: Long) {
        exercises.removeAll { it.workoutId == workoutId }
    }

    override suspend fun deleteAll() {
        exercises.clear()
    }
}

class InMemoryHistoryLogDao : HistoryLogDao {
    val logs = mutableListOf<HistoryLog>()
    private var nextId = 1L

    override suspend fun insertLog(log: HistoryLog): Long {
        val id = if (log.id == 0L) nextId++ else log.id
        val stored = log.copy(id = id)
        val i = logs.indexOfFirst { it.id == id }
        if (i >= 0) logs[i] = stored else logs.add(stored)
        return id
    }

    override suspend fun insertAll(logs: List<HistoryLog>) {
        logs.forEach { insertLog(it) }
    }

    override fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>> =
        flowOf(this.logs.filter { it.exerciseId == exerciseId }.sortedByDescending { it.timestamp })

    override fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>> =
        flowOf(this.logs.filter { it.workoutId == workoutId }.sortedByDescending { it.timestamp })

    override suspend fun getLogsForWorkoutInRange(workoutId: Long, start: Long, end: Long): List<HistoryLog> =
        this.logs.filter { it.workoutId == workoutId && it.timestamp >= start && it.timestamp < end }
            .sortedBy { it.timestamp }

    override suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog? =
        this.logs.filter { it.workoutId == workoutId }.maxByOrNull { it.timestamp }

    override suspend fun getAllLogs(): List<HistoryLog> =
        this.logs.sortedByDescending { it.timestamp }

    override fun getAllLogsFlow(): Flow<List<HistoryLog>> =
        flowOf(this.logs.sortedByDescending { it.timestamp })

    override suspend fun getMostRecentWorkoutIdForExercise(exerciseId: Long): Long? =
        this.logs.filter { it.exerciseId == exerciseId }.maxByOrNull { it.timestamp }?.workoutId

    override suspend fun getMaxRepsForExerciseInWorkout(exerciseId: Long, workoutId: Long): Int? =
        this.logs.filter { it.exerciseId == exerciseId && it.workoutId == workoutId }.maxByOrNull { it.reps }?.reps

    override suspend fun getMaxWeightForExerciseInWorkout(exerciseId: Long, workoutId: Long): Float? =
        this.logs.filter { it.exerciseId == exerciseId && it.workoutId == workoutId }.maxByOrNull { it.weightKg }?.weightKg

    override suspend fun updateLog(logId: Long, weightKg: Float, reps: Int) {
        val i = logs.indexOfFirst { it.id == logId }
        if (i >= 0) logs[i] = logs[i].copy(weightKg = weightKg, reps = reps)
    }

    override suspend fun deleteAll() {
        logs.clear()
    }
}

class InMemoryWorkoutSessionDao : WorkoutSessionDao {
    val sessions = mutableListOf<WorkoutSession>()
    private var nextId = 1L

    override suspend fun insertSession(session: WorkoutSession): Long {
        val id = if (session.id == 0L) nextId++ else session.id
        val stored = session.copy(id = id)
        val i = this.sessions.indexOfFirst { it.id == id }
        if (i >= 0) this.sessions[i] = stored else this.sessions.add(stored)
        return id
    }

    override fun getAllSessions(): Flow<List<WorkoutSession>> =
        flowOf(sessions.sortedByDescending { it.startTimestamp })

    override suspend fun getAllSessionsList(): List<WorkoutSession> =
        sessions.sortedBy { it.startTimestamp }

    override fun getSessionsForWorkout(workoutId: Long): Flow<List<WorkoutSession>> =
        flowOf(sessions.filter { it.workoutId == workoutId }.sortedByDescending { it.startTimestamp })

    override suspend fun deleteAll() {
        sessions.clear()
    }
}

class FakeWorkoutEnvironment {
    val workoutDao = InMemoryWorkoutDao()
    val exerciseDao = InMemoryExerciseDao()
    val historyLogDao = InMemoryHistoryLogDao()
    val workoutSessionDao = InMemoryWorkoutSessionDao()
    val repository = WorkoutRepository(workoutDao, exerciseDao, historyLogDao, workoutSessionDao)
}
