package com.adel.s_connect

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

object AuthRepository {

    private const val DOMAIN = "@smartsocial.app"

    /**
     * Вход или регистрация.
     *
     * Логика:
     * 1. Формируем email: username@smartsocial.app
     * 2. Пробуем ВОЙТИ
     * 3. Если "Invalid login credentials" → неверный пароль
     * 4. Если "User not found" → регистрируемся
     * 5. Если успех → возвращаем Result.success
     */
    suspend fun signInOrSignUp(username: String, password: String): Result<Unit> {
        val email = "$username$DOMAIN"

        // 1. Пробуем войти
        try {
            Supabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            val msg = e.message ?: ""

            // 2. Неверный пароль
            if (msg.contains("Invalid login", ignoreCase = true)) {
                return Result.failure(Exception("Неверный пароль"))
            }

            // 3. Пользователь не найден → регистрируемся
            if (msg.contains("User not found", ignoreCase = true) ||
                msg.contains("invalid_grant", ignoreCase = true)) {
                return try {
                    Supabase.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    Result.success(Unit)
                } catch (e2: Exception) {
                    Result.failure(Exception("Ошибка регистрации: ${e2.message}"))
                }
            }

            // 4. Другие ошибки
            return Result.failure(Exception("Ошибка входа: $msg"))
        }
    }

    /**
     * Выход из аккаунта.
     */
    suspend fun signOut() {
        try {
            Supabase.client.auth.signOut()
        } catch (e: Exception) {
            // Игнорируем
        }
    }

    /**
     * Проверка: вошёл ли пользователь.
     */
    fun isLoggedIn(): Boolean {
        return try {
            Supabase.client.auth.currentSessionOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Текущий ID пользователя.
     */
    fun currentUserId(): String? {
        return try {
            Supabase.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
}