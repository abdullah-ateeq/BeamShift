package com.example.beamshift.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class GameSoundManager {

    private val sampleRate = 22050
    private var isMuted = false

    // Pre-generated PCM samples for zero-latency playback
    private val shiftAudioTrack: AudioTrack? by lazy { createStaticTrack(generateShiftSamples()) }
    private val passAudioTrack: AudioTrack? by lazy { createStaticTrack(generatePassSamples()) }
    private val gameOverAudioTrack: AudioTrack? by lazy { createStaticTrack(generateGameOverSamples()) }
    private val clickAudioTrack: AudioTrack? by lazy { createStaticTrack(generateClickSamples()) }

    fun setSoundEnabled(enabled: Boolean) {
        isMuted = !enabled
    }

    fun playShift() {
        if (isMuted) return
        playTrack(shiftAudioTrack)
    }

    fun playPass() {
        if (isMuted) return
        playTrack(passAudioTrack)
    }

    fun playGameOver() {
        if (isMuted) return
        playTrack(gameOverAudioTrack)
    }

    fun playClick() {
        if (isMuted) return
        playTrack(clickAudioTrack)
    }

    private fun playTrack(track: AudioTrack?) {
        try {
            track?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.reloadStaticData()
                it.play()
            }
        } catch (e: Exception) {
            Log.w("GameSoundManager", "Error playing sound: ${e.message}")
        }
    }

    private fun createStaticTrack(samples: ShortArray): AudioTrack? {
        return try {
            val bufferSize = samples.size * 2
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val track = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(samples, 0, samples.size)
            track
        } catch (e: Exception) {
            Log.w("GameSoundManager", "Failed to create AudioTrack: ${e.message}")
            null
        }
    }

    private fun generateShiftSamples(): ShortArray {
        // Quick futuristic sweep: 480Hz -> 760Hz in 60ms
        val duration = 0.065
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            // Frequency modulation
            val freq = 480.0 + (760.0 - 480.0) * progress
            // Envelope: quick attack, smooth exponential decay
            val envelope = (1.0 - progress) * (1.0 - exp(-progress * 25.0))
            val angle = 2.0 * PI * freq * t
            val sample = (sin(angle) * envelope * 0.75 * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generatePassSamples(): ShortArray {
        // Rewarding harmonious chime: 880Hz + 1320Hz with soft sparkle (140ms)
        val duration = 0.14
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            val envelope = exp(-progress * 4.2)
            val tone1 = sin(2.0 * PI * 880.0 * t)
            val tone2 = sin(2.0 * PI * 1320.0 * t) * 0.5
            val tone3 = sin(2.0 * PI * 1760.0 * t) * 0.25
            val combined = (tone1 + tone2 + tone3) / 1.75
            val sample = (combined * envelope * 0.85 * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateGameOverSamples(): ShortArray {
        // Heavy low saw shockwave: 190Hz down to 65Hz (280ms)
        val duration = 0.28
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            val freq = 190.0 * (1.0 - progress * 0.65)
            val envelope = exp(-progress * 3.5)
            // Distorted wave
            val angle = 2.0 * PI * freq * t
            val tone = sin(angle) + 0.3 * sin(2.0 * angle) + 0.2 * sin(3.0 * angle)
            val sample = (tone * envelope * 0.9 * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateClickSamples(): ShortArray {
        // 35ms subtle interface tap
        val duration = 0.035
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            val envelope = exp(-progress * 15.0)
            val sample = (sin(2.0 * PI * 650.0 * t) * envelope * 0.6 * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    fun release() {
        listOf(shiftAudioTrack, passAudioTrack, gameOverAudioTrack, clickAudioTrack).forEach {
            try {
                it?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
