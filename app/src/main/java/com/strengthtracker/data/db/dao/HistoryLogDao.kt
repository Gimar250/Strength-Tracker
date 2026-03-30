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

    // Batch insert — used at the end of a workout to save everything at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<HistoryLog>)

    // Retrieve all logs for a specific exercise, newest first
    @Query("""
        SELECT * FROM history_logs 
        WHERE exerciseId = :exerciseId 
        ORDER BY timestamp DESC
    """)
    fun getLogsForExercise(exerciseId: Long): Flow<List<HistoryLog>>

    // Retrieve all logs for a complete workout session (by workout + date range)
    @Query("""
        SELECT * FROM history_logs 
        WHERE workoutId = :workoutId 
        ORDER BY timestamp DESC
    """)
    fun getLogsForWorkout(workoutId: Long): Flow<List<HistoryLog>>

    // Useful for home screen: show last time this workout was done
    @Query("""
        SELECT * FROM history_logs 
        WHERE workoutId = :workoutId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastSessionForWorkout(workoutId: Long): HistoryLog?
}
