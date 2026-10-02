package com.adel.s_connect

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Story(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String,   // "image" | "video"
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("views_count") val viewsCount: Int = 0
)

/**
 * Группировка сторис одного пользователя.
 * Для ленты сверху чатов: один кружок на пользователя.
 */
data class UserStories(
    val user: UserProfile,
    val stories: List<Story>,
    val hasUnseen: Boolean = true  // пока всегда true — реализуем "просмотрено" позже
)

object StoryRepository {

    /**
     * Универсальная retry-обёртка. См. ChatRepository.
     */
    private suspend fun <T> retryNetwork(
        times: Int = 3,
        block: suspend () -> T
    ): T {
        var lastError: Exception? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < times - 1) delay(1500L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Network error after $times attempts")
    }

    /**
     * Загружает файл в Storage и создаёт запись в stories.
     */
    suspend fun uploadStory(
        context: Context,
        uri: Uri,
        caption: String?
    ): Result<Story> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.failure(Exception("Не удалось прочитать файл"))

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo = mimeType.startsWith("video/")
            val mediaType = if (isVideo) "video" else "image"
            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                "video/mp4" -> "mp4"
                "video/quicktime" -> "mov"
                "video/webm" -> "webm"
                else -> if (isVideo) "mp4" else "jpg"
            }

            val storyId = UUID.randomUUID().toString()
            val path = "$userId/$storyId.$ext"

            Supabase.client
                .storage
                .from("stories")
                .upload(path, bytes, upsert = false)

            val publicUrl = Supabase.client
                .storage
                .from("stories")
                .publicUrl(path)

            val newStory = Story(
                id = storyId,
                userId = userId,
                mediaUrl = publicUrl,
                mediaType = mediaType,
                caption = caption?.takeIf { it.isNotBlank() }
            )

            Supabase.client
                .postgrest["stories"]
                .insert(newStory)

            Result.success(newStory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Загружает все активные сторис (не истёкшие).
     * С retry и fallback на фильтрацию в Kotlin.
     */
    suspend fun loadActiveStories(): Result<List<Story>> {
        return try {
            val stories = retryNetwork {
                try {
                    // Пытаемся отфильтровать на стороне БД
                    Supabase.client
                        .postgrest["stories"]
                        .select {
                            filter { gt("expires_at", "now()") }
                            order("created_at", Order.DESCENDING)
                            limit(100)
                        }
                        .decodeList<Story>()
                } catch (e: Exception) {
                    // Если фильтр не работает — грузим всё, отфильтруем в Kotlin
                    Supabase.client
                        .postgrest["stories"]
                        .select {
                            order("created_at", Order.DESCENDING)
                            limit(100)
                        }
                        .decodeList<Story>()
                }
            }

            // Fallback-фильтр на клиенте (на случай, если сервер не отфильтровал)
            val now = System.currentTimeMillis()
            val active = stories.filter { story ->
                story.expiresAt == null || parseIsoToMillis(story.expiresAt) > now
            }

            Result.success(active)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Загружает сторис и группирует по пользователям.
     * Для ленты сверху чатов: один кружок на пользователя.
     *
     * Возвращает список UserStories, отсортированный:
     * 1. Сначала мои (если есть).
     * 2. Потом остальные, по времени последней сторис (новые — выше).
     */
    suspend fun loadStoriesGrouped(): Result<List<UserStories>> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id

            val storiesResult = loadActiveStories()
            if (storiesResult.isFailure) {
                return Result.failure(storiesResult.exceptionOrNull() ?: Exception("Ошибка"))
            }

            val stories = storiesResult.getOrNull() ?: emptyList()
            if (stories.isEmpty()) return Result.success(emptyList())

            // Загружаем профили всех авторов сторис ОДНИМ запросом
            val userIds = stories.map { it.userId }.distinct()
            val users = retryNetwork {
                Supabase.client
                    .postgrest["users"]
                    .select { filter { isIn("id", userIds) } }
                    .decodeList<UserProfile>()
            }
            val usersById = users.associateBy { it.id }

            // Группируем сторис по userId
            val grouped = stories
                .groupBy { it.userId }
                .mapNotNull { (userId, userStories) ->
                    val user = usersById[userId] ?: return@mapNotNull null
                    UserStories(
                        user = user,
                        stories = userStories.sortedByDescending { it.createdAt },
                        hasUnseen = true
                    )
                }

            // Сортировка: мои — первыми, остальные — по последней сторис
            val sorted = grouped.sortedWith(
                compareByDescending<UserStories> { it.user.id == myId }
                    .thenByDescending { it.stories.firstOrNull()?.createdAt ?: "" }
            )

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Удаляет свою сторис.
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            retryNetwork {
                Supabase.client
                    .postgrest["stories"]
                    .delete {
                        filter {
                            eq("id", storyId)
                            eq("user_id", userId)
                        }
                    }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // Хелпер: ISO-8601 → миллисекунды
    // ------------------------------------------------------------

    /**
     * Парсит ISO-8601 timestamp из Supabase в миллисекунды.
     * Работает на всех API без desugaring.
     *
     * Supabase возвращает форматы:
     *   2026-10-02T11:30:00.123456+00:00
     *   2026-10-02T11:30:00.123456Z
     *   2026-10-02T11:30:00+00:00
     *   2026-10-02T11:30:00Z
     */
    private fun parseIsoToMillis(iso: String): Long {
        return try {
            var normalized = iso
                .replace("+00:00", "Z")
                .replace("+0000", "Z")

            // Обрезаем микросекунды: .123456 → .123
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
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd HH:mm:ss"
            )

            for (pattern in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val date = sdf.parse(normalized)
                    if (date != null) return date.time
                } catch (_: Exception) {
                    // пробуем следующий
                }
            }

            Long.MAX_VALUE
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}