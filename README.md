# Ollama Talk

App Android para chat com modelos cloud do Ollama, com suporte a voz.

## Tech Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose |
| HTTP | OkHttp |
| JSON | Gson |
| Voz (input) | SpeechRecognizer (Android SDK) |
| Voz (output) | TextToSpeech (Android SDK) |
| Armazenamento | SharedPreferences |
| Min SDK | 26 |
| Target SDK | 34 |

## Estrutura de Pastas

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

## Como Buildar

1. Abrir a pasta raiz no **Android Studio**
2. Aguardar sincronização do Gradle
3. Conectar dispositivo Android ou abrir emulador
4. Executar **Run** (Shift+F10)

O APK será gerado em `app/build/outputs/apk/debug/`

## Como Usar

1. Obter API Key em https://ollama.com/settings/keys
2. Inserir a key em **Configurações** no app
3. Selecionar o modelo cloud desejado
4. Iniciar o chat normalmente ou usar o botão de voz
