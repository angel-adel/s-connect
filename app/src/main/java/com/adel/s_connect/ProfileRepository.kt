package com.adel.s_connect

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileRepository {

    /**
     * Загружает аватар в Supabase Storage и обновляет users.avatar_url.
     *
     * Повторяет логику веб-версии (profile.html):
     *   bucket = "avatars"
     *   path   = "<user_id>.<ext>"
     *   upsert = true
     *   ?t=timestamp против кэша Coil
     */
    suspend fun uploadAvatar(context: Context, uri: Uri): Result<String> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            // 1. Читаем байты из Uri
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.failure(Exception("Не удалось прочитать файл"))

            // 2. Определяем расширение по MIME
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }

            // 3. Имя файла = "<user_id>.<ext>" — как в вебе
            val fileName = "$userId.$ext"

            // 4. Загрузка в bucket "avatars" с перезаписью
            Supabase.client
                .storage
                .from("avatars")
                .upload(fileName, bytes) {
                    upsert = true
                }

            // 5. Публичный URL + ?t=timestamp (иначе Coil покажет старый кэш)
            val publicUrl = Supabase.client
                .storage
                .from("avatars")
                .publicUrl(fileName)

            val avatarUrlWithCache = "$publicUrl?t=${System.currentTimeMillis()}"

            // 6. UPDATE users.avatar_url
            Supabase.client
                .postgrest["users"]
                .update({
                    set("avatar_url", avatarUrlWithCache)
                }) {
                    filter { eq("id", userId) }
                }

            Result.success(avatarUrlWithCache)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}