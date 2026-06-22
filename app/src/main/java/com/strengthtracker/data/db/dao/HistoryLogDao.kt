package com.strengthtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.strengthtracker.data.db.entity.HistoryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HistoryLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<HistoryLog>)

    @Query("SELECT * FROM history_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC")
    fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>>

    @Query("SELECT * FROM history_logs WHERE workoutId = :workoutId ORDER BY timestamp DESC")
    fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>>

    @Query("SELECT * FROM history_logs WHERE workoutId = :workoutId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog?

    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<HistoryLog>

    // Live flow of all logs — used by HistoryViewModel
    @Query("SELECT * FROM history_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<HistoryLog>>

    // Get the most recent workout ID for a given exercise
    @Query("SELECT workoutId FROM history_logs WHERE exerciseId = :exerciseId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentWorkoutIdForExercise(exerciseId: Long): Long?

    // Get the best reps value from a specific workout for a specific exercise
    @Query("SELECT MAX(reps) FROM history_logs WHERE exerciseId = :exerciseId AND workoutId = :workoutId")
    suspend fun getMaxRepsForExerciseInWorkout(exerciseId: Long, workoutId: Long): Int?

    @Query("UPDATE history_logs SET weightKg = :weightKg, reps = :reps WHERE id = :logId")
    suspend fun updateLog(logId: Long, weightKg: Float, reps: Int)

    @Query("DELETE FROM history_logs")
    suspend fun deleteAll()
}