package com.adel.s_connect

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileRepository {

    suspend fun uploadAvatar(context: Context, uri: Uri): Result<String> {
        return try {
            val userId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return Result.failure(Exception("Не удалось прочитать файл"))

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "jpg"
            }

            val fileName = "$userId.$ext"

            // ← ИСПРАВЛЕНО: upsert как именованный параметр, без лямбды
            Supabase.client
                .storage
                .from("avatars")
                .upload(fileName, bytes, upsert = true)

            val publicUrl = Supabase.client
                .storage
                .from("avatars")
                .publicUrl(fileName)

            val avatarUrlWithCache = "$publicUrl?t=${System.currentTimeMillis()}"

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