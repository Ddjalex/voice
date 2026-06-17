package com.yourname.voicetest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class AudioPlayback {

    private val sampleRate = 24000
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val audioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        bufferSize,
        AudioTrack.MODE_STREAM,
        AudioTrack.SESSION_ID_GENERATE
    )

    private val chunkQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackJob: Job? = null
    private var isPlaying = false

    fun start() {
        audioTrack.play()
        isPlaying = true

        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            while (isPlaying) {
                val chunk = chunkQueue.poll()
                if (chunk != null) {
                    audioTrack.write(chunk, 0, chunk.size)
                } else {
                    delay(10)
                }
            }
        }
    }

    fun enqueueBase64Chunk(base64Data: String) {
        val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
        chunkQueue.add(bytes)
    }

    /** Call immediately when Gemini sends an "interrupted" signal — stops AI voice instantly. */
    fun clearQueueForInterruption() {
        chunkQueue.clear()
        audioTrack.flush()
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        chunkQueue.clear()
        audioTrack.stop()
        audioTrack.release()
    }
}
