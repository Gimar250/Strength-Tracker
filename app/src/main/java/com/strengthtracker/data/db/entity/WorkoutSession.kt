package com.strengthtracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// No foreign key — sessions are permanent history even if the workout is later deleted
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val workoutName: String,      // denormalized so history survives workout deletion
    val startTimestamp: Long,
    val durationSeconds: Int,
    val notes: String = "",
    val completedSets: Int,
    val totalSets: Int
)