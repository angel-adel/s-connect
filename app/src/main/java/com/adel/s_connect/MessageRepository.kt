package com.adel.s_connect

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String? = null,
    val sender_id: String,
    val receiver_id: String,
    val content: String,
    val is_read: Boolean = false,
    val created_at: String? = null,
    val edited_at: String? = null,
    val deleted_at: String? = null
)

object MessageRepository {

    /**
     * Загружает сообщения между текущим пользователем и собеседником.
     */
    suspend fun loadMessages(otherUserId: String): Result<List<MessageDto>> {
        return try {
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val messages = Supabase.client
                .postgrest["messages"]
                .select {
                    filter {
                        or {
                            and {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", otherUserId)
                            }
                            and {
                                eq("sender_id", otherUserId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<MessageDto>()

            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Отправляет сообщение.
     */
    suspend fun sendMessage(
        receiverId: String,
        content: String
    ): Result<MessageDto> {
        return try {
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val newMessage = MessageDto(
                sender_id = currentUserId,
                receiver_id = receiverId,
                content = content
            )

            val created = Supabase.client
                .postgrest["messages"]
                .insert(newMessage) {
                    select()
                }
                .decodeSingle<MessageDto>()

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Отмечает сообщения от собеседника как прочитанные.
     */
    suspend fun markAsRead(otherUserId: String) {
        try {
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id ?: return

            Supabase.client
                .postgrest["messages"]
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("sender_id", otherUserId)
                        eq("receiver_id", currentUserId)
                        eq("is_read", false)
                    }
                }
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    /**
     * Редактирует сообщение (только своё).
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit> {
        return try {
            Supabase.client
                .postgrest["messages"]
                .update({
                    set("content", newContent)
                    set("edited_at", java.time.Instant.now().toString())
                }) {
                    filter {
                        eq("id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Удаляет сообщение (только своё).
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            Supabase.client
                .postgrest["messages"]
                .delete {
                    filter {
                        eq("id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}