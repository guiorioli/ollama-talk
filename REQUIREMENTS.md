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
- Default model: `gemma4:31b`

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
- Target SDK: 36 (Android 16)

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

### FR17 — Copy entire conversation
- Copy icon button in the chat top bar (next to the hamburger menu or inside an overflow/menu)
- Copies the full conversation history to the system clipboard as plain text
- Each message prefixed with a separator indicating the speaker:
  - User messages: `--- User`
  - Assistant messages: `--- Ollama (<model-name>)`
- Markdown is stripped from the copied text so it reads naturally
- Brief visual feedback after copying (icon change or toast)

### FR18 — Donate button
- "Buy me a coffee!" button in the Settings screen, below the Save button
- Opens the Liberapay donation page (`https://liberapay.com/gui.orioli/donate`) in the device's default browser
- Uses the same pattern as the existing "Get API Key" external link button

### FR19 — Cancel ongoing request
- A cancel button appears next to the "Thinking…" indicator inside the assistant loading bubble
- Tapping it immediately cancels the in-flight HTTP request and removes the loading bubble
- The user's message remains in the chat history
- Visual feedback: the loading bubble disappears and the input field becomes enabled again

### FR20 — Web Search Tool Calling
- User can enable "web search" in Settings as a toggle switch
- When enabled, the Ollama API receives a `tools` array with a `web_search` function definition
- The model decides independently whether a user query requires a web search
- If the model calls the tool, the app executes a `POST /api/web_search` call with the query
- Results are formatted and sent back as a `role: "tool"` message in the conversation history
- The model then generates a final response based on the search results
- Maximum 3 tool call iterations per message to prevent infinite loops
- Visual indicators show "Searching the web..." and "Analyzing results..." during tool execution
- Tool calling is disabled by default to avoid unnecessary latency
- Model compatibility: hardcoded list of known cloud models that support tools
- For models outside the list, a scraping check against `https://ollama.com/search?c=cloud&c=tools` is performed only when the user attempts to enable the toggle
- If the scraping check fails, a dialog asks the user if they want to try anyway
- Messages with `role: "tool"` and `tool_calls` are persisted in conversation history
- Tool call messages are displayed as discrete bubbles in the chat UI (e.g., "🔍 Searched: 'query'")
- TTS does not read tool messages aloud

### FR21 — TTS Language Expansion
- Expand the TTS Language selector from 5 to 14 languages
- New languages: Mandarin (zh-CN), Hindi (hi-IN), Arabic (ar-SA), Russian (ru-RU), Japanese (ja-JP), Korean (ko-KR), Italian (it-IT), Turkish (tr-TR), Dutch (nl-NL)
- All languages are hardcoded in `TtsLanguage.kt` and used by both SpeechRecognizer and TextToSpeech
- Fallback locale logic remains unchanged (`fromCode` defaults to English)

### FR22 — UI Localization
- Extract all user-facing strings from Composables (`ChatScreen`, `SettingsScreen`) into `res/values/strings.xml`
- Create translated `strings.xml` files for: Italian (`values-it`), Russian (`values-ru`), Dutch (`values-nl`), Japanese (`values-ja`), Korean (`values-ko`), Mandarin Chinese (`values-zh-rCN`)
- English serves as the universal fallback when the device locale does not match any translated language
- The app UI follows the Android system locale automatically; TTS language selection remains independent (FR21)

### FR23 — Smart auto-scroll
- During streaming, the chat auto-scrolls to the bottom only if the user is already at the bottom
- If the user scrolls up to read, auto-scroll stops immediately
- A floating action button (FAB) with a down-arrow appears when the user is not at the bottom
- Tapping the FAB scrolls the chat back to the latest message
- The FAB disappears once the user is back at the bottom

### FR24 — Donate button visual highlight
- The "Buy me a coffee!" button in Settings is ~20% larger than a standard button
- The text color cycles through eye-catching colors (red → orange → yellow → black) every ~2.5s
- Animation is subtle and looped continuously to draw attention without being intrusive

### FR25 — Hybrid voice input (press-and-hold + tap toggle)
- **Press and hold** the mic button: starts listening immediately; stops and sends when the user releases
- **Quick tap** (under 300ms): starts listening and switches to toggle mode (Stop icon appears); user taps Stop to finish and send
- `onEndOfSpeech` is suppressed so the recognizer does not auto-stop on speech pauses
- Hold threshold: 300ms distinguishes tap from hold

## Non-Functional Requirements

### NFR1 — Build
- APK compiled locally with Android Studio
- Kotlin + Jetpack Compose
- Min SDK: 26 (Android 8.0)
- Target SDK: 36 (Android 16)

### NFR2 — Minimal external dependencies
- OkHttp for HTTP calls
- Gson for JSON serialization
- `com.github.jeziellago:compose-markdown` for Markdown rendering
- All other features (voice, TTS, storage) use native Android APIs

### NFR3 — Offline
- App does not work offline (depends on Ollama Cloud API)
- Friendly message when offline
