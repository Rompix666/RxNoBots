https://www.youtube-nocookie.com/embed/Hs0OjhUJ-EQ

# RxNoBots 🤖🚫
> Advanced bot protection for Velocity servers using chat & movement verification with elegant purple gradient styling

<p align="center">

[![Velocity](https://img.shields.io/badge/Velocity-3.5.0+-00A9E0?logo=velocity&logoColor=white)](https://velocitypowered.com)
[![LimboAPI](https://img.shields.io/badge/LimboAPI-required-important)](https://github.com/Elytrium/LimboAPI)
[![Author](https://img.shields.io/badge/Author-Rompix666-9cf)](https://github.com/Rompix666)
[![Discord](https://img.shields.io/discord/972218989235298385?label=Discord&logo=discord)](https://discord.com/invite/PNp3S3sanv)
[![Telegram](https://img.shields.io/badge/Telegram-@RomixerX-26A5E4?logo=telegram)](https://t.me/RomixerX)

</p>

# Launges
**ru, en, de, fr, ja, pt, zh**

---
# 🇺🇸 English version

## 🔒 About the plugin

**RxNoBots** is a powerful anti‑bot plugin for **Velocity** proxy that filters unwanted players **before they connect to the main servers**.

The plugin uses **LimboAPI** to create an isolated environment where the player undergoes a two‑step verification process.

### 🔍 How verification works

1. 🗣 **Chat step** – the player types a unique code displayed on screen.
2. 🎯 **Movement step** – the player looks in a specified direction (up/down/left/right) and holds it for a few seconds.
3. ✅ **Success** – upon passing both steps, the player is allowed to join the server.

> 💡 **New:** Detailed on‑screen instructions and a stylish purple gradient interface help players understand exactly what to do, reducing confusion and failed attempts.

---

## ✨ Key features

| Category | Description |
|:---------:|:---------|
| 🗣 **Chat verification** | Generates a unique alphanumeric code. |
| 🎯 **Movement** | Gaze tracking (up, down, left, right) with configurable hold duration. |
| 🎲 **Randomization** | Each session generates unique movement sequences. |
| 📦 **Session persistence** | Sessions resume after disconnection (e.g., server restart). |
| 🔁 **Cooldown** | Verified players are not re‑verified for a configurable period. |
| ⚙ **Flexibility** | Fully customizable logic – chat‑only, movement‑only, or hybrid modes. |
| 🛡 **Bypass** | Whitelist by IP address or permission (`rxnobots.bypass`). |
| 📊 **Statistics** | Real‑time stats via `/rnb stats` and bStats integration. |
| 🔧 **Admin control** | Full management through dedicated commands. |
| 🌐 **Localization** | Built‑in English and Russian language files (easily extendable). |
| 🎨 **UI/UX** | Stylish purple gradient messages with clear step‑by‑step instructions for players. |

---

## 📸 Example workflow

<p align="center">

| Step 1 | Step 2 | Result |
|:--:|:--:|:--:|
| Enter code | Hold gaze direction | Join server |

</p>

---

## 🛠 Commands & permissions

| Command | Permission | Description |
|:--------|:------|:---------|
| `/rnb reload` | `rxnobots.admin` | Reload configuration and language files. |
| `/rnb verify <player>` | `rxnobots.admin` | Manually verify a player. |
| `/rnb reset <player>` | `rxnobots.admin` | Reset a player's verification status. |
| `/rnb timeout <player> <seconds>` | `rxnobots.admin` | Set a timeout for a player. |
| `/rnb bypass <player>` | `rxnobots.admin` | Toggle bypass permission for a player. |
| `/rnb stats` | `rxnobots.admin` | Show global statistics. |
| `/rnb session info <player>` | `rxnobots.admin` | Display detailed session information. |
| `/rnb session end <player>` | `rxnobots.admin` | Force‑end a player's verification session. |
| `/rnb cache clear <player>` | `rxnobots.admin` | Clear cached player data (useful for debugging). |

**Additional permission:**  
`rxnobots.bypass` – grants automatic bypass for players with this permission.

---

## ⚙ Config


<details>
<summary>config.yml</summary>

```yaml
# RxNoBots Plugin - Configuration
version: 1.2

# General Settings
general:
  language: "ru"           # Available: en, ru, de, fr, ja, pt, zh.
  debug: true             # Enable for detailed logs (disable in production)
  plugin-prefix: "<gradient:#ff5555:#ff00aa>RxNoBots</gradient> <dark_gray>»</dark_gray>"

# Database Settings
database:
  type: "h2"           # sqlite, mysql, h2
  sqlite:
    file: "rxnobots.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "rxnobots"
    username: "root"
    password: "password"
    connection-pool-size: 5      # HikariCP pool size (connections to MySQL)
    thread-pool-size: 4          # Size of async DB thread pool (MySQL only)

# Limbo Server Settings (requires LimboAPI)
limbo:
  enabled: true
  host: "127.0.0.1"
  port: 25566
  brand-name: "<gradient:#ff5555:#ff00aa>RxNoBots</gradient> <gray>Verification</gray>"

# Verification System
verification:
  enabled: true
  type: "HYBRID"           # HYBRID, CHAT_ONLY, MOVEMENT_ONLY

  # Chat Code Settings
  code:
    length: 4
    characters: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    case-sensitive: false

  # Movement Verification Settings
  movement:
    directions:            # Predefined directions (used when random: false)
      - "up:2"
      - "left:2"
    random: true           # Generate random sequence of directions
    min-duration: 2        # Minimum hold time per direction (seconds)
    max-duration: 4        # Maximum hold time per direction (seconds)
    available-directions:  # Pool of directions for random generation
      - "up"
      - "down"
      - "left"
      - "right"
    angles:                # Angle thresholds for each direction
      up:
        pitch-min: -90.0
        pitch-max: -30.0
      down:
        pitch-min: 30.0
        pitch-max: 90.0
      left:
        yaw-min: 135.0
        yaw-max: 225.0
      right:
        yaw-min: -45.0
        yaw-max: 45.0
    tolerance: 15.0        # Allowed deviation from exact angle (currently not used)
    response-timeout: 30   # Inactivity timeout per stage (seconds)
    kick-on-timeout: true  # Disconnect player on timeout

  # Attempt Limits
  attempts:
    max-attempts: 3        # Max failed code attempts per session
    max-sessions: 3        # Max total verification sessions before timeout
    reset-on-success: true

  # Timeout Settings (after failing max attempts or max sessions)
  timeout:
    duration: 600          # Timeout duration (seconds) = 10 minutes

  # Successful Verification Actions
  success:
    action: "DISCONNECT"   # DISCONNECT or SERVER (send to target server)
    target-server: "lobby"
    remember-duration: 86400  # Cooldown before requiring re-verification (seconds) = 24h

  # Cooldown Tracking (prevents repeated verifications)
  cooldown:
    track-by-user: true
    track-by-ip: true
    duration: 86400        # Cooldown duration (seconds)

# Bypass Settings
bypass:
  permission: "rxnobots.bypass"
  ip-whitelist:
    - "127.0.0.1"
    - "localhost"

# Performance Tuning
performance:
  cleanup-interval: 3600      # Cache cleanup interval (seconds) – legacy, replaced by Guava TTL
  max-sessions: 500           # Max concurrent verification sessions
  session-timeout: 300        # Max session lifetime (seconds)
  max-cache-size: 10000       # Maximum number of PlayerData entries in cache
  batch-save-interval: 10     # Interval (seconds) for batch saving dirty sessions

# Security Settings
security:
  max-verification-time: 120   # Overall verification time limit (seconds)
  anti-spam-delay: 1000        # Chat anti-spam delay (ms)
  log-attempts: true           # Log failed verification attempts
```

</details>



---

## 🗂 Database support

| Type | Description |
|:----:|:---------|
| **H2** | Embedded, used as fallback and for migration. |
| **SQLite** | Lightweight file‑based database. |
| **MySQL** | Production‑ready with connection pooling. |

Data is automatically migrated from H2 to your chosen database on first startup.

---

## 📦 Dependencies

- **Velocity** 3.5.0+
- **LimboAPI** (required) – provides the virtual world for verification.

---

## 🔧 Installation

1. Place `RxNoBots.jar` in your Velocity `plugins/` folder.
2. Place `LimboAPI.jar` in the same folder.
3. Start the server – default configuration and language files will be generated.
4. Adjust `config.yml` and language files (`lang/en.yml`, `lang/ru.yml`) as needed.
5. Reload with `/rnb reload` or restart the proxy.

---

## 🤝 Contributing

Issues and pull requests are welcome!  
For discussions, join our [Discord](https://discord.com/invite/PNp3S3sanv) or [Telegram](https://t.me/RomixerX).

---

# 🇷🇺 Русская версия

## 🔒 О плагине

**RxNoBots** — это мощный анти‑бот плагин для прокси **Velocity**, который фильтрует нежелательных игроков **до подключения к основным серверам**.

Плагин использует **LimboAPI**, создавая изолированное окружение, где игрок проходит двухэтапную проверку.

### 🔍 Как работает проверка

1. 🗣 **Чат‑этап** – игрок вводит уникальный код, отображаемый на экране.
2. 🎯 **Этап движения** – игрок смотрит в указанном направлении (вверх/вниз/влево/вправо) и удерживает взгляд несколько секунд.
3. ✅ **Успех** – после прохождения обоих этапов игрок допускается на сервер.

> 💡 **Новое:** Подробные инструкции на экране и стильный фиолетовый интерфейс помогают игрокам точно понять, что делать, уменьшая количество ошибок и неудачных попыток.

---

## ✨ Ключевые возможности

| Категория | Описание |
|:---------:|:---------|
| 🗣 **Чат‑проверка** | Генерируется уникальный буквенно‑цифровой код. |
| 🎯 **Движение** | Отслеживание взгляда (вверх, вниз, влево, вправо) с настраиваемой длительностью удержания. |
| 🎲 **Рандомизация** | Каждая сессия создаёт уникальную последовательность движений. |
| 📦 **Сохранение сессий** | Сессии восстанавливаются после отключения (например, перезапуска сервера). |
| 🔁 **Кулдаун** | Верифицированные игроки не проходят повторную проверку в течение настраиваемого периода. |
| ⚙ **Гибкость** | Полностью настраиваемая логика – только чат, только движение или гибридный режим. |
| 🛡 **Байпас** | Белый список по IP‑адресу или разрешению (`rxnobots.bypass`). |
| 📊 **Статистика** | Данные в реальном времени через `/rnb stats` и интеграция с bStats. |
| 🔧 **Админ‑контроль** | Полное управление через специальные команды. |
| 🌐 **Локализация** | Встроенные английский и русский языковые файлы (легко расширяются). |
| 🎨 **UI/UX** | Стильные фиолетовые градиентные сообщения с чёткими пошаговыми инструкциями для игроков. |

---

## 📸 Пример работы

<p align="center">

| Этап 1 | Этап 2 | Результат |
|:--:|:--:|:--:|
| Ввод кода | Удержание взгляда | Вход на сервер |

</p>

---

## 🛠 Команды и права

| Команда | Право | Описание |
|:--------|:------|:---------|
| `/rnb reload` | `rxnobots.admin` | Перезагрузить конфигурацию и языковые файлы. |
| `/rnb verify <игрок>` | `rxnobots.admin` | Вручную верифицировать игрока. |
| `/rnb reset <игрок>` | `rxnobots.admin` | Сбросить статус проверки игрока. |
| `/rnb timeout <игрок> <сек>` | `rxnobots.admin` | Установить таймаут для игрока. |
| `/rnb bypass <игрок>` | `rxnobots.admin` | Переключить право обхода проверки. |
| `/rnb stats` | `rxnobots.admin` | Показать глобальную статистику. |
| `/rnb session info <игрок>` | `rxnobots.admin` | Детальная информация о сессии. |
| `/rnb session end <игрок>` | `rxnobots.admin` | Принудительно завершить сессию. |
| `/rnb cache clear <игрок>` | `rxnobots.admin` | Очистить кэш данных игрока (полезно для отладки). |

**Дополнительное право:**  
`rxnobots.bypass` – даёт автоматический обход проверки.

---

## ⚙ Config


<details>
<summary>config.yml</summary>

```yaml
# RxNoBots Plugin - Configuration
version: 1.2

# General Settings
general:
  language: "ru"           # Available: en, ru, de, fr, ja, pt, zh.
  debug: true             # Enable for detailed logs (disable in production)
  plugin-prefix: "<gradient:#ff5555:#ff00aa>RxNoBots</gradient> <dark_gray>»</dark_gray>"

# Database Settings
database:
  type: "h2"           # sqlite, mysql, h2
  sqlite:
    file: "rxnobots.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "rxnobots"
    username: "root"
    password: "password"
    connection-pool-size: 5      # HikariCP pool size (connections to MySQL)
    thread-pool-size: 4          # Size of async DB thread pool (MySQL only)

# Limbo Server Settings (requires LimboAPI)
limbo:
  enabled: true
  host: "127.0.0.1"
  port: 25566
  brand-name: "<gradient:#ff5555:#ff00aa>RxNoBots</gradient> <gray>Verification</gray>"

# Verification System
verification:
  enabled: true
  type: "HYBRID"           # HYBRID, CHAT_ONLY, MOVEMENT_ONLY

  # Chat Code Settings
  code:
    length: 4
    characters: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    case-sensitive: false

  # Movement Verification Settings
  movement:
    directions:            # Predefined directions (used when random: false)
      - "up:2"
      - "left:2"
    random: true           # Generate random sequence of directions
    min-duration: 2        # Minimum hold time per direction (seconds)
    max-duration: 4        # Maximum hold time per direction (seconds)
    available-directions:  # Pool of directions for random generation
      - "up"
      - "down"
      - "left"
      - "right"
    angles:                # Angle thresholds for each direction
      up:
        pitch-min: -90.0
        pitch-max: -30.0
      down:
        pitch-min: 30.0
        pitch-max: 90.0
      left:
        yaw-min: 135.0
        yaw-max: 225.0
      right:
        yaw-min: -45.0
        yaw-max: 45.0
    tolerance: 15.0        # Allowed deviation from exact angle (currently not used)
    response-timeout: 30   # Inactivity timeout per stage (seconds)
    kick-on-timeout: true  # Disconnect player on timeout

  # Attempt Limits
  attempts:
    max-attempts: 3        # Max failed code attempts per session
    max-sessions: 3        # Max total verification sessions before timeout
    reset-on-success: true

  # Timeout Settings (after failing max attempts or max sessions)
  timeout:
    duration: 600          # Timeout duration (seconds) = 10 minutes

  # Successful Verification Actions
  success:
    action: "DISCONNECT"   # DISCONNECT or SERVER (send to target server)
    target-server: "lobby"
    remember-duration: 86400  # Cooldown before requiring re-verification (seconds) = 24h

  # Cooldown Tracking (prevents repeated verifications)
  cooldown:
    track-by-user: true
    track-by-ip: true
    duration: 86400        # Cooldown duration (seconds)

# Bypass Settings
bypass:
  permission: "rxnobots.bypass"
  ip-whitelist:
    - "127.0.0.1"
    - "localhost"

# Performance Tuning
performance:
  cleanup-interval: 3600      # Cache cleanup interval (seconds) – legacy, replaced by Guava TTL
  max-sessions: 500           # Max concurrent verification sessions
  session-timeout: 300        # Max session lifetime (seconds)
  max-cache-size: 10000       # Maximum number of PlayerData entries in cache
  batch-save-interval: 10     # Interval (seconds) for batch saving dirty sessions

# Security Settings
security:
  max-verification-time: 120   # Overall verification time limit (seconds)
  anti-spam-delay: 1000        # Chat anti-spam delay (ms)
  log-attempts: true           # Log failed verification attempts
```

</details>

---

## 🗂 Поддержка баз данных

| Тип | Описание |
|:---:|:---------|
| **H2** | Встроенная, используется как резервная и для миграции. |
| **SQLite** | Лёгкая файловая база данных. |
| **MySQL** | Готова к продакшену с пулом соединений. |

Данные автоматически мигрируются из H2 в выбранную БД при первом запуске.

---

## 📦 Зависимости

- **Velocity** 3.5.0+
- **LimboAPI** (обязательно) – предоставляет виртуальный мир для проверки.

---

## 🔧 Установка

1. Поместите `RxNoBots.jar` в папку `plugins/` вашего Velocity.
2. Поместите `LimboAPI.jar` в ту же папку.
3. Запустите сервер – будут созданы конфигурация и языковые файлы по умолчанию.
4. Настройте `config.yml` и языковые файлы (`lang/en.yml`, `lang/ru.yml`) по необходимости.
5. Перезагрузите плагин командой `/rnb reload` или перезапустите прокси.

---

## 🤝 Участие в разработке

Мы приветствуем Issues и Pull Requests!  
Для обсуждений присоединяйтесь к нашему [Discord](https://discord.com/invite/PNp3S3sanv) или [Telegram](https://t.me/RomixerX).
```
