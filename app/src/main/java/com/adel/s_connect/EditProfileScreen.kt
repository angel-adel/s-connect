package com.adel.s_connect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = ChatRepository.loadMyProfile()
        isLoading = false
        result.getOrNull()?.let {
            username = it.username
            bio = it.bio ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать профиль") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Имя пользователя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("О себе") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val result = ChatRepository.updateProfile(username, bio)
                            isSaving = false
                            if (result.isSuccess) {
                                onSaved()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                            }
                        }
                    },
                    enabled = !isSaving && username.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("💾 Сохранить")
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}