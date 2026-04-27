# Ollama Talk — PRD

## Visão Geral
Aplicativo Android que permite conectar à conta Ollama Cloud e utilizar um chat com modelos cloud selecionados. Suporte a entrada por voz (falar pergunta) e saída por áudio (ouvir resposta via TTS).

## Requisitos Funcionais

### RF1 — Autenticação
- Usuário insere uma API Key do Ollama Cloud manualmente
- A key é armazenada localmente no dispositivo (SharedPreferences)
- Botão de atalho para abrir o navegador em `https://ollama.com/settings/keys`
- Validação básica da key antes de salvar (test call à API)

### RF2 — Chat com modelos cloud
- Envio de mensagens de texto para a API Ollama Cloud (`POST /api/chat`)
- Exibição do histórico da conversa no formato chat
- Suporte a streaming desligado (`stream: false`) — aguarda resposta completa
- Default model: `gemma3:27b-cloud`

### RF3 — Seletor de modelo
- Dropdown/seletor com modelos cloud disponíveis
- Lista obtida via API (`GET /api/tags`) ou hardcoded com os modelos cloud conhecidos
- Modelo selecionado persiste entre sessões

### RF4 — Entrada por voz
- Botão "Falar" aciona o SpeechRecognizer nativo do Android
- Transcrição preenche o campo de texto da mensagem
- Feedback visual de que está gravando

### RF5 — Saída por áudio
- Após receber resposta do modelo, botão "Ouvir" ou reprodução automática via TextToSpeech
- Controle de play/stop

### RF6 — Navegação
- Tela principal: Chat
- Tela de configurações: API Key, modelo padrão

## Requisitos Não-Funcionais

### RNF1 — Build
- APK compilado localmente com Android Studio
- Kotlin + Jetpack Compose
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

### RNF2 — Dependências externas mínimas
- OkHttp para chamadas HTTP
- Gson para serialização JSON
- Demais funcionalidades (voz, TTS, armazenamento) com APIs nativas do Android

### RNF3 — Oflline
- App não funciona offline (depende da API Ollama Cloud)
- Mensagem amigável quando sem conexão
