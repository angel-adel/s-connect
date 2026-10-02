package com.adel.s_connect

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStoryUploaded: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Состояние профиля ---
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // --- Меню ---
    var showTopMenu by remember { mutableStateOf(false) }       // ⋮ в топбаре
    var showCameraMenu by remember { mutableStateOf(false) }    // 📷 на аватаре

    // --- Сторис ---
    var showStoryDialog by remember { mutableStateOf(false) }
    var pendingStoryUri by remember { mutableStateOf<Uri?>(null) }

    // --- Загрузка профиля ---
    suspend fun reloadProfile() {
        val result = ChatRepository.loadMyProfile()
        if (result.isSuccess) profile = result.getOrNull()
    }

    LaunchedEffect(Unit) {
        reloadProfile()
        isLoading = false
    }

    // --- Пикер АВАТАРА ---
    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploadingAvatar = true
                uploadError = null
                val result = ProfileRepository.uploadAvatar(context, uri)
                isUploadingAvatar = false
                if (result.isSuccess) {
                    reloadProfile()
                } else {
                    uploadError = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                }
            }
        }
    }

    // --- Пикер СТОРИС (фото и видео) ---
    val pickStoryMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingStoryUri = uri
            showStoryDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showTopMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙️ Настройки") },
                            onClick = {
                                showTopMenu = false
                                onSettingsClick()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ================= АВАТАР + КНОПКА 📷 =================
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Аватар
                    if (!profile?.avatar_url.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile!!.avatar_url,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile?.username?.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Кнопка 📷 — открывает меню (аватар / сторис)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isUploadingAvatar) {
                                showCameraMenu = true
                            },
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isUploadingAvatar) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "📷",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    // Меню 📷
                    DropdownMenu(
                        expanded = showCameraMenu,
                        onDismissRequest = { showCameraMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🖼 Сменить аватар") },
                            onClick = {
                                showCameraMenu = false
                                pickAvatar.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📸 Опубликовать сторис") },
                            onClick = {
                                showCameraMenu = false
                                pickStoryMedia.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                    )
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= ИМЯ =================
                Text(
                    text = profile?.username ?: "Без имени",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ================= BIO =================
                val bioText = profile?.bio
                if (!bioText.isNullOrBlank()) {
                    Text(
                        text = bioText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ================= ОШИБКА ЗАГРУЗКИ =================
                uploadError?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ================= КНОПКА РЕДАКТИРОВАНИЯ =================
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ Редактировать профиль")
                }
            }
        }
    }

    // ================= ДИАЛОГ ПУБЛИКАЦИИ СТОРИС =================
    val storyUri = pendingStoryUri
    if (showStoryDialog && storyUri != null) {
        StoryUploadDialog(
            uri = storyUri,
            onDismiss = {
                showStoryDialog = false
                pendingStoryUri = null
            },
            onPublished = {
                showStoryDialog = false
                pendingStoryUri = null
                onStoryUploaded()
            }
        )
    }
}

@Composable
private fun StoryUploadDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isVideo = remember(uri) {
        context.contentResolver.getType(uri)?.startsWith("video/") == true
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Text(if (isVideo) "📹 Новая сторис (видео)" else "📸 Новая сторис")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Превью
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🎬",
                                        style = MaterialTheme.typography.displayLarge
                                    )
                                    Text(
                                        text = "Видео",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Story preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Подпись
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 200) caption = it },
                    label = { Text("Подпись (необязательно)") },
                    placeholder = { Text("Что происходит?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    enabled = !isUploading
                )

                // Счётчик символов
                if (caption.isNotEmpty()) {
                    Text(
                        text = "${caption.length}/200",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                // Ошибка
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isUploading = true
                    error = null
                    scope.launch {
                        val result = StoryRepository.uploadStory(context, uri, caption)
                        isUploading = false
                        if (result.isSuccess) {
                            onPublished()
                        } else {
                            error = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                        }
                    }
                },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Публикация...")
                } else {
                    Text("Опубликовать")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Отмена")
            }
        }
    )
}