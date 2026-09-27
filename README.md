# 📱 S-Connect

<div align="center">

**Мессенджер экосистемы SmartSocial**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-Backend-green?logo=supabase)](https://supabase.com/)
[![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Дочерний проект [SmartSocial](https://github.com/angel-adel/smartsocial)** — тихой соцсети без рекламы и трекеров.

</div>

---

## 🎯 О проекте

**S-Connect** — это Android-мессенджер, который станет частью экосистемы SmartSocial. 

**Главная идея:** один аккаунт — две платформы. Пользователи SmartSocial автоматически получают доступ к S-Connect.

### Что будет внутри

- 💬 Личные и групповые чаты
- 🎤 Голосовые сообщения
- 📸 Статусы (исчезают через 24 часа)
- 🟢 Онлайн-статусы
- 🔔 Push-уведомления
- 🔄 Синхронизация с SmartSocial
- 🌍 5 языков: 🇷🇺 🇬🇧 🇺🇦 🇪🇸 🇩🇪

---

## 🛠 Технологии

| Слой | Технология |
|------|------------|
| **Язык** | Kotlin |
| **UI** | Jetpack Compose |
| **Архитектура** | MVVM (планируется) |
| **Backend** | Supabase (Auth, Postgrest, Realtime) |
| **Сборка** | Gradle Kotlin DSL |
| **Минимум** | Android 7.0 (API 24) |

---

## 📸 Скриншоты

<div align="center">

| Экран авторизации |
|:-----------------:|
| _Скриншот появится после сборки_ |

</div>

---

## 🚀 Статус разработки

- [x] Создан проект Android Studio
- [x] Настроен Gradle
- [x] Экран авторизации
- [x] 5 языков (RU, EN, UK, ES, DE)
- [x] Цвета SmartSocial
- [x] Git-репозиторий
- [ ] Подключение Supabase Auth
- [ ] Список чатов
- [ ] Окно чата
- [ ] Голосовые сообщения
- [ ] Статусы
- [ ] Синхронизация с SmartSocial
- [ ] Релиз в Google Play

---

## 📦 Установка

### Для разработчиков

```bash
# Клонировать репозиторий
git clone https://github.com/angel-adel/s-connect.git
cd s-connect

# Собрать APK
./gradlew assembleDebug

# Установить на устройство
adb install app/build/outputs/apk/debug/app-debug.apk
