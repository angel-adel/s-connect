package com.adel.s_connect

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWindowScreen(
    otherUser: UserProfile,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var editingMessage by remember { mutableStateOf<MessageDto?>(null) }
    var deletingMessage by remember { mutableStateOf<MessageDto?>(null) }
    var selectedMessage by remember { mutableStateOf<MessageDto?>(null) }

    val currentUserId = remember { AuthRepository.currentUserId() }

    // Загрузка сообщений
    LaunchedEffect(otherUser.id) {
        val result = MessageRepository.loadMessages(otherUser.id)
        isLoading = false
        if (result.isSuccess) {
            messages = result.getOrNull() ?: emptyList()
            if (messages.isNotEmpty()) {
                listState.scrollToItem(messages.size - 1)
            }
            MessageRepository.markAsRead(otherUser.id)
        } else {
            errorMessage = result.exceptionOrNull()?.message
        }
    }

    // Realtime подписка
    LaunchedEffect(otherUser.id) {
        val myId = currentUserId ?: return@LaunchedEffect
        println("🔵 Realtime: подписка на chat_${myId}_${otherUser.id}")

        val channel = Supabase.client.channel("chat_${myId}_${otherUser.id}")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }

        channel.subscribe()
        println("🔵 Realtime: подписка выполнена")

        changes.collectLatest { change ->
            println("🔵 Realtime: пришло событие!")
            try {
                val newMsg = change.decodeRecord<MessageDto>()
                println("🔵 Realtime: новое сообщение: ${newMsg.content}")
                val isRelevant = (newMsg.sender_id == myId && newMsg.receiver_id == otherUser.id) ||
                        (newMsg.sender_id == otherUser.id && newMsg.receiver_id == myId)
                if (isRelevant) {
                    if (messages.none { it.id == newMsg.id }) {
                        messages = messages + newMsg
                    }
                }
            } catch (e: Exception) {
                println("🔴 Realtime ошибка: ${e.message}")
            }
        }
    }

    // Автопрокрутка
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onBack) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = otherUser.username,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Список сообщений
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    messages.isEmpty() -> {
                        Text(
                            text = "Начните общение первым! 👋",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id ?: it.hashCode().toString() }) { msg ->
                                MessageBubble(
                                    message = msg,
                                    isMine = msg.sender_id == currentUserId,
                                    otherUsername = otherUser.username,
                                    onLongClick = {
                                        if (msg.sender_id == currentUserId) {
                                            selectedMessage = msg
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Поле ввода
            Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Сообщение...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        enabled = !isSending
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotEmpty()) {
                                isSending = true
                                scope.launch {
                                    val result = MessageRepository.sendMessage(
                                        receiverId = otherUser.id,
                                        content = text
                                    )
                                    isSending = false
                                    if (result.isSuccess) {
                                        inputText = ""
                                        messages = messages + result.getOrThrow()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message
                                    }
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("➤", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Меню действий
        selectedMessage?.let { msg ->
            MessageActionsDialog(
                onEdit = {
                    editingMessage = msg
                    selectedMessage = null
                },
                onDelete = {
                    deletingMessage = msg
                    selectedMessage = null
                },
                onDismiss = { selectedMessage = null }
            )
        }

        // Диалог редактирования
        editingMessage?.let { msg ->
            EditMessageDialog(
                currentText = msg.content,
                onSave = { newText ->
                    scope.launch {
                        val result = MessageRepository.editMessage(msg.id ?: return@launch, newText)
                        if (result.isSuccess) {
                            messages = messages.map {
                                if (it.id == msg.id) it.copy(content = newText, edited_at = "now") else it
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                        }
                        editingMessage = null
                    }
                },
                onDismiss = { editingMessage = null }
            )
        }

        // Диалог удаления
        deletingMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { deletingMessage = null },
                title = { Text("Удалить сообщение?") },
                text = { Text("Это действие нельзя отменить.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val result = MessageRepository.deleteMessage(msg.id ?: return@launch)
                            if (result.isSuccess) {
                                messages = messages.filter { it.id != msg.id }
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                            }
                            deletingMessage = null
                        }
                    }) {
                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingMessage = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageDto,
    isMine: Boolean,
    otherUsername: String,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = { },
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isMine) "Вы:" else "$otherUsername:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.edited_at != null) {
                        Text(
                            text = "изменено ",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    message.created_at?.let { time ->
                        Text(
                            text = formatTime(time),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (message.is_read) "✓✓" else "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.is_read) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageActionsDialog(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Действия") },
        text = {
            Column {
                TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("✏️", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Редактировать")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑️", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditMessageDialog(
    currentText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать сообщение") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

fun formatTime(iso: String): String {
    return try {
        iso.substringAfter("T").take(5).ifEmpty { iso }
    } catch (_: Exception) {
        iso
    }
}