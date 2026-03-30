package com.strengthtracker.util

import android.content.Context
import android.net.Uri
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── Export ──────────────────────────────────────────────────────────────

    fun exportWorkouts(
        context: Context,
        uri: Uri,
        workouts: List<Workout>,
        exercisesByWorkout: Map<Long, List<Exercise>>
    ) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            val writer = stream.bufferedWriter()
            writer.write("workout_name,workout_order,exercise_name,exercise_order,sets,rest_seconds,target_weight_kg,target_reps,exercise_type\n")
            workouts.forEach { workout ->
                val exercises = exercisesByWorkout[workout.id] ?: emptyList()
                if (exercises.isEmpty()) {
                    writer.write("${workout.name.escapeCsv()},${workout.orderIndex},,,,,,\n")
                } else {
                    exercises.forEach { ex ->
                        writer.write(
                            "${workout.name.escapeCsv()}," +
                                    "${workout.orderIndex}," +
                                    "${ex.name.escapeCsv()}," +
                                    "${ex.orderIndex}," +
                                    "${ex.numberOfSets}," +
                                    "${ex.restInSeconds}," +
                                    "${ex.targetWeightKg ?: ""}," +
                                    "${ex.targetReps ?: ""}," +
                                    "${ex.exerciseType.name}\n"
                        )
                    }
                }
            }
            writer.flush()
        }
    }

    fun exportLogs(
        context: Context,
        uri: Uri,
        logs: List<HistoryLog>,
        workouts: List<Workout>,
        exercisesByWorkout: Map<Long, List<Exercise>>
    ) {
        val workoutMap = workouts.associateBy { it.id }
        val exerciseMap = exercisesByWorkout.values.flatten().associateBy { it.id }

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            val writer = stream.bufferedWriter()
            writer.write("workout_name,exercise_name,exercise_type,set_number,weight_kg,reps_or_seconds,timestamp\n")
            logs.forEach { log ->
                val workoutName = workoutMap[log.workoutId]?.name ?: "Unknown"
                val exercise = exerciseMap[log.exerciseId]
                val exerciseName = exercise?.name ?: "Unknown"
                val exerciseType = exercise?.exerciseType?.name ?: "REPS"
                writer.write(
                    "${workoutName.escapeCsv()}," +
                            "${exerciseName.escapeCsv()}," +
                            "$exerciseType," +
                            "${log.setNumber}," +
                            "${log.weightKg}," +
                            "${log.reps}," +
                            "${dateFormat.format(Date(log.timestamp))}\n"
                )
            }
            writer.flush()
        }
    }

    // ── Import ──────────────────────────────────────────────────────────────

    data class ImportedExercise(
        val name: String,
        val orderIndex: Int,
        val sets: Int,
        val restSeconds: Int,
        val targetWeightKg: Float?,
        val targetReps: Int?,
        val exerciseType: ExerciseType
    )

    data class ImportedWorkout(
        val name: String,
        val orderIndex: Int,
        val exercises: List<ImportedExercise>
    )

    fun importWorkouts(context: Context, uri: Uri): List<ImportedWorkout> {
        val rows = mutableMapOf<String, MutableList<ImportedExercise>>()
        val workoutOrders = mutableMapOf<String, Int>()

        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            reader.readLine() // skip header
            var line = reader.readLine()
            while (line != null) {
                val cols = line.parseCsvLine()
                val workoutName = cols.getOrNull(0)?.trim() ?: ""
                if (workoutName.isNotBlank()) {
                    val workoutOrder = cols.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                    workoutOrders[workoutName] = workoutOrder
                    if (!rows.containsKey(workoutName)) rows[workoutName] = mutableListOf()

                    val exerciseName = cols.getOrNull(2)?.trim() ?: ""
                    if (exerciseName.isNotBlank()) {
                        rows[workoutName]!!.add(
                            ImportedExercise(
                                name = exerciseName,
                                orderIndex = cols.getOrNull(3)?.trim()?.toIntOrNull() ?: 0,
                                sets = cols.getOrNull(4)?.trim()?.toIntOrNull() ?: 3,
                                restSeconds = cols.getOrNull(5)?.trim()?.toIntOrNull() ?: 90,
                                targetWeightKg = cols.getOrNull(6)?.trim()?.toFloatOrNull(),
                                targetReps = cols.getOrNull(7)?.trim()?.toIntOrNull(),
                                exerciseType = cols.getOrNull(8)?.trim()?.let { t ->
                                    ExerciseType.entries.find { it.name == t }
                                } ?: ExerciseType.REPS
                            )
                        )
                    }
                }
                line = reader.readLine()
            }
        }

        return rows.map { (name, exercises) ->
            ImportedWorkout(
                name = name,
                orderIndex = workoutOrders[name] ?: 0,
                exercises = exercises.sortedBy { it.orderIndex }
            )
        }.sortedBy { it.orderIndex }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun String.escapeCsv(): String =
        if (contains(',') || contains('"') || contains('\n'))
            "\"${replace("\"", "\"\"")}\""
        else this

    private fun String.parseCsvLine(): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < length) {
            when {
                this[i] == '"' && inQuotes && i + 1 < length && this[i + 1] == '"' -> {
                    current.append('"'); i += 2
                }
                this[i] == '"' -> { inQuotes = !inQuotes; i++ }
                this[i] == ',' && !inQuotes -> {
                    result.add(current.toString()); current = StringBuilder(); i++
                }
                else -> { current.append(this[i]); i++ }
            }
        }
        result.add(current.toString())
        return result
    }
}