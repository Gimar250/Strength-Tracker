package com.strengthtracker

import com.strengthtracker.data.db.entity.Exercise
import com.strengthtracker.data.db.entity.HistoryLog
import com.strengthtracker.data.db.entity.Workout
import com.strengthtracker.ui.viewmodel.ActiveWorkoutViewModel
import com.strengthtracker.ui.viewmodel.RestSoundSettings
import com.strengthtracker.ui.viewmodel.RestSoundSettingsProvider
import com.strengthtracker.ui.viewmodel.SetStatus
import com.strengthtracker.ui.viewmodel.WorkoutScreenState
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

class ActiveWorkoutViewModelTest {

    private lateinit var env: FakeWorkoutEnvironment
    private lateinit var vm: ActiveWorkoutViewModel

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

    // Sound is disabled so the test never touches the Android tone engine
    private class TestRestSettingsProvider : RestSoundSettingsProvider {
        override suspend fun settings(): RestSoundSettings =
            RestSoundSettings(volume = 0, beepDurationMs = 100, prepareBeepEnabled = false, endBeepEnabled = false)
    }

    private fun exercise(
        workoutId: Long = 0L,
        sets: Int = 2,
        rest: Int = 1
    ): Exercise = Exercise(
        workoutId = workoutId,
        name = "Bench",
        numberOfSets = sets,
        restInSeconds = rest,
        orderIndex = 0
    )

    private suspend fun makeViewModel(exercises: List<Exercise>, workoutName: String = "Test Workout"): ActiveWorkoutViewModel {
        val workoutId = env.repository.insertWorkout(Workout(name = workoutName, orderIndex = 0))
        exercises.forEach { env.repository.insertExercise(it.copy(workoutId = workoutId, id = 0)) }
        return ActiveWorkoutViewModel(env.repository, workoutId, TestRestSettingsProvider())
    }

    private suspend fun TestScope.awaitUntil(timeoutMillis: Long = 30_000, condition: () -> Boolean) {
        var waited = 0L
        while (!condition()) {
            advanceTimeBy(100)
            runCurrent()
            waited += 100
            if (waited >= timeoutMillis) fail("Timed out waiting for state change")
        }
    }

    @Test
    fun `completing all sets reaches summary and persists logs incrementally`() = runTest {
        env = FakeWorkoutEnvironment()
        vm = makeViewModel(listOf(exercise(sets = 2)))

        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }

        vm.onWeightChanged("60")
        vm.onRepsChanged("5")
        vm.completeSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        vm.onWeightChanged("62.5")
        vm.onRepsChanged("4")
        vm.completeSet()

        val summary = vm.state.value as WorkoutScreenState.Summary
        assertEquals(2, summary.logs.size)

        // Sets were persisted to the database as they were completed
        val logs = env.historyLogDao.logs.associateBy { it.setNumber }
        assertEquals(60f, logs[1]!!.weightKg)
        assertEquals(5, logs[1]!!.reps)
        assertEquals(62.5f, logs[2]!!.weightKg)
        assertEquals(4, logs[2]!!.reps)

        // Finishing writes the session record
        vm.onNotesChanged("felt strong")
        vm.saveAndFinish()
        assertEquals(WorkoutScreenState.Finished, vm.state.value)
        val sessions = env.workoutSessionDao.getAllSessionsList()
        assertEquals(1, sessions.size)
        assertEquals("felt strong", sessions[0].notes)
        assertEquals(2, sessions[0].completedSets)
        assertEquals(2, sessions[0].totalSets)
    }

    @Test
    fun `modifying a completed set updates the persisted log`() = runTest {
        env = FakeWorkoutEnvironment()
        vm = makeViewModel(listOf(exercise(sets = 2)))

        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }
        vm.onWeightChanged("60")
        vm.onRepsChanged("5")
        vm.completeSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        // Go back to set 1 — values are prefilled from the persisted log
        vm.onBackNavigation()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 1 }
        val active = vm.state.value as WorkoutScreenState.ActiveSet
        assertEquals("60.0", active.weightKg) // Float.toString() of 60f
        assertEquals("5", active.reps)

        vm.onRepsChanged("7")
        vm.onModifySet("60", "7")
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        val log = env.historyLogDao.logs.find { it.setNumber == 1 }!!
        assertEquals(60f, log.weightKg)
        assertEquals(7, log.reps)
    }

    @Test
    fun `skipping a registered set persists zero values`() = runTest {
        env = FakeWorkoutEnvironment()
        vm = makeViewModel(listOf(exercise(sets = 2)))

        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }
        vm.onRepsChanged("5")
        vm.completeSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        vm.onBackNavigation()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 1 }
        vm.skipSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        val log = env.historyLogDao.logs.find { it.setNumber == 1 }!!
        assertEquals(0f, log.weightKg)
        assertEquals(0, log.reps)
    }

    @Test
    fun `forward navigation keeps registered logs and skips unregistered sets`() = runTest {
        env = FakeWorkoutEnvironment()
        vm = makeViewModel(listOf(exercise(sets = 3)))

        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }
        vm.onRepsChanged("5")
        vm.completeSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        // Set 2 is not registered — navigating forward skips it without a log
        vm.onForwardNavigation()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 3 }

        // Set 3 is the last set — navigating forward finishes the workout
        vm.onForwardNavigation()
        awaitUntil { vm.state.value is WorkoutScreenState.Summary }

        val logs = env.historyLogDao.logs
        assertEquals(1, logs.size)
        assertEquals(1, logs[0].setNumber)
    }

    @Test
    fun `progress sheet marks completed, current and pending sets`() = runTest {
        env = FakeWorkoutEnvironment()
        vm = makeViewModel(listOf(exercise(sets = 3)))

        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }
        vm.onRepsChanged("5")
        vm.completeSet()
        awaitUntil { (vm.state.value as? WorkoutScreenState.ActiveSet)?.currentSet == 2 }

        val sets = vm.buildProgressItems()[0].sets
        assertEquals(SetStatus.COMPLETED, sets[0].status)
        assertEquals(SetStatus.CURRENT, sets[1].status)
        assertEquals(SetStatus.PENDING, sets[2].status)
    }

    @Test
    fun `first set is prefilled with best reps of the previous session`() = runTest {
        env = FakeWorkoutEnvironment()
        // A previous session of the same workout already in the database
        val workoutId = env.repository.insertWorkout(Workout(name = "Bench Day", orderIndex = 0))
        val exerciseId = env.repository.insertExercise(
            Exercise(workoutId = workoutId, name = "Bench", numberOfSets = 1, restInSeconds = 1, orderIndex = 0)
        )
        env.historyLogDao.insertLog(
            HistoryLog(exerciseId = exerciseId, workoutId = workoutId, setNumber = 1, weightKg = 80f, reps = 8)
        )

        vm = ActiveWorkoutViewModel(env.repository, workoutId, TestRestSettingsProvider())
        awaitUntil { vm.state.value is WorkoutScreenState.ActiveSet }
        assertEquals("8", (vm.state.value as WorkoutScreenState.ActiveSet).reps)
    }
}
