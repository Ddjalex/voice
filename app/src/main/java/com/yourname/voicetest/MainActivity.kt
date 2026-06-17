package com.yourname.voicetest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ── Configure these before building ─────────────────────────────────────
    // Set to your deployed Replit backend URL and the APP_SHARED_SECRET you configured
    private val backendUrl = "https://92e53d0a-c1ce-4d64-82c1-549a7ce0a059-00-mi33ong3v0vi.picard.replit.dev"
    private val appSecret  = "k8Hn3xQwP9vYmT4cRz7LbN2eXjF6sUaD"
    // ────────────────────────────────────────────────────────────────────────

    private var micCapture:    MicCapture?        = null
    private var audioPlayback: AudioPlayback?     = null
    private var session:       GeminiLiveSession? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startConversation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var status   by remember { mutableStateOf("Idle — tap Start to begin") }
            var isActive by remember { mutableStateOf(false) }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary   = Color(0xFF00BCD4),
                    surface   = Color(0xFF0D1117),
                    background = Color(0xFF0D1117),
                    onBackground = Color(0xFFE6EDF3)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier              = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.Center
                    ) {
                        Text(
                            text       = "Gemini Voice Test",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 22.sp,
                            color      = Color(0xFF00BCD4)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text     = "Real-time speech-to-speech",
                            fontSize = 13.sp,
                            color    = Color(0xFF8B949E)
                        )
                        Spacer(modifier = Modifier.height(48.dp))

                        Surface(
                            shape  = MaterialTheme.shapes.medium,
                            color  = Color(0xFF161B22),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text     = status,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 14.sp,
                                color    = if (isActive) Color(0xFF3FB950) else Color(0xFF8B949E),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = {
                                if (!isActive) {
                                    status = "Connecting…"
                                    isActive = true
                                    checkPermissionAndStart { status = it }
                                } else {
                                    stopConversation()
                                    status = "Idle — tap Start to begin"
                                    isActive = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActive) Color(0xFFDA3633) else Color(0xFF00BCD4),
                                contentColor   = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                text       = if (isActive) "End Conversation" else "Start Conversation",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text     = "Just talk — Gemini listens continuously.\nInterrupt it any time by speaking.",
                            fontSize = 12.sp,
                            color    = Color(0xFF484F58),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissionAndStart(setStatus: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            setStatus("Fetching token…")
            startConversation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startConversation() {
        lifecycleScope.launch {
            try {
                val token = TokenFetcher(backendUrl, appSecret).fetchToken()

                audioPlayback = AudioPlayback().also { it.start() }

                session = GeminiLiveSession(
                    token         = token,
                    onAudioChunk  = { chunk -> audioPlayback?.enqueueBase64Chunk(chunk) },
                    onInterrupted = { audioPlayback?.clearQueueForInterruption() },
                    onTurnComplete = { /* queue plays out naturally */ }
                )
                session?.connect()

                micCapture = MicCapture { chunk -> session?.sendAudioChunk(chunk) }
                micCapture?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopConversation() {
        micCapture?.stop()
        audioPlayback?.stop()
        session?.close()
        micCapture    = null
        audioPlayback = null
        session       = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConversation()
    }
}
