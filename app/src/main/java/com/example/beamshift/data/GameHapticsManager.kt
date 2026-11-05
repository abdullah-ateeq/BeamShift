package com.example.beamshift.data

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class GameHapticsManager(private val context: Context) {

    private var isEnabled = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun vibrateShift() {
        if (!isEnabled) return
        vibrate(durationMs = 15, amplitude = 90)
    }

    fun vibratePass() {
        if (!isEnabled) return
        vibrate(durationMs = 35, amplitude = 180)
    }

    fun vibrateCollision() {
        if (!isEnabled) return
        vibratePattern(longArrayOf(0, 70, 40, 110), intArrayOf(0, 255, 0, 255))
    }

    fun vibrateButton() {
        if (!isEnabled) return
        vibrate(durationMs = 12, amplitude = 70)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val clampedAmp = amplitude.coerceIn(1, 255)
                    v.vibrate(VibrationEffect.createOneShot(durationMs, clampedAmp))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // gracefully fallback if vibration fails
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            vibrator?.let { v ->
                if (!v.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            // gracefully fallback
        }
    }
}
