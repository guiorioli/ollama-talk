# Ollama Talk

Android app for chatting with Ollama Cloud models, with voice support, conversation history, image attachments, and rich Markdown rendering.

## Download

### Google Play Store (Closed Testing)

The app is currently in **closed testing** on the Google Play Store and should be publicly available on Play Store **soon**.

### Pre-built APK

The latest compiled APK is also available at:

```
dist/ollama-talk.apk
```

Just copy it to your Android device and install. No build required.

---

## Features

- **Chat with Ollama Cloud models** using your personal API key
- **Real-time streaming** — assistant responses appear word-by-word as they are generated
- **Web search tool calling** — let the AI search the internet for current information with visual indicators
- **Conversation history** — save, rename, delete, and switch between multiple chat threads
- **Voice input** — speak your messages using Android SpeechRecognizer
- **Voice output** — listen to assistant responses via TextToSpeech, with auto-speak toggle
- **Image attachments** — send photos to multimodal models (compressed and Base64-encoded)
- **Rich Markdown rendering** — including code blocks, LaTeX math, HTML entities, and tables
- **Copy messages** — copy individual messages or the entire conversation (Markdown stripped)
- **Text selection** — select and copy partial text from assistant responses
- **Cancel requests** — stop an in-flight assistant response at any time
- **Dark & light themes** — follows the system theme automatically
- **TTS language selector** — 14 supported languages
- **UI localization** — English, Portuguese (BR), Italian, Russian, Dutch, Japanese, Korean, Mandarin Chinese
- **Runtime microphone permission** — requested dynamically on Android 6+

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| HTTP | OkHttp |
| JSON | Gson |
| Markdown | compose-markdown (JitPack) |
| Voice (input) | SpeechRecognizer (Android SDK) |
| Voice (output) | TextToSpeech (Android SDK) |
| Settings Storage | SharedPreferences |
| Conversation Storage | JSON files in app-private directory |
| Min SDK | 26 |
| Target SDK | 35 |
| Compile SDK | 35 |

## Project Structure

```
app/src/main/java/com/guiorioli/ollamatalk/
├── MainActivity.kt
├── OllamaTalkApp.kt
├── audio/
│   ├── SpeechRecognizerManager.kt
│   ├── StreamingTtsManager.kt
│   └── TextToSpeechManager.kt
├── data/
│   ├── api/
│   │   ├── ChatModels.kt          (ChatMessage, ChatRequest, ChatResponse, ChatStreamEvent, Tool, ModelInfo, TagsResponse)
│   │   └── OllamaApiService.kt
│   └── local/
│       ├── Conversation.kt
│       ├── ConversationIndexEntry.kt
│       ├── ConversationManager.kt
│       ├── PreferencesManager.kt
│       └── TtsLanguage.kt
├── navigation/
│   └── NavGraph.kt                (class: AppNavGraph)
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/
│       └── Theme.kt
└── util/
    ├── ImageUtils.kt
    └── MarkdownUtils.kt
```

## Tests

Unit tests are located under `app/src/test/java/com/guiorioli/ollamatalk/`:

- `data/api/ChatModelsTest.kt`
- `data/api/ChatStreamEventTest.kt`
- `data/local/TtsLanguageTest.kt`
- `util/MarkdownUtilsTest.kt`

Run them with:

```bash
./gradlew test
```

## How to Build

### Prerequisites

- **Java 17+** — A JDK 17 or higher is required. Android Studio ships with one at `android-studio/jbr/`.
- **Android SDK** — with platform **android-35** and build-tools installed.
- **Gradle wrapper** — already included in the repository.

### Via Android Studio

1. Open the root folder in **Android Studio**
2. Wait for Gradle sync to finish
3. Connect an Android device or start an emulator
4. Click **Run** (Shift+F10)

### Via Command Line

```bash
# Set JAVA_HOME to the JDK bundled with Android Studio
export JAVA_HOME="/path/to/android-studio/jbr"

# Build debug APK
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## How to Use

1. Get an API Key at https://ollama.com/settings/keys
2. Enter the key in **Settings** within the app
3. Select your desired cloud model and TTS language
4. Start chatting, use the voice button, or attach an image
5. Tap the speaker icon on any response to hear it read aloud
6. Open the drawer to switch between saved conversations or start a new one
