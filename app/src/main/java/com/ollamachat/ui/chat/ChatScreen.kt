package com.ollamachat.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ollamachat.data.local.ConversationIndexEntry
import com.ollamachat.util.preProcessMarkdownForDisplay
import com.ollamachat.util.stripMarkdown
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.toString()?.let { viewModel.onImagePicked(it) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear chat?") },
            text = { Text("All messages will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChat()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                ConversationDrawerContent(
                    conversations = state.conversations,
                    currentConversationId = state.currentConversationId,
                    onNewConversation = {
                        scope.launch { drawerState.close() }
                        viewModel.startNewConversation()
                    },
                    onSelectConversation = { conv ->
                        scope.launch { drawerState.close() }
                        viewModel.loadConversation(conv.id)
                    },
                    onDeleteConversation = { convId ->
                        viewModel.deleteConversation(convId)
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ollama Talk") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Open menu"
                            )
                        }
                    },
                    actions = {
                        if (state.isSpeaking) {
                            IconButton(onClick = viewModel::stopSpeaking) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop reading"
                                )
                            }
                        }
                        if (state.messages.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear chat")
                            }
                        }
                        IconButton(onClick = { viewModel.toggleAutoSpeak() }) {
                            Icon(
                                if (state.isAutoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (state.isAutoSpeak) "Disable auto-read" else "Enable auto-read",
                                tint = if (state.isAutoSpeak)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = state.inputText,
                    isLoading = state.isLoading,
                    isListening = state.isListening,
                    pendingImageUri = state.pendingImageUri,
                    onInputChanged = viewModel::onInputChanged,
                    onSend = viewModel::sendMessage,
                    onMicClick = {
                        if (state.isListening) {
                            viewModel.cancelVoiceInput()
                        } else if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onAttachClick = { imagePickerLauncher.launch("image/*") },
                    onClearImage = viewModel::clearPendingImage
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.messages.isEmpty()) {
                    EmptyChatHint()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isSpeaking = state.speakingMessageId == message.id,
                                onSpeak = { viewModel.speakMessage(message.content, message.id) },
                                onStopSpeaking = viewModel::stopSpeaking,
                                onCopy = {
                                    val plainText = stripMarkdown(message.content)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("response", plainText))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                state.error?.let { error ->
                    AlertDialog(
                        onDismissRequest = viewModel::clearError,
                        title = { Text("Notice") },
                        text = { Text(error) },
                        confirmButton = {
                            TextButton(onClick = viewModel::clearError) { Text("OK") }
                        }
                    )
                }

                if (!state.hasApiKey) {
                    NoApiKeyOverlay(onOpenSettings = onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun EmptyChatHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Ollama Talk",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type or speak a message to start",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoApiKeyOverlay(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "API Key not set",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Go to Settings and enter your Ollama Cloud API Key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenSettings) {
                    Text("Go to Settings")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatUiMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val color = if (isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (message.hasImage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Has image",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Image attached",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MarkdownText(
                        markdown = preProcessMarkdownForDisplay(message.content),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (!isUser) {
                                IconButton(
                                    onClick = { if (isSpeaking) onStopSpeaking() else onSpeak() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                        contentDescription = if (isSpeaking) "Stop" else "Listen",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationDrawerContent(
    conversations: List<ConversationIndexEntry>,
    currentConversationId: String?,
    onNewConversation: () -> Unit,
    onSelectConversation: (ConversationIndexEntry) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = onNewConversation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New conversation")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (conversations.isEmpty()) {
            Text(
                text = "No past conversations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(conversations, key = { it.id }) { conv ->
                    val isCurrent = conv.id == currentConversationId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { onSelectConversation(conv) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dateFormat.format(Date(conv.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteConversation(conv.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete conversation",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    isListening: Boolean,
    pendingImageUri: String?,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onAttachClick: () -> Unit,
    onClearImage: () -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        if (pendingImageUri != null) {
            val context = LocalContext.current
            val bitmap = remember(pendingImageUri) {
                var result: android.graphics.Bitmap? = null
                try {
                    val uri = Uri.parse(pendingImageUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    result = inputStream?.use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) { }
                result
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove image",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachClick,
                enabled = !isLoading
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Attach image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message…") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onMicClick,
                enabled = !isLoading
            ) {
                if (isListening) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = onSend,
                enabled = (inputText.isNotBlank() || pendingImageUri != null) && !isLoading
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if ((inputText.isNotBlank() || pendingImageUri != null) && !isLoading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
