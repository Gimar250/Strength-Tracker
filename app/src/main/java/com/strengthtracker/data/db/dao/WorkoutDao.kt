package com.strengthtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.strengthtracker.data.db.entity.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Ordered by orderIndex, with id as tiebreaker for stability after migration
    @Query("SELECT * FROM workouts ORDER BY orderIndex ASC, id ASC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Long): Workout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<Workout>): List<Long>

    @Update
    suspend fun updateWorkout(workout: Workout)

    // Batch update used when persisting a reordered list
    @Update
    suspend fun updateAll(workouts: List<Workout>)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)
}