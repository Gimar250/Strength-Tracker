package com.strengthtracker

import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.data.db.entity.WorkoutSession
import com.strengthtracker.ui.viewmodel.SessionDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class SessionDetailViewModelTest {

    private lateinit var env: FakeWorkoutEnvironment

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun TestScope.awaitLoaded(vm: SessionDetailViewModel) {
        var waited = 0L
        while (vm.state.value.isLoading) {
            advanceTimeBy(100)
            runCurrent()
            waited += 100
            if (waited >= 30_000) fail("Timed out waiting for session state")
        }
    }

    @Test
    fun `loads the session data for the given day`() = runTest {
        env = FakeWorkoutEnvironment()

        // Noon today — safely away from the day boundary
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val ts = cal.timeInMillis

        val workoutId = env.repository.insertWorkout(Workout(name = "Chest", orderIndex = 0))
        val benchId = env.repository.insertExercise(
            Exercise(workoutId = workoutId, name = "Bench", numberOfSets = 2, restInSeconds = 60, orderIndex = 0)
        )
        val flyId = env.repository.insertExercise(
            Exercise(workoutId = workoutId, name = "Fly", numberOfSets = 1, restInSeconds = 60, orderIndex = 1)
        )
        env.historyLogDao.insertLog(
            HistoryLog(exerciseId = benchId, workoutId = workoutId, setNumber = 1, weightKg = 60f, reps = 5, timestamp = ts - 300_000)
        )
        env.historyLogDao.insertLog(
            HistoryLog(exerciseId = benchId, workoutId = workoutId, setNumber = 2, weightKg = 65f, reps = 4, timestamp = ts - 200_000)
        )
        env.historyLogDao.insertLog(
            HistoryLog(exerciseId = flyId, workoutId = workoutId, setNumber = 1, weightKg = 20f, reps = 12, timestamp = ts - 100_000)
        )
        // A log from another day — must not appear
        env.historyLogDao.insertLog(
            HistoryLog(exerciseId = benchId, workoutId = workoutId, setNumber = 1, weightKg = 70f, reps = 3, timestamp = ts - 2L * 24 * 60 * 60 * 1000)
        )
        env.workoutSessionDao.insertSession(
            WorkoutSession(
                workoutId = workoutId, workoutName = "Chest",
                startTimestamp = ts - 300_000, durationSeconds = 300,
                notes = "solid session", completedSets = 3, totalSets = 3
            )
        )

        val vm = SessionDetailViewModel(env.repository, workoutId, ts)
        awaitLoaded(vm)

        val s = vm.state.value
        assertEquals("Chest", s.workoutName)
        assertEquals(300, s.durationSeconds)
        assertEquals("solid session", s.notes)
        assertEquals(3, s.completedSets)
        assertEquals(3, s.totalSets)
        assertEquals(2, s.logsByExercise[benchId]!!.size)
        assertEquals(1, s.logsByExercise[flyId]!!.size)
    }
}
