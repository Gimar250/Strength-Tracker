# Strength-Tracker — AGENTS.md

## Build / run

Open in Android Studio and run. No CLI build needed.

Gradle versions in `gradle/libs.versions.toml` — keep AGP/Kotlin/KSP in sync when updating.
Compose BOM version is in `app/build.gradle.kts` (not in libs.versions.toml).
DataStore preference dependency in `app/build.gradle.kts`.

## KSP caveat

Room's `@Entity` and `@Dao` require KSP to generate implementations. **Always sync Gradle** after adding a new entity/DAO before running.

## Tech stack

- Compose (Material 3, Compose BOM 2025.02.00)
- Navigation Compose 2.8.9
- Room 2.7.0 + KSP
- Coroutines + Flow
- DataStore preferences (settings persistence)
- MinSdk 26, compileSdk 35, JVM target 17

## Repo layout

```
app/src/main/java/com/strengthtracker/
  MainActivity.kt          — entrypoint, creates AppDatabase + WorkoutRepository, sets up Compose navigation, StrengthTrackerTheme
  data/
    db/
      AppDatabase.kt       — Room database singleton
      entity/              — 5 entities (Workout, WorkoutSession, Exercise, HistoryLog, ExerciseType)
      dao/                 — 4 DAOs (WorkoutDao, ExerciseDao, HistoryLogDao, WorkoutSessionDao)
    repository/
      WorkoutRepository.kt — sole repository, wraps all DAOs
  ui/
    screen/                — 7 screens (Home, History, ActiveWorkout, WorkoutSummary, EditRoutine, ExerciseHistory, Settings)
    viewmodel/             — 6 ViewModels (one per screen)
  util/
    CsvManager.kt          — CSV import/export
    SoundPlayer.kt         — rest timer beeps
    SettingsRepository.kt  — DataStore settings persistence
  theme/
    Color.kt               — DarkColors (dark theme), LightColors (light theme)
    Theme.kt               — StrengthTrackerTheme(themeMode, fontSizeSp) — reads settings via DataStore
    Type.kt                — AppTypography(fontSizeSp) — scales all font sizes proportionally
```

## Architecture notes

- Single-module app (`:app`). No library modules.
- `WorkoutRepository` is the sole repository — all DAOs injected through it.
- **No tests exist** — manual testing only.
- Navigation: MainActivity sets up NavHost with routes `home`, `history`, `workout/{id}`, `edit/{id}`, `exercise_history/{id}`, `settings`. Bottom nav only on `home`/`history`/`settings`.
- CSV import/export uses `CsvManager` in `util/`.
- Settings persisted via DataStore preferences (key `settings`), not SharedPreferences.

## Settings architecture

SettingsRepository stores all settings in DataStore. SettingsViewModel buffers changes in-memory and only persists to DataStore when `saveAllSettings()` is called. The SAVE button is in the Settings screen top bar.

Settings categories:
1. **Sound** — Beep Volume (0-100%), Beep Duration (200-1000ms), Prepare Beep toggle, End Beep toggle
2. **Workout Defaults** — Default Rest Timer (text, must be >0), Default Sets (text, must be >0), Default Exercise Type (Reps/Timed)
3. **Display** — Theme (System/Dark/Light), Font Size (12-24sp)
4. **Data** — Export Workouts CSV, Export Logs CSV, Import Workouts CSV, Clear All Data

Key files:
- `SettingsRepository.kt` — DataStore keys, Flows, and suspend setters
- `SettingsViewModel.kt` — In-memory UI state, `saveAllSettings()`, validation on save
- `SettingsScreen.kt` — Compose UI, text field labels, validation error messages
- `Theme.kt` — `StrengthTrackerTheme(themeMode, fontSizeSp)` reads from DataStore in MainActivity
- `Type.kt` — `AppTypography(fontSizeSp)` scales all font sizes by `fontSizeSp / 16f`

**Important:** Theme and font size changes propagate automatically through DataStore. No explicit wiring needed — MainActivity reads from DataStore and passes to `StrengthTrackerTheme`.

## ActiveWorkoutViewModel state machine (important)

`ActiveWorkoutViewModel` uses mutable fields (`currentExerciseIndex`, `currentSet`) and `sessionLogs` (in-memory list) as the single source of truth during a workout. All set states are tracked in-memory until `saveAndFinish()` persists.

Key patterns:
- **Navigation during workout**: Use `viewModelScope.launch` when calling `emitActiveSetState()` — it's a `suspend` function, never call it from non-suspend context.
- **Modifying existing sets**: Always find the existing `HistoryLog` first, then update it (don't insert a new one — that creates duplicates). Use the original `HistoryLog.id`.
- **Forward navigation on unregistered sets**: Only skip/remove the current set's log if it has NOT been registered yet. If already registered, just navigate without modifying.
- **Progress sheet**: `buildProgressItems()` derives set status from `sessionLogs` + navigation state (`backExerciseIndex`/`backSet`) to mark sets as PENDING after the navigation point.

## Code style

- `kotlin.code.style=official` in `gradle.properties` — use `ktlint` conventions.
- PascalCase for classes, camelCase for functions/variables.
