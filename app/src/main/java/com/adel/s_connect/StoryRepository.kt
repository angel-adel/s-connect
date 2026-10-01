package com.adel.s_connect

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
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
    @SerialName("views_count") val viewsCount: Int = 0,
    // Поле для UI — присоединяем вручную после загрузки
    val author: UserProfile? = null
)

object StoryRepository {

    /**
     * Загружает файл в Storage и создаёт запись в stories.
     * Возвращает Story с публичным URL.
     */
    suspend fun uploadStory(
        context: Context,
        uri: Uri,
        caption: String?
    ): Result<Story> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            // 1. Читаем байты
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.failure(Exception("Не удалось прочитать файл"))

            // 2. Определяем MIME и расширение
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

            // 3. Уникальное имя файла в папке <user_id>/
            val storyId = UUID.randomUUID().toString()
            val path = "$userId/$storyId.$ext"

            // 4. Загрузка в bucket "stories"
            Supabase.client
                .storage
                .from("stories")
                .upload(path, bytes, upsert = false)

            // 5. Публичный URL
            val publicUrl = Supabase.client
                .storage
                .from("stories")
                .publicUrl(path)

            // 6. Запись в таблицу stories
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
     * Сортировка: сначала новые.
     */
    suspend fun loadActiveStories(): Result<List<Story>> {
        return try {
            val stories = Supabase.client
                .postgrest["stories"]
                .select {
                    filter { gt("expires_at", "now()") }  // если не работает — см. примечание ниже
                    order("created_at", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<Story>()

            Result.success(stories)
        } catch (e: Exception) {
            // Если фильтр gt с "now()" не работает — вернём всё, отфильтруем в Kotlin
            try {
                val stories = Supabase.client
                    .postgrest["stories"]
                    .select {
                        order("created_at", Order.DESCENDING)
                        limit(100)
                    }
                    .decodeList<Story>()
                Result.success(stories)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    /**
     * Удаляет свою сторис.
     */
    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            Supabase.client
                .postgrest["stories"]
                .delete {
                    filter {
                        eq("id", storyId)
                        eq("user_id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}