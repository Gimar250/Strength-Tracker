package com.strengthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.strengthtracker.data.db.dao.ExerciseDao
import com.strengthtracker.data.db.dao.HistoryLogDao
import com.strengthtracker.data.db.dao.WorkoutDao
import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Workout::class, Exercise::class, HistoryLog::class],
    version = 2,           // ← bumped from 1 to 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun historyLogDao(): HistoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Adds the two new nullable columns to the exercises table.
        // SQLite allows ALTER TABLE ADD COLUMN for nullable/defaulted columns only.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN targetWeightKg REAL"
                )
                database.execSQL(
                    "ALTER TABLE exercises ADD COLUMN targetReps INTEGER"
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
                    .addMigrations(MIGRATION_1_2)   // ← register migration
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
                INSTANCE?.let { database -> seedDatabase(database) }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val workoutDao = db.workoutDao()
            val exerciseDao = db.exerciseDao()

            val chestId = workoutDao.insertWorkout(Workout(name = "Chest Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = chestId, name = "Bench Press",      numberOfSets = 4, restInSeconds = 120, orderIndex = 0, targetWeightKg = 80f, targetReps = 5),
                Exercise(workoutId = chestId, name = "Incline Dumbbell", numberOfSets = 3, restInSeconds = 90,  orderIndex = 1, targetWeightKg = 24f, targetReps = 10),
                Exercise(workoutId = chestId, name = "Cable Fly",        numberOfSets = 3, restInSeconds = 60,  orderIndex = 2, targetWeightKg = 15f, targetReps = 12)
            ))

            val legsId = workoutDao.insertWorkout(Workout(name = "Leg Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = legsId, name = "Squat",             numberOfSets = 4, restInSeconds = 180, orderIndex = 0, targetWeightKg = 100f, targetReps = 5),
                Exercise(workoutId = legsId, name = "Romanian Deadlift", numberOfSets = 3, restInSeconds = 120, orderIndex = 1, targetWeightKg = 70f,  targetReps = 8),
                Exercise(workoutId = legsId, name = "Leg Press",         numberOfSets = 3, restInSeconds = 90,  orderIndex = 2, targetWeightKg = 120f, targetReps = 10),
                Exercise(workoutId = legsId, name = "Calf Raises",       numberOfSets = 4, restInSeconds = 60,  orderIndex = 3, targetWeightKg = 40f,  targetReps = 15)
            ))

            val pullId = workoutDao.insertWorkout(Workout(name = "Pull Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = pullId, name = "Deadlift",          numberOfSets = 4, restInSeconds = 180, orderIndex = 0, targetWeightKg = 120f, targetReps = 5),
                Exercise(workoutId = pullId, name = "Pull-Ups",          numberOfSets = 3, restInSeconds = 90,  orderIndex = 1, targetWeightKg = null,  targetReps = 8),
                Exercise(workoutId = pullId, name = "Barbell Row",       numberOfSets = 3, restInSeconds = 90,  orderIndex = 2, targetWeightKg = 60f,  targetReps = 8),
                Exercise(workoutId = pullId, name = "Face Pulls",        numberOfSets = 3, restInSeconds = 60,  orderIndex = 3, targetWeightKg = 20f,  targetReps = 15)
            ))
        }
    }
}