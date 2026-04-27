# RxNoBots 🤖🚫
> Advanced bot protection for Velocity servers using chat & movement verification

<p align="center">

[![Velocity](https://img.shields.io/badge/Velocity-3.5.0+-00A9E0?logo=velocity&logoColor=white)](https://velocitypowered.com)
[![LimboAPI](https://img.shields.io/badge/LimboAPI-required-important)](https://github.com/Elytrium/LimboAPI)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Author](https://img.shields.io/badge/Author-Rompix666-9cf)](https://github.com/Rompix666)
[![Discord](https://img.shields.io/discord/972218989235298385?label=Discord&logo=discord)](https://discord.com/invite/PNp3S3sanv)
[![Telegram](https://img.shields.io/badge/Telegram-@RomixerX-26A5E4?logo=telegram)](https://t.me/RomixerX)

</p>

---

# 🇷🇺 Русская версия

## 🔒 О плагине

**RxNoBots** — это мощный анти-бот плагин для прокси **Velocity**, который фильтрует нежелательных игроков **до подключения к основным серверам**.

Плагин использует **LimboAPI**, создавая изолированное окружение, где игрок проходит проверку.

### 🔍 Как работает проверка

1. 🗣 Игрок вводит код в чат  
2. 🎯 Выполняет серию движений головой  
3. ✅ При успехе — допускается на сервер  

---

## ✨ Ключевые возможности

| Категория | Описание |
|:---------:|:---------|
| 🗣 **Чат-проверка** | Генерируется уникальный код |
| 🎯 **Движение** | Проверка взгляда (вверх/вниз/влево/вправо) |
| 🎲 **Рандомизация** | Уникальные последовательности |
| 📦 **Сессии** | Восстановление после дисконнекта |
| 🔁 **Кулдаун** | Повторная проверка не требуется |
| ⚙ **Гибкость** | Полная настройка логики |
| 🛡 **Байпас** | По IP и правам |
| 📊 **Статистика** | Логи и `/rnb stats` |
| 🔧 **Админ-контроль** | Полный контроль через команды |

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
| `/rnb reload` | `rxnobots.admin` | Перезагрузка |
| `/rnb verify <игрок>` | `rxnobots.admin` | Верификация |
| `/rnb reset <игрок>` | `rxnobots.admin` | Сброс |
| `/rnb timeout <игрок> <сек>` | `rxnobots.admin` | Таймаут |
| `/rnb bypass <игрок>` | `rxnobots.admin` | Байпас |
| `/rnb stats` | `rxnobots.admin` | Статистика |
| `/rnb session info <игрок>` | `rxnobots.admin` | Инфо |
| `/rnb session end <игрок>` | `rxnobots.admin` | Завершить |
| `/rnb cache clear <игрок>` | `rxnobots.admin` | Очистить кэш |

**Дополнительно:**  
`rxnobots.bypass`

---

## ⚙ Конфигурация

```yaml
verification:
  type: HYBRID
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
    duration: 600
  success:
    action: DISCONNECT
    target-server: lobby
  cooldown:
    track-by-user: true
    track-by-ip: true
