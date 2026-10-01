package com.adel.s_connect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = ChatRepository.loadMyProfile()
        isLoading = false
        if (result.isSuccess) profile = result.getOrNull()
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
                    IconButton(onClick = { showMenu = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙️ Настройки") },
                            onClick = {
                                showMenu = false
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Аватар
                if (!profile?.avatar_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = profile!!.avatar_url,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(120.dp).clip(CircleShape),
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

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = profile?.username ?: "Без имени",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val bioText = profile?.bio
                if (bioText != null) {
                    Text(bioText)   // ✅ Работает — val не делегат
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✏️ Редактировать профиль")
                }
            }
        }
    }
}