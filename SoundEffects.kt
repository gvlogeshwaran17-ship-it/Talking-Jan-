package com.talkingjan.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

object SoundEffects {
    private const val SAMPLE_RATE = 22050

    private fun playSamples(samples: ShortArray) {
        thread(start = true) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE + 80L
            Thread.sleep(durationMs)
            track.stop()
            track.release()
        }
    }

    fun playBoing() {
        val n = (SAMPLE_RATE * 0.45).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 220.0 + 380.0 * sin(2.0 * PI * 9.0 * t) * exp(-3.5 * t)
            val env = exp(-4.0 * t)
            buf[i] = (sin(2.0 * PI * freq * t) * env * 24000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playGiggle() {
        val n = (SAMPLE_RATE * 0.65).toInt()
        val buf = ShortArray(n)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 659.25, 880.0)
        val seg = n / notes.size
        for (i in 0 until n) {
            val idx = minOf(i / seg, notes.size - 1)
            val localT = (i % seg).toDouble() / SAMPLE_RATE
            val env = sin(PI * (i % seg).toDouble() / seg)
            buf[i] = (sin(2.0 * PI * notes[idx] * localT) * env * 20000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playPurr() {
        val n = (SAMPLE_RATE * 0.8).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin(PI * t / 0.8)
            val carrier = sin(2.0 * PI * 140.0 * t) * (0.5 + 0.5 * sin(2.0 * PI * 22.0 * t))
            buf[i] = (carrier * env * 18000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playGulp() {
        val n = (SAMPLE_RATE * 0.5).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 180.0 + 420.0 * (t / 0.5)
            val env = sin(PI * t / 0.5)
            buf[i] = (sin(2.0 * PI * freq * t) * env * 22000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playCrunch() {
        val n = (SAMPLE_RATE * 0.4).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val pulse = if (((t * 14).toInt() % 2) == 0) 1.0 else 0.2
            val env = exp(-5.0 * t) * pulse
            buf[i] = ((Random.nextDouble() * 2.0 - 1.0) * env * 24000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playCymbal() {
        val n = (SAMPLE_RATE * 0.9).toInt()
        val buf = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-3.2 * t)
            val noise = Random.nextDouble() * 2.0 - 1.0
            val metallic = sin(2.0 * PI * 3200.0 * t) * 0.3 + sin(2.0 * PI * 4700.0 * t) * 0.3
            buf[i] = ((noise * 0.7 + metallic) * env * 24000).toInt().toShort()
        }
        playSamples(buf)
    }

    fun playDanceBeat() {
        val n = (SAMPLE_RATE * 1.4).toInt()
        val buf = ShortArray(n)
        val melody = doubleArrayOf(392.0, 440.0, 523.25, 587.33, 659.25, 587.33, 523.25, 659.25)
        val seg = n / melody.size
        for (i in 0 until n) {
            val idx = minOf(i / seg, melody.size - 1)
            val localT = (i % seg).toDouble() / SAMPLE_RATE
            val env = exp(-6.0 * localT)
            val tone = sin(2.0 * PI * melody[idx] * localT)
            buf[i] = (tone * env * 21000).toInt().toShort()
        }
        playSamples(buf)
    }
}
