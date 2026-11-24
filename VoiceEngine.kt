package com.talkingjan.app

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import kotlin.math.abs

class VoiceEngine(
    private val onStateChanged: (State, Float) -> Unit
) {
    enum class State { IDLE, LISTENING, SPEAKING }

    @Volatile
    private var running = false
    private val recordSampleRate = 16000
    private val playbackSampleRate = (recordSampleRate * 1.48f).toInt()

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        thread(start = true, name = "TalkingJanVoiceLoop") {
            val minBuf = AudioRecord.getMinBufferSize(
                recordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                recordSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf * 2, 4096)
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                running = false
                return@thread
            }

            val buffer = ShortArray(1024)
            val recordedPcm = ByteArrayOutputStream()
            var isRecordingSpeech = false
            var silenceFrames = 0
            val speechThreshold = 1400
            val maxSilenceFrames = 14
            val maxRecordFrames = 120
            var speechFrames = 0

            recorder.startRecording()
            onStateChanged(State.IDLE, 0f)

            try {
                while (running) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue

                    var sum = 0L
                    for (i in 0 until read) sum += abs(buffer[i].toInt())
                    val avgAmp = (sum / read).toInt()
                    val normAmp = (avgAmp / 12000f).coerceIn(0f, 1f)

                    if (!isRecordingSpeech) {
                        if (avgAmp > speechThreshold) {
                            isRecordingSpeech = true
                            silenceFrames = 0
                            speechFrames = 0
                            recordedPcm.reset()
                            onStateChanged(State.LISTENING, normAmp)
                        }
                    }

                    if (isRecordingSpeech) {
                        for (i in 0 until read) {
                            val s = buffer[i].toInt()
                            recordedPcm.write(s and 0xFF)
                            recordedPcm.write((s shr 8) and 0xFF)
                        }
                        speechFrames++
                        onStateChanged(State.LISTENING, normAmp)

                        if (avgAmp < speechThreshold * 0.7) silenceFrames++ else silenceFrames = 0

                        if (silenceFrames >= maxSilenceFrames || speechFrames >= maxRecordFrames) {
                            isRecordingSpeech = false
                            val audioBytes = recordedPcm.toByteArray()
                            if (speechFrames > 6) {
                                recorder.stop()
                                playPitchShifted(audioBytes)
                                if (running) {
                                    recorder.startRecording()
                                    onStateChanged(State.IDLE, 0f)
                                }
                            } else {
                                onStateChanged(State.IDLE, 0f)
                            }
                        }
                    }
                }
            } finally {
                try {
                    recorder.stop()
                    recorder.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun playPitchShifted(pcmBytes: ByteArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            playbackSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(playbackSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, 4096))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.play()
        val chunkSize = 1024
        var offset = 0
        while (running && offset < pcmBytes.size) {
            val len = minOf(chunkSize, pcmBytes.size - offset)
            track.write(pcmBytes, offset, len)
            var sum = 0L
            var i = offset
            while (i + 1 < offset + len) {
                val sample = (pcmBytes[i].toInt() and 0xFF) or (pcmBytes[i + 1].toInt() shl 8)
                sum += abs(sample.toShort().toInt())
                i += 2
            }
            val norm = ((sum / maxOf(1, len / 2)) / 10000f).coerceIn(0f, 1f)
            onStateChanged(State.SPEAKING, norm)
            offset += len
        }
        track.stop()
        track.release()
    }

    fun stop() {
        running = false
    }
}
