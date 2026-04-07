package com.strengthtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.strengthtracker.data.db.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTimestamp ASC")
    suspend fun getAllSessionsList(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE workoutId = :workoutId ORDER BY startTimestamp DESC")
    fun getSessionsForWorkout(workoutId: Long): Flow<List<WorkoutSession>>
}