# Strength-Tracker — AGENTS.md

## Repo layout

```
app/src/main/java/com/strengthtracker/
  MainActivity.kt          — entrypoint, creates AppDatabase + WorkoutRepository, sets up Compose navigation
  data/
    db/
      AppDatabase.kt       — Room database singleton
      entity/              — 5 entities (Workout, WorkoutSession, Exercise, HistoryLog, ExerciseType)
      dao/                 — 4 DAOs (WorkoutDao, ExerciseDao, HistoryLogDao, WorkoutSessionDao)
    repository/
      WorkoutRepository.kt — sole repository, wraps all DAOs
  ui/
    screen/                — 6 screens (Home, History, ActiveWorkout, WorkoutSummary, EditRoutine, ExerciseHistory)
    viewmodel/             — 5 ViewModels (one per screen)
  util/
    CsvManager.kt          — CSV import/export
    SoundPlayer.kt         — rest timer beeps
```

## Tech stack

- **Compose** (Material 3, Compose BOM 2025.02.00)
- **Navigation Compose** 2.8.9
- **Room** 2.7.0 + KSP annotation processing
- **Coroutines** for all async/Flow work
- **MinSdk 26**, compileSdk 35, JVM target 17

## Build / run

Open in Android Studio and run. No CLI build needed.

Gradle versions in `gradle/libs.versions.toml` — keep AGP/Kotlin/KSP in sync when updating.
Compose BOM version is in `app/build.gradle.kts` (not in libs.versions.toml).

## KSP caveat

Room's `@Entity` and `@Dao` require KSP to generate implementations. **Always sync Gradle** after adding a new entity/DAO before running.

## Architecture notes

- Single-module app (`:app`). No library modules.
- `WorkoutRepository` is the sole repository — all DAOs injected through it.
- **No tests exist** — manual testing only.
- Navigation: MainActivity sets up NavHost with routes `home`, `history`, `workout/{id}`, `edit/{id}`, `exercise_history/{id}`. Bottom nav only on `home`/`history`.
- CSV import/export uses `CsvManager` in `util/`.

## Code style

- `kotlin.code.style=official` in `gradle.properties` — use `ktlint` conventions.
- PascalCase for classes, camelCase for functions/variables.

## ActiveWorkoutViewModel state machine (important)

`ActiveWorkoutViewModel` uses mutable fields (`currentExerciseIndex`, `currentSet`) and `sessionLogs` (in-memory list) as the single source of truth during a workout. All set states are tracked in-memory until `saveAndFinish()` persists.

Key patterns:
- **Navigation during workout**: Use `viewModelScope.launch` when calling `emitActiveSetState()` — it's a `suspend` function, never call it from non-suspend context.
- **Modifying existing sets**: Always find the existing `HistoryLog` first, then update it (don't insert a new one — that creates duplicates). Use the original `HistoryLog.id`.
- **Forward navigation on unregistered sets**: Only skip/remove the current set's log if it has NOT been registered yet. If already registered, just navigate without modifying.
- **Progress sheet**: `buildProgressItems()` derives set status from `sessionLogs` + navigation state (`backExerciseIndex`/`backSet`) to mark sets as PENDING after the navigation point.
