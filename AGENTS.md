# Strength-Tracker — AGENTS.md

## Repo layout

```
app/src/main/java/com/strengthtracker/
  MainActivity.kt          — entrypoint, sets up Compose navigation
  data/
    db/                    — Room entities, DAOs, database
    repository/            — single repository layer
  ui/
    screen/                — Compose screens (5 screens)
    viewmodel/             — ViewModel per screen (5 ViewModels)
  util/                    — CsvManager, SoundPlayer
```

## Tech stack

- **Compose** (Material 3, Compose BOM 2025.02.00)
- **Room** 2.7.0 with KSP annotation processing
- **Coroutines** for all async/Flow work
- **MinSdk 26** (Android 8.0+), compileSdk 35
- **JVM target 17**

## Build / run

Open in Android Studio and run. No CLI build needed in daily workflow.

Gradle versions in `gradle/libs.versions.toml` — keep AGP/Kotlin/KSP versions in sync when updating.

## KSP caveat

Room's `@Entity` and `@Dao` annotations require KSP to generate implementations. After adding a new entity/DAO, **sync Gradle** before running to regenerate code.

## Architecture notes

- Single-module app (`:app`). No library modules.
- `WorkoutRepository` is the sole repository — all Room DAOs are injected through it.
- No tests exist (no `test/` or `androidTest/`). Manual testing only.
- CSV import/export uses `CsvManager` in `util/`.

## Code style

- `kotlin.code.style=official` in `gradle.properties` — use `ktlint` conventions.
- Kotlin files: PascalCase for classes, camelCase for functions/variables.
