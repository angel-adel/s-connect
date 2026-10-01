package com.adel.s_connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (UserProfile) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    var chats by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Поиск
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var globalResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Загрузка чатов
    LaunchedEffect(Unit) {
        val result = ChatRepository.loadChats()
        isLoading = false
        if (result.isSuccess) chats = result.getOrNull() ?: emptyList()
        else errorMessage = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
    }

    // Realtime
    LaunchedEffect(Unit) {
        val myId = AuthRepository.currentUserId() ?: return@LaunchedEffect
        val channel = Supabase.client.channel("chats_$myId")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }
        channel.subscribe()
        var updateJob: Job? = null
        changes.collectLatest { change ->
            try {
                val newMsg = change.decodeRecord<MessageDto>()
                if (newMsg.sender_id == myId || newMsg.receiver_id == myId) {
                    updateJob?.cancel()
                    updateJob = launch {
                        delay(500)
                        ChatRepository.loadChats().onSuccess { chats = it }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Debounce глобального поиска
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            globalResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(300) // debounce
        val result = ChatRepository.searchUsers(searchQuery)
        isSearching = false
        if (result.isSuccess) globalResults = result.getOrNull() ?: emptyList()
    }

    // Локальная фильтрация чатов
    val filteredChats = remember(chats, searchQuery) {
        if (searchQuery.isBlank()) chats
        else {
            val q = searchQuery.trim().lowercase()
            chats.filter { it.user.username.lowercase().contains(q) }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Поиск людей") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            globalResults = emptyList()
                        }) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("S-Connect", color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Text("🔍", style = MaterialTheme.typography.titleLarge)
                        }
                        IconButton(onClick = onProfileClick) {
                            Text("👤", style = MaterialTheme.typography.titleLarge)
                        }
                        TextButton(onClick = onLogout) { Text("Выйти") }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                errorMessage != null -> Text(
                    "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )

                isSearchActive -> SearchResults(
                    query = searchQuery,
                    filteredChats = filteredChats,
                    globalResults = globalResults,
                    isSearching = isSearching,
                    onChatClick = onChatClick,
                    scope = scope
                )

                chats.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Пока нет чатов", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Найдите друзей в Smart Social",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(chats) { chat ->
                        ChatListItem(chat = chat, onClick = { onChatClick(chat.user) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    query: String,
    filteredChats: List<ChatPreview>,
    globalResults: List<UserProfile>,
    isSearching: Boolean,
    onChatClick: (UserProfile) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    when {
        query.length < 2 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Введите минимум 2 символа",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        filteredChats.isEmpty() && globalResults.isEmpty() && !isSearching ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Ничего не найдено",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        else -> LazyColumn(Modifier.fillMaxSize()) {
            // Секция: мои чаты
            if (filteredChats.isNotEmpty()) {
                item {
                    SectionHeader("💬 Мои чаты")
                }
                items(filteredChats) { chat ->
                    ChatListItem(chat = chat, onClick = { onChatClick(chat.user) })
                    HorizontalDivider()
                }
            }

            // Секция: глобальный поиск
            if (globalResults.isNotEmpty()) {
                item {
                    SectionHeader("🌍 Найдены пользователи")
                }
                items(globalResults) { user ->
                    UserSearchItem(user = user, onClick = { onChatClick(user) })
                    HorizontalDivider()
                }
            }

            if (isSearching) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun UserSearchItem(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.avatar_url != null) {
            AsyncImage(
                model = user.avatar_url,
                contentDescription = "Avatar",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.username.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = user.bio ?: "Без статуса",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatListItem(chat: ChatPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chat.user.avatar_url != null) {
            AsyncImage(
                model = chat.user.avatar_url,
                contentDescription = "Avatar",
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        chat.user.username.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = chat.user.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chat.lastMessage?.content ?: "Нет сообщений",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (chat.unreadCount > 0) {
            Badge { Text(chat.unreadCount.toString()) }
        }
    }
}