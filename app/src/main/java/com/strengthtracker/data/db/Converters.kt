package com.strengthtracker.data.db

import androidx.room.TypeConverter
import com.strengthtracker.data.db.entity.ExerciseType

class Converters {
    @TypeConverter
    fun fromExerciseType(type: ExerciseType): String = type.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType =
        ExerciseType.entries.find { it.name == value } ?: ExerciseType.REPS
}