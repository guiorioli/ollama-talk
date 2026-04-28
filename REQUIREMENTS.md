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

### FR8 — Refresh conversation on key change
- When returning from Settings after updating the API key, the chat screen must refresh automatically
- The "missing key" message must disappear as soon as a valid key is saved

### FR9 — Cancel TTS playback
- A stop button must appear on the message bubble currently being read aloud
- A global stop button must appear in the top bar when TTS is active
- Clicking either stops TextToSpeech immediately

### FR10 — Auto-speak toggle icon
- When auto-speak is enabled: loudspeaker icon (as currently)
- When auto-speak is disabled: muted/loudspeaker-off icon

### FR11 — Markdown rendering
- LLM responses must be rendered as formatted Markdown in the chat
- Use `com.github.jeziellago:compose-markdown` library
- Before passing text to TTS, strip Markdown symbols so speech flows naturally

### FR12 — Auto-send on voice input
- After speech recognition transcribes the user's speech, the message is sent automatically
- No manual "send" click needed after voice input

### FR13 — Cancel voice input
- A cancel button must be visible during speech recognition
- Tapping it cancels the recording and discards the transcription

### FR14 — Conversation history drawer
- Hamburger menu icon on the top-left of the chat screen
- Tapping it opens a side drawer (ModalNavigationDrawer)
- Drawer items:
  - "New conversation" button at the top — clears current chat and starts fresh
  - List of past conversations with title (truncated first message) + timestamp
- Tapping a past conversation loads its full message history
- Conversations persist across app restarts
- Storage: individual JSON files per conversation in internal storage (`filesDir/conversations/`)
- Index file (`index.json`) stores only metadata (id, title, timestamp, model) for fast drawer loading

### FR15 — Image attachment in chat
- Paperclip/image icon next to the message input field
- Tapping it opens the device image picker (ActivityResultContracts)
- Selected image is displayed as a thumbnail in the input area (removable)
- On send, image is compressed and sent as base64 via the `images` field in Ollama `/api/chat`
- Image data is ephemeral (NOT persisted in conversation history)
- When saving a conversation (FR14), images are replaced by a `[Image]` placeholder in the message
- Chat bubble shows a visual indicator when a message had an attached image

### FR16 — Copy response button
- Copy icon button on each assistant message bubble
- Copies the plain text version of the response (markdown stripped) to the system clipboard
- Brief visual feedback after copying (icon change or toast)

## Non-Functional Requirements

### NFR1 — Build
- APK compiled locally with Android Studio
- Kotlin + Jetpack Compose
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

### NFR2 — Minimal external dependencies
- OkHttp for HTTP calls
- Gson for JSON serialization
- `com.github.jeziellago:compose-markdown` for Markdown rendering
- All other features (voice, TTS, storage) use native Android APIs

### NFR3 — Offline
- App does not work offline (depends on Ollama Cloud API)
- Friendly message when offline
