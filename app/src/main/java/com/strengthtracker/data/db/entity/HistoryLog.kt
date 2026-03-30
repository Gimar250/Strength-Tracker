package com.strengthtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_logs",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class HistoryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val workoutId: Long,   // Denormalized for easier history queries by workout
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    // Stored as epoch milliseconds — no external date library needed
    val timestamp: Long = System.currentTimeMillis()
)
