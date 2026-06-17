# Gemini Voice Test — Native Android

A standalone real-time voice conversation app that streams your mic to Gemini Live and plays back the response — no push-to-talk, no delay. Tap "Start Conversation" and just talk.

This app exists to solve the real-time audio streaming problem in isolation before wiring it into the call-answering bot. Once it works smoothly here, `MicCapture.kt`, `AudioPlayback.kt`, `GeminiLiveSession.kt`, and `TokenFetcher.kt` all move straight into the main call-bot app.

## Before building — set your backend URL

Open `app/src/main/java/com/yourname/voicetest/MainActivity.kt` and update two lines at the top of the class:

```kotlin
private val backendUrl = "https://your-backend.replit.app"  // ← your deployed Replit URL
private val appSecret  = "your-app-shared-secret"           // ← your APP_SHARED_SECRET
```

## Option A — Build with GitHub Actions (no local computer needed)

1. Create a new GitHub repository
2. Upload this entire `voice-test-native/` folder to it (drag-and-drop in the GitHub web UI)
3. Push — the Actions workflow (`.github/workflows/android-build.yml`) triggers automatically
4. Wait ~3–5 minutes → go to the **Actions** tab → click the latest run → scroll to **Artifacts** → download `app-debug.zip`
5. Unzip → transfer `app-debug.apk` to your phone (email, Google Drive, etc.)
6. On your phone: Settings → install unknown apps → allow → tap the APK to install

## Option B — Build with Android Studio

1. Open this `voice-test-native/` folder in Android Studio (File → Open)
2. Let Gradle sync complete
3. Connect your Android phone via USB
4. Run → Run 'app'

## What to listen for when testing

- Does Gemini respond quickly (~300ms) after you stop talking?
- Is the playback smooth and continuous, or choppy?
- If you start talking while Gemini is still speaking, does it stop immediately?
- Does it switch naturally between Amharic and English without configuring anything?

## Architecture

```
Mic (16kHz PCM)
    │ MicCapture.kt
    ▼
WebSocket ──── GeminiLiveSession.kt ────> Gemini Live API
                        │
                        │ audio chunks (24kHz PCM, base64)
                        ▼
               AudioPlayback.kt (queue + AudioTrack MODE_STREAM)
                        │
                      Speaker
```

## Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Compose UI: Start/Stop button, status, permission request |
| `MicCapture.kt` | AudioRecord loop — captures 16kHz PCM, sends base64 chunks |
| `AudioPlayback.kt` | AudioTrack streaming — queue-based, interruption-aware |
| `GeminiLiveSession.kt` | OkHttp WebSocket client for Gemini Live API |
| `TokenFetcher.kt` | Calls `/api/app/mint-token` on your backend |
