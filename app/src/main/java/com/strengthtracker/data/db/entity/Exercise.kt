package com.strengthtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val numberOfSets: Int,
    val restInSeconds: Int,
    val orderIndex: Int,
    val targetWeightKg: Float? = null,
    // For REPS type: target reps. For TIMED type: target seconds.
    val targetReps: Int? = null,
    val exerciseType: ExerciseType = ExerciseType.REPS
)