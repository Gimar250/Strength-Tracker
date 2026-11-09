package com.strengthtracker

import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.db.entity.WorkoutSession
import com.strengthtracker.util.CsvManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvManagerTest {

    @Test
    fun `export and import round trip preserves workouts and exercises`() {
        val w1 = Workout(id = 1, name = "Chest", orderIndex = 0)
        val w2 = Workout(id = 2, name = "Legs, Squats", orderIndex = 1) // comma must be escaped
        val ex1 = Exercise(
            id = 1, workoutId = 1, name = "Bench", numberOfSets = 4, restInSeconds = 120,
            orderIndex = 0, targetWeightKg = 80f, targetReps = 5, exerciseType = ExerciseType.REPS
        )
        val ex2 = Exercise(
            id = 2, workoutId = 1, name = "Fly", numberOfSets = 3, restInSeconds = 60,
            orderIndex = 1, exerciseType = ExerciseType.TIMED
        )
        val ex3 = Exercise(
            id = 3, workoutId = 2, name = "Squat", numberOfSets = 5, restInSeconds = 180,
            orderIndex = 0, targetReps = 6
        )

        val text = CsvManager.exportWorkoutsText(listOf(w1, w2), mapOf(1L to listOf(ex1, ex2), 2L to listOf(ex3)))
        val result = CsvManager.importWorkoutsText(text)

        assertEquals(0, result.skippedRows)
        assertEquals(2, result.workouts.size)
        assertEquals("Chest", result.workouts[0].name)
        assertEquals("Legs, Squats", result.workouts[1].name)

        val chest = result.workouts[0].exercises
        assertEquals(2, chest.size)
        assertEquals("Bench", chest[0].name)
        assertEquals(4, chest[0].sets)
        assertEquals(120, chest[0].restSeconds)
        assertEquals(80f, chest[0].targetWeightKg)
        assertEquals(5, chest[0].targetReps)
        assertEquals(ExerciseType.REPS, chest[0].exerciseType)
        assertEquals(ExerciseType.TIMED, chest[1].exerciseType)
        assertEquals(6, result.workouts[1].exercises[0].targetReps)
    }

    @Test
    fun `malformed numeric rows are skipped and counted`() {
        val text = """
            workout_name,workout_order,exercise_name,exercise_order,sets,rest_seconds,target_weight_kg,target_reps,exercise_type
            Chest,0,Bench,0,abc,120,80,5,REPS
            Chest,0,Fly,1,3,60,,12,TIMED
        """.trimIndent()

        val result = CsvManager.importWorkoutsText(text)

        assertEquals(1, result.skippedRows)
        assertEquals(1, result.workouts.size)
        assertEquals(listOf("Fly"), result.workouts[0].exercises.map { it.name })
        val fly = result.workouts[0].exercises[0]
        assertEquals(3, fly.sets)
        assertEquals(60, fly.restSeconds)
        assertEquals(null, fly.targetWeightKg)
        assertEquals(12, fly.targetReps)
    }

    @Test
    fun `logs are matched to the correct session when two sessions share a day`() {
        val workout = Workout(id = 1, name = "Chest", orderIndex = 0)
        val exercise = Exercise(id = 1, workoutId = 1, name = "Bench", numberOfSets = 2, restInSeconds = 60, orderIndex = 0)
        val day = 1_700_000_000_000L
        val morning = WorkoutSession(
            id = 1, workoutId = 1, workoutName = "Chest",
            startTimestamp = day, durationSeconds = 300, notes = "morning",
            completedSets = 2, totalSets = 2
        )
        val evening = WorkoutSession(
            id = 2, workoutId = 1, workoutName = "Chest",
            startTimestamp = day + 3_600_000L, durationSeconds = 300, notes = "evening",
            completedSets = 2, totalSets = 2
        )
        val logs = listOf(
            HistoryLog(id = 1, exerciseId = 1, workoutId = 1, setNumber = 1, weightKg = 60f, reps = 5, timestamp = day + 60_000L),
            HistoryLog(id = 2, exerciseId = 1, workoutId = 1, setNumber = 1, weightKg = 65f, reps = 5, timestamp = day + 3_660_000L)
        )

        val text = CsvManager.exportLogsText(logs, listOf(workout), mapOf(1L to listOf(exercise)), listOf(morning, evening))
        val lines = text.trim().lines()

        assertEquals(3, lines.size)
        assertEquals("morning", lines[1].split(",").last())
        assertEquals("evening", lines[2].split(",").last())
    }
}
