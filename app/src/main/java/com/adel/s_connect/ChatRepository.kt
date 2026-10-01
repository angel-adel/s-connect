package com.adel.s_connect

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

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

object ChatRepository {

    suspend fun loadMyProfile(): Result<UserProfile?> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            val profile = Supabase.client
                .postgrest["users"]
                .select {
                    filter { eq("id", myId) }
                }
                .decodeSingleOrNull<UserProfile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(username: String, bio: String): Result<Unit> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            Supabase.client
                .postgrest["users"]
                .update({
                    set("username", username)
                    set("bio", bio)
                }) {
                    filter { eq("id", myId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadChats(): Result<List<ChatPreview>> {
        return try {
            val currentUserId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Пользователь не авторизован"))

            val friendships = Supabase.client
                .postgrest["friendships"]
                .select {
                    filter { eq("status", "accepted") }
                }
                .decodeList<Friendship>()

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

            val users = Supabase.client
                .postgrest["users"]
                .select {
                    filter { isIn("id", myFriendIds) }
                }
                .decodeList<UserProfile>()

            val result = users.map { user ->
                val messages = Supabase.client
                    .postgrest["messages"]
                    .select {
                        filter {
                            or {
                                and {
                                    eq("sender_id", currentUserId)
                                    eq("receiver_id", user.id)
                                }
                                and {
                                    eq("sender_id", user.id)
                                    eq("receiver_id", currentUserId)
                                }
                            }
                        }
                        order("created_at", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<Message>()

                val lastMessage = messages.firstOrNull()
                val unreadCount = messages.count {
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

    /**
     * Глобальный поиск пользователей по username.
     * Повторяет логику веб-поиска (search.html).
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            val myId = Supabase.client.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Не авторизован"))

            if (query.isBlank()) return Result.success(emptyList())

            val users = Supabase.client
                .postgrest["users"]
                .select {
                    filter {
                        ilike("username", "%$query%")
                        neq("id", myId)
                    }
                    limit(20)
                }
                .decodeList<UserProfile>()

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}