package com.strengthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strengthtracker.data.db.dao.ExerciseDao
import com.strengthtracker.data.db.dao.HistoryLogDao
import com.strengthtracker.data.db.dao.WorkoutDao
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.ExerciseType
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Workout::class, Exercise::class, HistoryLog::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun historyLogDao(): HistoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 → v2: added targetWeightKg and targetReps to exercises
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN targetWeightKg REAL")
                database.execSQL("ALTER TABLE exercises ADD COLUMN targetReps INTEGER")
            }
        }

        // v2 → v3: added exerciseType to exercises, orderIndex to workouts
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN exerciseType TEXT NOT NULL DEFAULT 'REPS'"
                )
                database.execSQL(
                    "ALTER TABLE workouts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strength_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { seedDatabase(it) }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val workoutDao = db.workoutDao()
            val exerciseDao = db.exerciseDao()

            val chestId = workoutDao.insertWorkout(Workout(name = "Chest Day", orderIndex = 0))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = chestId, name = "Bench Press",      numberOfSets = 4, restInSeconds = 120, orderIndex = 0, targetWeightKg = 80f,  targetReps = 5,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = chestId, name = "Incline Dumbbell", numberOfSets = 3, restInSeconds = 90,  orderIndex = 1, targetWeightKg = 24f,  targetReps = 10, exerciseType = ExerciseType.REPS),
                Exercise(workoutId = chestId, name = "Cable Fly",        numberOfSets = 3, restInSeconds = 60,  orderIndex = 2, targetWeightKg = 15f,  targetReps = 12, exerciseType = ExerciseType.REPS)
            ))

            val legsId = workoutDao.insertWorkout(Workout(name = "Leg Day", orderIndex = 1))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = legsId, name = "Squat",             numberOfSets = 4, restInSeconds = 180, orderIndex = 0, targetWeightKg = 100f, targetReps = 5,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = legsId, name = "Romanian Deadlift", numberOfSets = 3, restInSeconds = 120, orderIndex = 1, targetWeightKg = 70f,  targetReps = 8,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = legsId, name = "Leg Press",         numberOfSets = 3, restInSeconds = 90,  orderIndex = 2, targetWeightKg = 120f, targetReps = 10, exerciseType = ExerciseType.REPS),
                Exercise(workoutId = legsId, name = "Wall Sit",          numberOfSets = 3, restInSeconds = 60,  orderIndex = 3, targetWeightKg = null, targetReps = 60, exerciseType = ExerciseType.TIMED)
            ))

            val pullId = workoutDao.insertWorkout(Workout(name = "Pull Day", orderIndex = 2))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = pullId, name = "Deadlift",          numberOfSets = 4, restInSeconds = 180, orderIndex = 0, targetWeightKg = 120f, targetReps = 5,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = pullId, name = "Pull-Ups",          numberOfSets = 3, restInSeconds = 90,  orderIndex = 1, targetWeightKg = null,  targetReps = 8,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = pullId, name = "Barbell Row",       numberOfSets = 3, restInSeconds = 90,  orderIndex = 2, targetWeightKg = 60f,  targetReps = 8,  exerciseType = ExerciseType.REPS),
                Exercise(workoutId = pullId, name = "Dead Hang",         numberOfSets = 3, restInSeconds = 60,  orderIndex = 3, targetWeightKg = null,  targetReps = 30, exerciseType = ExerciseType.TIMED)
            ))
        }
    }
}