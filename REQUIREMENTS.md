# Ollama Talk — PRD

## Overview
Android app that connects to your Ollama Cloud account and enables chatting with selected cloud models. Supports voice input (speak questions) and audio output (hear responses via TTS).

## Functional Requirements

### FR1 — Authentication
- User manually enters an Ollama Cloud API Key
- Key is stored locally on the device (SharedPreferences)
- Shortcut button to open `https://ollama.com/settings/keys` in the browser
- Basic key validation before saving (test call to the API)

### FR2 — Chat with cloud models
- Send text messages to the Ollama Cloud API (`POST /api/chat`)
- Display conversation history in chat format
- Streaming disabled (`stream: false`) — waits for full response
- Default model: `gemma3:27b-cloud`

### FR3 — Model selector
- Dropdown/selector with available cloud models
- List fetched via API (`GET /api/tags`) or hardcoded known cloud models
- Selected model persists across sessions

### FR4 — Voice input
- "Speak" button triggers the native Android SpeechRecognizer
- Runtime permission request for RECORD_AUDIO (Android 6.0+)
- Transcription fills the message text field
- Visual feedback while recording

### FR5 — Audio output
- After receiving a model response, "Listen" button or automatic playback via TextToSpeech
- Play/stop control

### FR6 — Navigation
- Main screen: Chat
- Settings screen: API Key, default model

### FR7 — Auto-speak toggle
- Speaker icon in the top bar to toggle automatic TTS playback of responses
- When enabled, responses are read aloud immediately after receiving
- When disabled, responses can still be played manually per-message
- Visual indicator (highlighted/faded icon) showing toggle state

## Non-Functional Requirements

### NFR1 — Build
- APK compiled locally with Android Studio
- Kotlin + Jetpack Compose
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

### NFR2 — Minimal external dependencies
- OkHttp for HTTP calls
- Gson for JSON serialization
- All other features (voice, TTS, storage) use native Android APIs

### NFR3 — Offline
- App does not work offline (depends on Ollama Cloud API)
- Friendly message when offline
