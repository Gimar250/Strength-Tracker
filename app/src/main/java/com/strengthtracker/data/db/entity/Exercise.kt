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
    // Nullable — null means no target configured
    val targetWeightKg: Float? = null,
    val targetReps: Int? = null
)