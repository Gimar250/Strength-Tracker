package com.strengthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.strengthtracker.data.db.AppDatabase
import com.strengthtracker.data.repository.WorkoutRepository
import com.strengthtracker.ui.screen.*
import com.strengthtracker.ui.theme.StrengthTrackerTheme

// ── Bottom nav definition ─────────────────────────────────────────────────────

sealed class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Workouts : BottomTab("home", "Workouts", Icons.Default.FitnessCenter)
    object History  : BottomTab("history", "History", Icons.Default.ShowChart)
}

val bottomTabs = listOf(BottomTab.Workouts, BottomTab.History)

// Routes where the bottom nav should be hidden
private val noBottomBarRoutes = setOf(
    "workout/{workoutId}",
    "edit/{workoutId}",
    "exercise_history/{exerciseId}"
)

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val repository = WorkoutRepository(
            workoutDao    = db.workoutDao(),
            exerciseDao   = db.exerciseDao(),
            historyLogDao = db.historyLogDao()
        )

        setContent {
            StrengthTrackerTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}

@Composable
private fun AppNavigation(repository: WorkoutRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Bottom bar visible only on the two main tab routes
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.Workouts.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Tab 1: Workouts ───────────────────────────────────────────
            composable(BottomTab.Workouts.route) {
                HomeScreen(
                    repository = repository,
                    onStartWorkout = { workoutId ->
                        navController.navigate("workout/$workoutId")
                    },
                    onEditWorkout = { workoutId ->
                        navController.navigate("edit/$workoutId")
                    }
                )
            }

            // ── Active Workout (full screen, no bottom bar) ───────────────
            composable(
                route = "workout/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                ActiveWorkoutScreen(
                    repository = repository,
                    workoutId = entry.arguments!!.getLong("workoutId"),
                    onWorkoutFinished = { navController.popBackStack() }
                )
            }

            // ── Edit Routine (full screen, no bottom bar) ─────────────────
            composable(
                route = "edit/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { entry ->
                EditRoutineScreen(
                    repository = repository,
                    workoutId = entry.arguments!!.getLong("workoutId"),
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Tab 2: History ────────────────────────────────────────────
            composable(BottomTab.History.route) {
                HistoryScreen(
                    repository = repository,
                    onExerciseSelected = { exerciseId ->
                        navController.navigate("exercise_history/$exerciseId")
                    }
                )
            }

            // ── Exercise detail (full screen, no bottom bar) ──────────────
            composable(
                route = "exercise_history/{exerciseId}",
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
            ) { entry ->
                ExerciseHistoryScreen(
                    repository = repository,
                    exerciseId = entry.arguments!!.getLong("exerciseId"),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            // Avoid building up a large back stack when switching tabs
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelLarge)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}