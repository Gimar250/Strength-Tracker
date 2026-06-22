package com.strengthtracker.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Thin wrapper around ToneGenerator.
 * No external files, no permissions needed — uses the device's built-in tone engine.
 */
object SoundPlayer {

    private const val DEFAULT_VOLUME = 100 // Max volume (range: 0–100)
    private const val DEFAULT_BEEP_DURATION_MS = 400

    fun playRestEndBeep(
        volume: Int = DEFAULT_VOLUME,
        beepDurationMs: Int = DEFAULT_BEEP_DURATION_MS,
        toneType: Int = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
        enabled: Boolean = true
    ) {
        if (!enabled) return
        try {
            // STREAM_ALARM ensures the beep is audible even in silent mode
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, volume)
            toneGen.startTone(toneType, beepDurationMs)

            // Release after the tone finishes to avoid resource leaks
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (beepDurationMs + 100).toLong())

        } catch (e: Exception) {
            // ToneGenerator can throw on some devices/emulators — fail silently
            e.printStackTrace()
        }
    }

    fun playRestPrepareBeep(
        volume: Int = DEFAULT_VOLUME,
        beepDurationMs: Int = DEFAULT_BEEP_DURATION_MS,
        toneType: Int = ToneGenerator.TONE_PROP_BEEP,
        enabled: Boolean = true
    ) {
        if (!enabled) return
        try {
            // STREAM_ALARM ensures the beep is audible even in silent mode
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, volume)

            // TONE_PROP_BEEP genera un único pitido estándar del sistema
            toneGen.startTone(toneType, beepDurationMs)

            // Se liberan los recursos justo después de que termine de sonar
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (beepDurationMs + 100).toLong())

        } catch (e: Exception) {
            // Evita cierres inesperados en dispositivos que no soportan ToneGenerator
            e.printStackTrace()
        }
    }
}
