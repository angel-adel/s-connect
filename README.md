# 📱 S-Connect

<div align="center">

**Мессенджер экосистемы SmartSocial**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Supabase](https://img.shields.io/badge/Supabase-Backend-green?logo=supabase)](https://supabase.com/)
[![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android)](https://developer.android.com/)

**Дочерний проект [SmartSocial](https://github.com/angel-adel/smartsocial)** — тихой соцсети без рекламы и трекеров.

</div>

---

## 🎯 О проекте

**S-Connect** — это Android-мессенджер, который станет частью экосистемы SmartSocial.

**Главная идея:** один аккаунт — две платформы. Пользователи SmartSocial автоматически получают доступ к S-Connect.

### Что уже работает

- 💬 Личные чаты
- ⚡ Realtime — мгновенные сообщения
- ✏️ Редактирование сообщений
- 🗑️ Удаление сообщений
- ✓✓ Галочки прочтения
- 🔔 Счётчик непрочитанных
- 🔄 Синхронизация с веб-версией SmartSocial
- 🌍 5 языков: 🇷🇺 🇬🇧 🇺🇦 🇪🇸 🇩🇪
- 🎨 Тёмная и светлая тема

### Что в разработке

- 📎 Медиа (фото, видео, файлы)
- 🎤 Голосовые сообщения
- 🔔 Push-уведомления
- ⚙️ Экран настроек
- 📸 Статусы (24 часа)

---

## 🛠 Технологии

| Слой | Технология |
|------|------------|
| **Язык** | Kotlin |
| **UI** | Jetpack Compose |
| **Backend** | Supabase (Auth, Postgrest, Realtime) |
| **WebSocket** | Ktor + OkHttp |
| **Сериализация** | kotlinx.serialization |
| **Изображения** | Coil |
| **Сборка** | Gradle Kotlin DSL |
| **Минимум** | Android 7.0 (API 24) |

---

## 🚀 Статус разработки

- [x] Создан проект Android Studio
- [x] Настроен Gradle
- [x] Экран авторизации
- [x] 5 языков (RU, EN, UK, ES, DE)
- [x] Цвета SmartSocial
- [x] Supabase Auth (единый аккаунт)
- [x] Список чатов
- [x] Окно чата
- [x] Отправка сообщений
- [x] Редактирование и удаление
- [x] Realtime (WebSocket)
- [x] Синхронизация с веб-версией
- [ ] Медиа (фото, видео, файлы)
- [ ] Голосовые сообщения
- [ ] Push-уведомления
- [ ] Экран настроек
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
```

### Для пользователей

APK будет доступен в разделе [Releases](https://github.com/angel-adel/s-connect/releases) после завершения разработки.

**Требования:**
- Android 7.0+
- Интернет
- ~15 МБ

---

## 🤝 Связь с SmartSocial

| Проект | Описание |
|--------|----------|
| **[SmartSocial](https://github.com/angel-adel/smartsocial)** | Соцсеть — «тихое, уютное пространство» |
| **S-Connect** (этот репозиторий) | Мессенджер — общение в реальном времени |

**Единая база данных Supabase** = общие аккаунты, друзья, профили.

**Один аккаунт — две платформы.**

---

## 👨‍💻 Автор

**Adel** ([@angel-adel](https://github.com/angel-adel))

- 🌐 [angel-adel.github.io](https://angel-adel.github.io)
- 📧 smartsocials@mail.ru

---

## 📜 Лицензия

MIT License — используйте свободно.

---

<div align="center">

**Сделано с 💜 одним человеком**

_Козёл-тимлид одобряет_ 🐏

</div>
