package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object FeedbackHelper {

    fun triggerCompletionFeedback(
        context: Context,
        hapticEnabled: Boolean,
        soundEnabled: Boolean
    ) {
        if (hapticEnabled) {
            triggerHaptic(context, isHeavy = false)
        }
        if (soundEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            } catch (_: Exception) {
                // Ignore audio errors on devices/emulators without audio hardware
            }
        }
    }

    fun triggerHaptic(context: Context, isHeavy: Boolean = false) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                val duration = if (isHeavy) 35L else 18L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = if (isHeavy) 160 else 90
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
