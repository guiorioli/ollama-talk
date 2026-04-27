# Ollama Talk

Android app for chatting with Ollama Cloud models, with voice support.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| HTTP | OkHttp |
| JSON | Gson |
| Voice (input) | SpeechRecognizer (Android SDK) |
| Voice (output) | TextToSpeech (Android SDK) |
| Storage | SharedPreferences |
| Min SDK | 26 |
| Target SDK | 34 |

## Project Structure

```
app/src/main/java/com/ollamachat/
├── MainActivity.kt
├── OllamaTalkApp.kt
├── audio/
│   ├── SpeechRecognizerManager.kt
│   └── TextToSpeechManager.kt
├── data/
│   ├── api/
│   │   ├── OllamaApiService.kt
│   │   ├── ChatRequest.kt
│   │   └── ChatResponse.kt
│   └── local/
│       └── PreferencesManager.kt
├── navigation/
│   └── AppNavigation.kt
└── ui/
    ├── chat/
    │   ├── ChatScreen.kt
    │   └── ChatViewModel.kt
    └── settings/
        ├── SettingsScreen.kt
        └── SettingsViewModel.kt
```

## Pre-built APK

A pre-built debug APK is available at `dist/ollama-talk.apk`.

## How to Build

### Prerequisites

- **Java 17+** — A JDK 17 or higher is required. Android Studio ships with one at `android-studio/jbr/`.
- **Android SDK** — with platform **android-34** and build-tools installed.
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
3. Select your desired cloud model
4. Start chatting or use the voice button
