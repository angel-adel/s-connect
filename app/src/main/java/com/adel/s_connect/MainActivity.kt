package com.adel.s_connect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.adel.s_connect.ui.theme.SConnectTheme
import kotlinx.coroutines.launch

private const val SCREEN_CHATS = "chats"
private const val SCREEN_PROFILE = "profile"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_EDIT_PROFILE = "edit_profile"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var themeSetting by remember { mutableStateOf(SettingsManager.getTheme(context)) }
            var isLoggedIn by remember { mutableStateOf(AuthRepository.isLoggedIn()) }
            var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
            var currentScreen by remember { mutableStateOf(SCREEN_CHATS) }

            val darkTheme = when (themeSetting) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SConnectTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !isLoggedIn -> {
                            AuthScreen(
                                themeSetting = themeSetting,
                                onThemeChange = { newTheme ->
                                    themeSetting = newTheme
                                    SettingsManager.setTheme(context, newTheme)
                                },
                                onLoginSuccess = {
                                    currentScreen = SCREEN_CHATS
                                    isLoggedIn = true
                                }
                            )
                        }

                        selectedUser != null -> {
                            ChatWindowScreen(
                                otherUser = selectedUser!!,
                                onBack = { selectedUser = null }
                            )
                        }

                        else -> {
                            when (currentScreen) {
                                SCREEN_PROFILE -> ProfileScreen(
                                    onBack = { currentScreen = SCREEN_CHATS },
                                    onEditClick = { currentScreen = SCREEN_EDIT_PROFILE },
                                    onSettingsClick = { currentScreen = SCREEN_SETTINGS }
                                )

                                SCREEN_EDIT_PROFILE -> EditProfileScreen(
                                    onBack = { currentScreen = SCREEN_PROFILE },
                                    onSaved = { currentScreen = SCREEN_PROFILE }
                                )

                                SCREEN_SETTINGS -> SettingsScreen(
                                    themeSetting = themeSetting,
                                    onThemeChange = { newTheme ->
                                        themeSetting = newTheme
                                        SettingsManager.setTheme(context, newTheme)
                                    },
                                    onBack = { currentScreen = SCREEN_PROFILE }
                                )

                                else -> ChatListScreen(
                                    onChatClick = { user -> selectedUser = user },
                                    onProfileClick = { currentScreen = SCREEN_PROFILE },
                                    onLogout = {
                                        scope.launch {
                                            AuthRepository.signOut()
                                            isLoggedIn = false
                                            currentScreen = SCREEN_CHATS
                                            selectedUser = null
                                        }
                                    }
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
fun AuthScreen(
    themeSetting: String,
    onThemeChange: (String) -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.auth_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.auth_username)) },
            placeholder = { Text(stringResource(R.string.auth_username_hint)) },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            placeholder = { Text(stringResource(R.string.auth_password_hint)) },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isNotBlank() && password.isNotBlank()) {
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        val result = AuthRepository.signInOrSignUp(username, password)
                        isLoading = false
                        if (result.isSuccess) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Ошибка: ${result.exceptionOrNull()?.message ?: "неизвестная"}"
                        }
                    }
                }
            },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.auth_login), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            enabled = !isLoading,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:smartsocials@mail.ru")
                    putExtra(Intent.EXTRA_SUBJECT, "Восстановление пароля S-Connect")
                    putExtra(Intent.EXTRA_TEXT, "Здравствуйте! Забыл пароль. Имя: $username")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    errorMessage = "Почтовый клиент не найден"
                }
            }
        ) {
            Text(stringResource(R.string.auth_forgot_password))
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}