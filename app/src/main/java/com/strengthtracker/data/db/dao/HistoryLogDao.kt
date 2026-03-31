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
}