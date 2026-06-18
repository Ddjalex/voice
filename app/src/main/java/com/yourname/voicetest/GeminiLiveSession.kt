package com.yourname.voicetest

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveSession(
    private val token: String,
    private val onAudioChunk: (String) -> Unit,
    private val onInterrupted: () -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onStatusChange: (String) -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/" +
                "google.ai.generativelanguage.v1alpha.GenerativeService" +
                ".BidiGenerateContentConstrained?access_token=$token"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onStatusChange("Connected — listening…")
                val setupMessage = JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("model", "models/gemini-3.1-flash-live-preview")
                        put("responseModalities", JSONArray().put("AUDIO"))
                    })
                }
                webSocket.send(setupMessage.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val serverContent = json.optJSONObject("serverContent") ?: return

                    if (serverContent.optBoolean("interrupted", false)) {
                        onInterrupted()
                    }

                    val modelTurn = serverContent.optJSONObject("modelTurn")
                    val parts = modelTurn?.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val audioData = part.optJSONObject("inlineData")?.optString("data")
                            if (!audioData.isNullOrEmpty()) {
                                onAudioChunk(audioData)
                            }
                        }
                    }

                    if (serverContent.optBoolean("turnComplete", false)) {
                        onTurnComplete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStatusChange("Error: ${t.message}")
                t.printStackTrace()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatusChange("Disconnected")
            }
        })
    }

    fun sendAudioChunk(base64Chunk: String) {
        val message = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("audio", JSONObject().apply {
                    put("data", base64Chunk)
                    put("mimeType", "audio/pcm;rate=16000")
                })
            })
        }
        webSocket?.send(message.toString())
    }

    fun close() {
        webSocket?.close(1000, "Session ended")
    }
}
