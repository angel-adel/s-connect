package com.adel.s_connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Полноэкранный просмотр сторис одного пользователя.
 *
 * Логика:
 * - Показываем все сторис [userStories.stories] по порядку.
 * - Каждая — 5 секунд (автопереключение).
 * - Тап слева → предыдущая, тап справа → следующая.
 * - После последней — автозакрытие через onClose().
 * - Крестик справа сверху — закрыть вручную.
 *
 * Всё состояние локально, никаких глобальных таймеров.
 */
@Composable
fun StoryViewerScreen(
    userStories: UserStories,
    onClose: () -> Unit
) {
    val stories = userStories.stories
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }

    val currentStory = stories[currentIndex]

    // Таймер + автопереключение. LaunchedEffect(currentIndex) перезапускается
    // при смене индекса — прогресс сбрасывается автоматически.
    LaunchedEffect(currentIndex) {
        progress = 0f
        val stepMs = 50L
        val totalMs = 5000L
        val step = stepMs.toFloat() / totalMs.toFloat() // 0.01f за шаг

        while (progress < 1f) {
            delay(stepMs)
            progress = (progress + step).coerceAtMost(1f)
        }

        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ---------- МЕДИА (фото) ----------
        if (currentStory.mediaType == "image") {
            AsyncImage(
                model = currentStory.mediaUrl,
                contentDescription = "Story",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Видео — пока заглушка (без плеера)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎬", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Видео (плеер в разработке)",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ---------- ВЕРХНЯЯ ПАНЕЛЬ: ПРОГРЕСС-БАРЫ + АВТОР + КРЕСТИК ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            // Прогресс-бары
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val barProgress = when {
                        index < currentIndex -> 1f          // просмотрено
                        index == currentIndex -> progress   // идёт сейчас
                        else -> 0f                          // ещё не началось
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barProgress)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Автор + крестик
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Аватар
                if (!userStories.user.avatar_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = userStories.user.avatar_url,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userStories.user.username.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = userStories.user.username,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!currentStory.createdAt.isNullOrEmpty()) {
                        Text(
                            text = formatStoryTime(currentStory.createdAt),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Крестик
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✕",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // ---------- ПОДПИСЬ (caption) СНИЗУ ----------
        if (!currentStory.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = currentStory.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // ---------- ЗОНЫ ТАПА: СЛЕВА / СПРАВА ----------
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTapGestures { offset ->
                        val width = size.width
                        if (offset.x < width * 0.3f) {
                            // Тап слева — предыдущая
                            if (currentIndex > 0) currentIndex-- else progress = 0f
                        } else if (offset.x > width * 0.7f) {
                            // Тап справа — следующая
                            if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                        }
                        // Тап по центру — игнорируем (позже — пауза)
                    }
                }
        ) {
            Box(Modifier.weight(1f).fillMaxSize())
            Box(Modifier.weight(1f).fillMaxSize())
        }
    }
}

/**
 * Простое форматирование времени: «5 мин назад», «2 ч назад».
 * Для отображения над аватаром в шапке сторис.
 */
private fun formatStoryTime(iso: String): String {
    return try {
        // Парсим как в StoryRepository
        var normalized = iso.replace("+00:00", "Z").replace("+0000", "Z")
        val dotIndex = normalized.indexOf('.')
        if (dotIndex > 0) {
            val afterDot = normalized.substring(dotIndex + 1)
            val tzIndex = afterDot.indexOfFirst { it == 'Z' || it == '+' || it == '-' }
            if (tzIndex > 3) {
                val millis = afterDot.substring(0, 3)
                val rest = if (tzIndex >= 0) afterDot.substring(tzIndex) else ""
                normalized = normalized.substring(0, dotIndex + 1) + millis + rest
            }
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )

        var timeMs: Long? = null
        for (pattern in patterns) {
            try {
                val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = sdf.parse(normalized)
                if (date != null) { timeMs = date.time; break }
            } catch (_: Exception) {}
        }

        val t = timeMs ?: return ""
        val diffMs = System.currentTimeMillis() - t
        val minutes = diffMs / 60_000
        when {
            minutes < 1 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            minutes < 1440 -> "${minutes / 60} ч назад"
            else -> "${minutes / 1440} дн назад"
        }
    } catch (e: Exception) {
        ""
    }
}