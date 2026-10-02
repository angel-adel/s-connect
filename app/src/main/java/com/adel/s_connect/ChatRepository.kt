package com.adel.s_connect

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// ============================================================
// МОДЕЛИ
// ============================================================

@Serializable
data class UserProfile(
    val id: String,
    val username: String,
    val avatar_url: String? = null,
    val last_seen: String? = null,
    val bio: String? = null
)

@Serializable
data class Friendship(
    val id: String,
    val requester_id: String,
    val addressee_id: String,
    val status: String,
    val created_at: String? = null
)

@Serializable
data class Message(
    val id: String,
    val sender_id: String,
    val receiver_id: String,
    val content: String,
    val is_read: Boolean = false,
    val created_at: String? = null
)

data class ChatPreview(
    val user: UserProfile,
    val lastMessage: Message?,
    val unreadCount: Int
)

// ============================================================
// REPOSITORY
// ============================================================

object ChatRepository {

    /**
     * Универсальная обёртка с retry.
     * Пытается выполнить block до [times] раз с нарастающей задержкой.
     * Помогает при нестабильной сети (потеря пакетов, таймауты).
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
                if (attempt < times - 1) {
                    delay(1500L * (attempt + 1)) // 1.5s, потом 3s
                }
            }
        }
        throw lastError ?: Exception("Network error after $times attempts")
    }

    // ------------------------------------------------------------
    // МОЙ ПРОФИЛЬ
    // ------------------------------------------------------------

    suspend fun loadMyProfile(): Result<UserProfile?> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val profile = retryNetwork {
                Supabase.client
                    .postgrest["users"]
                    .select {
                        filter { eq("id", myId) }
                    }
                    .decodeSingleOrNull<UserProfile>()
            }

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // ОБНОВЛЕНИЕ ПРОФИЛЯ
    // ------------------------------------------------------------

    suspend fun updateProfile(username: String, bio: String): Result<Unit> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            retryNetwork {
                Supabase.client
                    .postgrest["users"]
                    .update({
                        set("username", username)
                        set("bio", bio)
                    }) {
                        filter { eq("id", myId) }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // СПИСОК ЧАТОВ (оптимизировано: 3 запроса вместо N+2)
    // ------------------------------------------------------------

    suspend fun loadChats(): Result<List<ChatPreview>> {
        return try {
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Пользователь не авторизован"))

            // 1. Все подтверждённые дружбы
            val friendships = retryNetwork {
                Supabase.client
                    .postgrest["friendships"]
                    .select {
                        filter { eq("status", "accepted") }
                    }
                    .decodeList<Friendship>()
            }

            val myFriendIds = friendships
                .filter { it.requester_id == currentUserId || it.addressee_id == currentUserId }
                .map {
                    if (it.requester_id == currentUserId) it.addressee_id
                    else it.requester_id
                }
                .distinct()

            if (myFriendIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // 2. Профили друзей — ОДИН запрос
            val users = retryNetwork {
                Supabase.client
                    .postgrest["users"]
                    .select {
                        filter { isIn("id", myFriendIds) }
                    }
                    .decodeList<UserProfile>()
            }

            // 3. Все сообщения, где я sender или receiver — ОДИН запрос вместо N
            val allMessages = retryNetwork {
                Supabase.client
                    .postgrest["messages"]
                    .select {
                        filter {
                            or {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(500)
                    }
                    .decodeList<Message>()
            }

            // 4. Группировка сообщений по собеседнику в памяти
            val result = users.map { user ->
                val messagesWithUser = allMessages.filter { msg ->
                    (msg.sender_id == currentUserId && msg.receiver_id == user.id) ||
                            (msg.sender_id == user.id && msg.receiver_id == currentUserId)
                }

                val lastMessage = messagesWithUser.firstOrNull()
                val unreadCount = messagesWithUser.count {
                    it.receiver_id == currentUserId && !it.is_read
                }

                ChatPreview(
                    user = user,
                    lastMessage = lastMessage,
                    unreadCount = unreadCount
                )
            }.sortedByDescending { it.lastMessage?.created_at }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // ГЛОБАЛЬНЫЙ ПОИСК ПОЛЬЗОВАТЕЛЕЙ
    // ------------------------------------------------------------

    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            if (query.isBlank()) return Result.success(emptyList())

            val users = retryNetwork {
                Supabase.client
                    .postgrest["users"]
                    .select {
                        filter {
                            ilike("username", "%$query%")
                            neq("id", myId)
                        }
                        limit(20)
                    }
                    .decodeList<UserProfile>()
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}