package com.strengthtracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SettingsRepository {

    // ── Defaults ───────────────────────────────────────────────────────────────
    const val DEFAULT_VOLUME = 100
    const val DEFAULT_BEEP_DURATION = 400
    const val DEFAULT_PREPARE_BEEP = true
    const val DEFAULT_END_BEEP = true
    const val DEFAULT_REST = 90
    const val DEFAULT_SETS = 3
    const val DEFAULT_EXERCISE_TYPE = "REPS"
    const val DEFAULT_THEME = "system"
    const val DEFAULT_FONT_SIZE = 16

    // ── Sound ──────────────────────────────────────────────────────────────────
    private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "settings")

    private val VOLUME_KEY = intPreferencesKey("beep_volume")
    private val BEEP_DURATION_KEY = intPreferencesKey("beep_duration_ms")
    private val PREPARE_BEEP_KEY = booleanPreferencesKey("prepare_beep_enabled")
    private val END_BEEP_KEY = booleanPreferencesKey("end_beep_enabled")

    // ── Workout defaults ───────────────────────────────────────────────────────
    private val DEFAULT_REST_KEY = intPreferencesKey("default_rest_seconds")
    private val DEFAULT_SETS_KEY = intPreferencesKey("default_number_of_sets")
    private val DEFAULT_EXERCISE_TYPE_KEY = stringPreferencesKey("default_exercise_type")

    // ── Display ────────────────────────────────────────────────────────────────
    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val FONT_SIZE_KEY = intPreferencesKey("font_size_sp")

    // ── Sound ──────────────────────────────────────────────────────────────────
    fun volumeFlow(context: Context): Flow<Int> = context.dataStore.data.map { it[VOLUME_KEY] ?: DEFAULT_VOLUME }
    fun beepDurationFlow(context: Context): Flow<Int> = context.dataStore.data.map { it[BEEP_DURATION_KEY] ?: DEFAULT_BEEP_DURATION }
    fun prepareBeepEnabledFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { it[PREPARE_BEEP_KEY] ?: DEFAULT_PREPARE_BEEP }
    fun endBeepEnabledFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { it[END_BEEP_KEY] ?: DEFAULT_END_BEEP }

    suspend fun setVolume(context: Context, volume: Int) { context.dataStore.edit { it[VOLUME_KEY] = volume } }
    suspend fun setBeepDuration(context: Context, duration: Int) { context.dataStore.edit { it[BEEP_DURATION_KEY] = duration } }
    suspend fun setPrepareBeepEnabled(context: Context, enabled: Boolean) { context.dataStore.edit { it[PREPARE_BEEP_KEY] = enabled } }
    suspend fun setEndBeepEnabled(context: Context, enabled: Boolean) { context.dataStore.edit { it[END_BEEP_KEY] = enabled } }

    // ── Workout defaults ───────────────────────────────────────────────────────
    fun defaultRestFlow(context: Context): Flow<Int> = context.dataStore.data.map { it[DEFAULT_REST_KEY] ?: DEFAULT_REST }
    fun defaultSetsFlow(context: Context): Flow<Int> = context.dataStore.data.map { it[DEFAULT_SETS_KEY] ?: DEFAULT_SETS }
    fun defaultExerciseTypeFlow(context: Context): Flow<String> = context.dataStore.data.map { it[DEFAULT_EXERCISE_TYPE_KEY] ?: DEFAULT_EXERCISE_TYPE }

    suspend fun setDefaultRest(context: Context, seconds: Int) { context.dataStore.edit { it[DEFAULT_REST_KEY] = seconds } }
    suspend fun setDefaultSets(context: Context, sets: Int) { context.dataStore.edit { it[DEFAULT_SETS_KEY] = sets } }
    suspend fun setDefaultExerciseType(context: Context, type: String) { context.dataStore.edit { it[DEFAULT_EXERCISE_TYPE_KEY] = type } }

    // ── Display ────────────────────────────────────────────────────────────────
    fun themeModeFlow(context: Context): Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: DEFAULT_THEME }
    fun fontSizeFlow(context: Context): Flow<Int> = context.dataStore.data.map { it[FONT_SIZE_KEY] ?: DEFAULT_FONT_SIZE }

    suspend fun setThemeMode(context: Context, mode: String) { context.dataStore.edit { it[THEME_KEY] = mode } }
    suspend fun setFontSize(context: Context, sp: Int) { context.dataStore.edit { it[FONT_SIZE_KEY] = sp } }
}
