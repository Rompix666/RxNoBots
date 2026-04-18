<details>
<summary>RU</summary>
  
  # **RxNoBots** 🤖🚫
  ### Умная защита Velocity-сервера от ботов с помощью чат- и движенческой верификации

  [![Velocity](https://img.shields.io/badge/Velocity-3.5.0+-00A9E0?logo=velocity&logoColor=white)](https://velocitypowered.com)
  [![LimboAPI](https://img.shields.io/badge/LimboAPI-required-important)](https://github.com/Elytrium/LimboAPI)
  [![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
  [![bStats](https://img.shields.io/badge/bStats-28400-blue)](https://bstats.org/plugin/velocity/RxNoBots/28400)
  [![Author](https://img.shields.io/badge/Author-rompix-9cf)](https://github.com/rompix)
  [![Discord](https://img.shields.io/discord/972218989235298385?color=5865F2&label=Discord&logo=discord&logoColor=red)](https://discord.com/invite/PNp3S3sanv)
  [![Telegram](https://img.shields.io/badge/Telegram-@RomixerX-26A5E4?logo=telegram&logoColor=red)](https://t.me/RomixerX)
  
  ---

  ## 🔒 **О плагине**

  **RxNoBots** — мощный и гибкий плагин для прокси-сервера **Velocity**, который надёжно отсеивает ботов перед подключением к основным серверам. Используя виртуальный мир **LimboAPI**, плагин изолирует новых игроков и проводит двухэтапную проверку: ввод кода в чат и выполнение серии движений головой.

  - 🛡 **Защита от ботов и прокси-атак**
  - 🔄 **Сохранение прогресса** при обрыве соединения
  - 🧩 **Гибкая настройка** этапов и условий
  - 🌍 **Полная локализация** (русский, английский и любые другие)
  - 💾 **Поддержка SQLite и MySQL**

  **Версия:** 1.0.0  
  **Поддержка:** Velocity 3.5.0+

  ---

  ## ✨ **Ключевые возможности**

  | Категория | Описание |
  |:---------:|:---------|
  | 🗣 **Чат-проверка** | Сгенерированный код, который нужно ввести в чат |
  | 🎯 **Проверка движением** | Посмотри вверх, вниз, влево или вправо и удерживай взгляд |
  | 🎲 **Случайные последовательности** | Направления и длительность генерируются случайно для каждой сессии |
  | 📦 **Восстановление сессии** | При дисконнекте игрок продолжит с того же этапа после перезахода |
  | 🔁 **Кулдаун и запоминание** | Успешно прошедшие проверку игроки не беспокоятся повторно (настраиваемый период) |
  | ⚙ **Гибкие настройки** | Максимум попыток, таймауты, углы обзора, тип проверки (HYBRID / CHAT_ONLY / MOVEMENT_ONLY) |
  | 🛡 **Байпас по IP и праву** | Исключения для администрации и доверенных IP |
  | 📊 **Статистика и логирование** | Встроенная команда `/rnb stats` и подробные логи попыток |
  | 🔧 **Админ-команды** | `/rnb verify/reset/timeout/bypass/session/cache` — полный контроль |

  ---

  ## 📸 **Пример работы**

  <div align="center">

  | Чат-этап | Движение | Успех |
  |:--:|:--:|:--:|
  | *Ввод кода в чат* | *Удержание взгляда* | *Переход на сервер* |

  </div>

  *Скриншоты будут добавлены в репозиторий*

  ---

## 🛠 **Команды и права**

| Команда | Право | Описание |
|:--------|:------|:---------|
| `/rnb reload` | `rxnobots.admin` | Перезагрузить конфигурацию и язык |
| `/rnb verify <игрок>` | `rxnobots.admin` | Вручную верифицировать игрока |
| `/rnb reset <игрок>` | `rxnobots.admin` | Сбросить все данные проверки |
| `/rnb timeout <игрок> <сек>` | `rxnobots.admin` | Выдать временный бан на вход |
| `/rnb bypass <игрок>` | `rxnobots.admin` | Переключить байпас для игрока |
| `/rnb stats` | `rxnobots.admin` | Показать статистику |
| `/rnb session info <игрок>` | `rxnobots.admin` | Информация об активной сессии |
| `/rnb session end <игрок>` | `rxnobots.admin` | Принудительно завершить сессию |
| `/rnb cache clear <игрок>` | `rxnobots.admin` | Очистить кэш данных игрока |

### Дополнительное право

| Право | Описание |
|:------|:---------|
| `rxnobots.bypass` | Игнорировать все проверки (выдаётся отдельно) |

---

## 🌍 **Локализация**

Плагин полностью локализуем. Файлы лежат в `plugins/rxnobots/lang/`:

- `en.yml` — английский
- `ru.yml` — русский

**Как добавить новый язык:**  
Скопируйте существующий `.yml`, переведите строки.  
Поддерживаются форматы:

- MiniMessage: `<red>текст</red>`
- Legacy: `&cтекст`

---

## 📦 **Установка**

1. Установите **LimboAPI** на Velocity
2. Скачайте последний релиз **RxNoBots** и положите в `plugins/`
3. Перезапустите прокси или выполните `/velocity plugins`
4. Настройте `config.yml` под свои нужды

✅ **Готово!** Новые игроки будут проходить проверку.

---

## 💬 **Поддержка и обратная связь**

| Тип | Ссылка |
|:----|:-------|
| 🐞 Баг-репорты | [GitHub Issues](https://github.com/rompix/RxNoBots/issues) |
| 💡 Предложения | [GitHub Discussions](https://github.com/rompix/RxNoBots/discussions) |
| 📧 Контакты автора | [rompix (GitHub)](https://github.com/rompix) |

---

## ⭐ **Помоги проекту**

Если плагин оказался полезным для твоего сервера:

- ⭐ Поставь звезду на GitHub
- 📢 Расскажи друзьям и коллегам
- 🛠️ Предложи улучшения или сообщи о багах

---

<div align="center">

**Сделай свой сервер свободным от ботов с RxNoBots!**

</div align="/center">

---

  ## ⚙ **Конфигурация (основные секции)**

  ```yaml
  verification:
    type: HYBRID           # HYBRID, CHAT_ONLY, MOVEMENT_ONLY
    code:
      length: 4
      characters: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    movement:
      random: true
      available-directions: ["up","down","left","right"]
      min-duration: 2
      max-duration: 4
    attempts:
      max-attempts: 3
      max-sessions: 3
    timeout:
      duration: 600        # в секундах
    success:
      action: DISCONNECT   # или SERVER (перекинет на указанный сервер)
      target-server: lobby
    cooldown:
      track-by-user: true
      track-by-ip: true
      duration: 86400      # 24 часа

  ```

---

</details>

<details>
<summary>EN</summary>
  
# **RxNoBots** 🤖🚫
### Smart bot protection for Velocity servers using chat & head movement verification

  [![Velocity](https://img.shields.io/badge/Velocity-3.5.0+-00A9E0?logo=velocity&logoColor=white)](https://velocitypowered.com)
  [![LimboAPI](https://img.shields.io/badge/LimboAPI-required-important)](https://github.com/Elytrium/LimboAPI)
  [![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
  [![bStats](https://img.shields.io/badge/bStats-28400-blue)](https://bstats.org/plugin/velocity/RxNoBots/28400)
  [![Author](https://img.shields.io/badge/Author-rompix-9cf)](https://github.com/rompix)
  [![Discord](https://img.shields.io/discord/972218989235298385?color=5865F2&label=Discord&logo=discord&logoColor=red)](https://discord.com/invite/PNp3S3sanv)
  [![Telegram](https://img.shields.io/badge/Telegram-@RomixerX-26A5E4?logo=telegram&logoColor=red)](https://t.me/RomixerX)

---

## 🔒 **About the plugin**

**RxNoBots** is a powerful and flexible plugin for **Velocity** proxy servers that reliably filters out bots before they connect to your main servers. Using the virtual world of **LimboAPI**, it isolates new players and performs a two‑stage verification: entering a code in chat and performing a series of head movements.

- 🛡 **Bot & proxy attack protection**
- 🔄 **Progress saving** across connection drops
- 🧩 **Flexible stage & condition settings**
- 🌍 **Full localization** (Russian, English, any other)
- 💾 **SQLite & MySQL support**

**Version:** 1.0.0  
**Support:** Velocity 3.5.0+

---

## ✨ **Key features**

| Category | Description |
|:--------:|:------------|
| 🗣 **Chat verification** | A generated code that must be typed in chat |
| 🎯 **Movement verification** | Look up, down, left or right and hold your gaze |
| 🎲 **Random sequences** | Directions and durations are randomly generated per session |
| 📦 **Session recovery** | On disconnect, the player resumes from the same stage after rejoining |
| 🔁 **Cooldown & remembering** | Successfully verified players are not bothered again (configurable period) |
| ⚙ **Flexible settings** | Max attempts, timeouts, view angles, verification type (HYBRID / CHAT_ONLY / MOVEMENT_ONLY) |
| 🛡 **IP & permission bypass** | Exemptions for admins and trusted IPs |
| 📊 **Stats & logging** | Built‑in `/rnb stats` command and detailed attempt logs |
| 🔧 **Admin commands** | `/rnb verify/reset/timeout/bypass/session/cache` — full control |

---

## 📸 **Example**

<div align="center">

| Chat stage | Movement | Success |
|:--:|:--:|:--:|
| *Enter code in chat* | *Hold gaze* | *Teleport to server* |

</div>

*Screenshots will be added to the repository*

---

## 🛠 **Commands & permissions**

| Command | Permission | Description |
|:--------|:-----------|:------------|
| `/rnb reload` | `rxnobots.admin` | Reload config & language |
| `/rnb verify <player>` | `rxnobots.admin` | Manually verify a player |
| `/rnb reset <player>` | `rxnobots.admin` | Reset all verification data |
| `/rnb timeout <player> <seconds>` | `rxnobots.admin` | Apply a temporary ban on join |
| `/rnb bypass <player>` | `rxnobots.admin` | Toggle bypass for a player |
| `/rnb stats` | `rxnobots.admin` | Show statistics |
| `/rnb session info <player>` | `rxnobots.admin` | Info about an active session |
| `/rnb session end <player>` | `rxnobots.admin` | Force end a session |
| `/rnb cache clear <player>` | `rxnobots.admin` | Clear player data cache |

### Additional permission

| Permission | Description |
|:-----------|:------------|
| `rxnobots.bypass` | Ignore all checks (assigned separately) |

---

## 🌍 **Localization**

The plugin is fully localizable. Files are located in `plugins/rxnobots/lang/`:

- `en.yml` — English
- `ru.yml` — Russian

**How to add a new language:**  
Copy an existing `.yml` and translate the strings.  
Both formats are supported:

- MiniMessage: `<red>text</red>`
- Legacy: `&ctext`

---

## 📦 **Installation**

1. Install **LimboAPI** on Velocity
2. Download the latest **RxNoBots** release and put it in the `plugins/` folder
3. Restart the proxy or run `/velocity plugins` to load it
4. Configure `config.yml` to your needs

✅ **Done!** New players will now be verified.

---

## 💬 **Support & feedback**

| Type | Link |
|:-----|:-----|
| 🐞 Bug reports | [GitHub Issues](https://github.com/rompix/RxNoBots/issues) |
| 💡 Suggestions | [GitHub Discussions](https://github.com/rompix/RxNoBots/discussions) |
| 📧 Author contact | [rompix (GitHub)](https://github.com/rompix) |

---

## ⭐ **Help the project**

If the plugin is useful for your server:

- ⭐ Star it on GitHub
- 📢 Tell your friends and colleagues
- 🛠️ Suggest improvements or report bugs

---

<div align="center">

**Make your server bot‑free with RxNoBots!**

</div>

---

## ⚙ **Configuration (main sections)**

```yaml
verification:
  type: HYBRID           # HYBRID, CHAT_ONLY, MOVEMENT_ONLY
  code:
    length: 4
    characters: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  movement:
    random: true
    available-directions: ["up","down","left","right"]
    min-duration: 2
    max-duration: 4
  attempts:
    max-attempts: 3
    max-sessions: 3
  timeout:
    duration: 600        # seconds
  success:
    action: DISCONNECT   # or SERVER (will teleport to the specified server)
    target-server: lobby
  cooldown:
    track-by-user: true
    track-by-ip: true
    duration: 86400      # 24 hours

 ```

</details>
