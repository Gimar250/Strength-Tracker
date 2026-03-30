package com.strengthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.strengthtracker.data.db.AppDatabase
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.screen.ActiveWorkoutScreen
import com.strengthtracker.ui.screen.EditRoutineScreen
import com.strengthtracker.ui.screen.HomeScreen
import com.strengthtracker.ui.theme.StrengthTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = WorkoutRepository(
            workoutDao = db.workoutDao(),
            exerciseDao = db.exerciseDao(),
            historyLogDao = db.historyLogDao()
        )

        setContent {
            StrengthTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                repository = repository,
                                onStartWorkout = { workoutId ->
                                    navController.navigate("workout/$workoutId")
                                },
                                onEditWorkout = { workoutId ->      // ← new
                                    navController.navigate("edit/$workoutId")
                                }
                            )
                        }
                        composable(
                            route = "workout/{workoutId}",
                            arguments = listOf(
                                navArgument("workoutId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments!!.getLong("workoutId")
                            ActiveWorkoutScreen(
                                repository = repository,
                                workoutId = workoutId,
                                onWorkoutFinished = { navController.popBackStack() }
                            )
                        }
                        composable(                                 // ← new route
                            route = "edit/{workoutId}",
                            arguments = listOf(
                                navArgument("workoutId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val workoutId = backStackEntry.arguments!!.getLong("workoutId")
                            EditRoutineScreen(
                                repository = repository,
                                workoutId = workoutId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}