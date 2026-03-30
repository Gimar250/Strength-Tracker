package com.strengthtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = true   // Keeps a schema history file — good practice for migrations
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun historyLogDao(): HistoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strength_tracker.db"
                )
                .addCallback(SeedCallback())
                .build()
                .also { INSTANCE = it }
            }
        }
    }

    // Seeds the database with sample workouts on first launch only
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Run seed on IO thread — DB is not yet available on the main thread here
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val workoutDao = db.workoutDao()
            val exerciseDao = db.exerciseDao()

            // --- Workout 1: Chest Day ---
            val chestId = workoutDao.insertWorkout(Workout(name = "Chest Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = chestId, name = "Bench Press",        numberOfSets = 4, restInSeconds = 120, orderIndex = 0),
                Exercise(workoutId = chestId, name = "Incline Dumbbell",   numberOfSets = 3, restInSeconds = 90,  orderIndex = 1),
                Exercise(workoutId = chestId, name = "Cable Fly",          numberOfSets = 3, restInSeconds = 60,  orderIndex = 2)
            ))

            // --- Workout 2: Leg Day ---
            val legsId = workoutDao.insertWorkout(Workout(name = "Leg Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = legsId, name = "Squat",               numberOfSets = 4, restInSeconds = 180, orderIndex = 0),
                Exercise(workoutId = legsId, name = "Romanian Deadlift",   numberOfSets = 3, restInSeconds = 120, orderIndex = 1),
                Exercise(workoutId = legsId, name = "Leg Press",           numberOfSets = 3, restInSeconds = 90,  orderIndex = 2),
                Exercise(workoutId = legsId, name = "Calf Raises",         numberOfSets = 4, restInSeconds = 60,  orderIndex = 3)
            ))

            // --- Workout 3: Pull Day ---
            val pullId = workoutDao.insertWorkout(Workout(name = "Pull Day"))
            exerciseDao.insertAll(listOf(
                Exercise(workoutId = pullId, name = "Deadlift",            numberOfSets = 4, restInSeconds = 180, orderIndex = 0),
                Exercise(workoutId = pullId, name = "Pull-Ups",            numberOfSets = 3, restInSeconds = 90,  orderIndex = 1),
                Exercise(workoutId = pullId, name = "Barbell Row",         numberOfSets = 3, restInSeconds = 90,  orderIndex = 2),
                Exercise(workoutId = pullId, name = "Face Pulls",          numberOfSets = 3, restInSeconds = 60,  orderIndex = 3)
            ))
        }
    }
}
