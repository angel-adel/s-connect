package com.adel.s_connect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeSetting: String,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ---------- Тема ----------
            Text("Тема", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            ThemeOption("light", "Светлая", themeSetting) { onThemeChange("light") }
            ThemeOption("dark", "Тёмная", themeSetting) { onThemeChange("dark") }
            ThemeOption("system", "Системная", themeSetting) { onThemeChange("system") }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "🔜 В разработке: уведомления, приватность, медиа",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Вспомогательные composable-функции ----------

@Composable
private fun ThemeOption(
    value: String,
    label: String,
    selected: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected == value, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}