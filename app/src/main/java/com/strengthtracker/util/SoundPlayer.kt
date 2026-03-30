package com.strengthtracker.util

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Thin wrapper around ToneGenerator.
 * No external files, no permissions needed — uses the device's built-in tone engine.
 */
object SoundPlayer {

    private const val VOLUME = 100 // Max volume (range: 0–100)
    private const val BEEP_DURATION_MS = 400

    fun playRestEndBeep() {
        try {
            // STREAM_ALARM ensures the beep is audible even in silent mode
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, VOLUME)
            toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_DURATION_MS)

            // Release after the tone finishes to avoid resource leaks
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (BEEP_DURATION_MS + 100).toLong())

        } catch (e: Exception) {
            // ToneGenerator can throw on some devices/emulators — fail silently
            e.printStackTrace()
        }
    }
}
