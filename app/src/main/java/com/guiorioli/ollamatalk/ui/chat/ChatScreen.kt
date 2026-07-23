package com.guiorioli.ollamatalk.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.ui.text.input.ImeAction
import com.guiorioli.ollamatalk.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.guiorioli.ollamatalk.data.local.ConversationIndexEntry
import com.guiorioli.ollamatalk.util.ConversationMessage
import com.guiorioli.ollamatalk.util.formatConversationText
import com.guiorioli.ollamatalk.util.preProcessMarkdownForDisplay
import com.guiorioli.ollamatalk.util.stripMarkdown
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = layoutInfo.totalItemsCount
            total <= 1 || lastVisible >= total - 1
        }
    }
    var autoScroll by remember { mutableStateOf(true) }
    var showScrollToBottom by remember { mutableStateOf(false) }

    LaunchedEffect(isAtBottom) {
        autoScroll = isAtBottom
        showScrollToBottom = !isAtBottom && state.messages.isNotEmpty()
    }

    LaunchedEffect(state.messages.lastIndex, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty() && autoScroll) {
            listState.scrollToItem(state.messages.size)
        }
    }

    var showClearDialog by remember { mutableStateOf(false) }

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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_chat_title)) },
            text = { Text(stringResource(R.string.clear_chat_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearChat()
                    showClearDialog = false
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
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
                    title = {
                        if (!state.isSpeaking) {
                            Text(stringResource(R.string.app_name))
                        }
                    },
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
                                contentDescription = stringResource(R.string.open_menu)
                            )
                        }
                    },
                    actions = {
                        if (state.isSpeaking) {
                            IconButton(onClick = viewModel::stopSpeaking) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.stop_reading)
                                )
                            }
                        }
                        if (state.messages.isNotEmpty()) {
                            IconButton(onClick = {
                                val text = formatConversationText(
                                    state.messages.map { msg ->
                                        ConversationMessage(
                                            role = msg.role,
                                            content = msg.content,
                                            hasImage = msg.hasImage,
                                            isLoading = msg.isLoading
                                        )
                                    },
                                    state.selectedModel
                                )
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("conversation", text))
                                Toast.makeText(context, context.getString(R.string.conversation_copied), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_conversation))
                            }
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_chat))
                            }
                        }
                        IconButton(onClick = { viewModel.toggleAutoSpeak() }) {
                            Icon(
                                if (state.isAutoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (state.isAutoSpeak) stringResource(R.string.disable_auto_read) else stringResource(R.string.enable_auto_read),
                                tint = if (state.isAutoSpeak)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
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
                            if (message.role == "tool") {
                                ToolMessageBubble(message = message)
                            } else {
                                MessageBubble(
                                    message = message,
                                    isSpeaking = state.speakingMessageId == message.id,
                                    onSpeak = { viewModel.speakMessage(message.content, message.id) },
                                    onStopSpeaking = viewModel::stopSpeaking,
                                    onCopy = {
                                        val plainText = stripMarkdown(message.content)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("response", plainText))
                                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                                    },
                                    onCancelLoading = if (message.isLoading || message.isStreaming) viewModel::cancelMessage else null
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }

                if (state.isWebSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.searching_the_web),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (showScrollToBottom) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(state.messages.size)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.scroll_to_bottom)
                        )
                    }
                }

                state.error?.let { error ->
                    AlertDialog(
                        onDismissRequest = viewModel::clearError,
                        title = { Text(stringResource(R.string.notice)) },
                        text = { Text(error) },
                        confirmButton = {
                            TextButton(onClick = viewModel::clearError) { Text(stringResource(R.string.ok)) }
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_chat_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoApiKeyOverlay(onOpenSettings: () -> Unit) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.api_key_info_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.api_key_info_body),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }

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
                    text = stringResource(R.string.no_api_key_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_api_key_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.go_to_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showInfoDialog = true }) {
                    Text(stringResource(R.string.more_info))
                }
            }
        }
    }
}

@Composable
private fun ToolMessageBubble(message: ChatUiMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatUiMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopSpeaking: () -> Unit,
    onCopy: () -> Unit,
    onCancelLoading: (() -> Unit)? = null
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
                if ((message.isLoading || message.isStreaming) && message.content.isEmpty()) {
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
                            text = stringResource(R.string.thinking),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (onCancelLoading != null) {
                            IconButton(
                                onClick = onCancelLoading,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    if (message.hasImage) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = stringResource(R.string.has_image),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.image_attached),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (isUser) {
                        MarkdownText(
                            markdown = preProcessMarkdownForDisplay(message.content),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        SelectionContainer {
                            MarkdownText(
                                markdown = preProcessMarkdownForDisplay(message.content),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    if (message.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.copy),
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
                                        contentDescription = if (isSpeaking) stringResource(R.string.stop) else stringResource(R.string.listen),
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
            text = stringResource(R.string.conversations),
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
            Text(stringResource(R.string.new_conversation))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        if (conversations.isEmpty()) {
            Text(
                text = stringResource(R.string.no_past_conversations),
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
                                    contentDescription = stringResource(R.string.delete_conversation),
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
                        contentDescription = stringResource(R.string.attached_image),
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
                        contentDescription = stringResource(R.string.remove_image),
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
                    contentDescription = stringResource(R.string.attach_image),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.type_message)) },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (!isLoading) onSend() }),
                minLines = 1,
                maxLines = 5,
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
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = stringResource(R.string.speak),
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
                    contentDescription = stringResource(R.string.send),
                    tint = if ((inputText.isNotBlank() || pendingImageUri != null) && !isLoading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}
