# HybridAC

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Server Version](https://img.shields.io/badge/Paper%20%2F%20Purpur-1.16.5--1.21.x-blue.svg)](https://papermc.io/)
[![Release](https://img.shields.io/github/v/release/Xoady/HybridAC?color=brightgreen&label=Download%20JAR)](https://github.com/Xoady/HybridAC/releases/latest)
[![Packages](https://img.shields.io/badge/Packages-GitHub-blue.svg)](https://github.com/Xoady/HybridAC/packages)

**HybridAC** — современный серверный античит для Minecraft (Paper / Purpur / Spigot 1.16.5–1.21.x), сочетающий детерминированные эвристические проверки на уровне сетевых пакетов с машинным обучением (ML) и нейросетевым скорингом атак в реальном времени.

> 📦 **[Скачать готовый скомпилированный плагин (v1.0.0)](https://github.com/Xoady/HybridAC/releases/latest)** — скачайте `hybridac-plugin-1.0.0.jar` и поместите в папку `plugins/`.

---

## Ключевые возможности

- **Гибридный анализ атак**:
  - Пакетный трекинг ротаций головы, шага GCD (`gcd_error`), угловой скорости и ускорения рывков.
  - Оценка динамики хитбокса, расстояния и стрейфа игрока/цели.
  - Асинхронная передача срезов боевых окон в локальный или облачный ML-сервис (XGBoost + LSTM).
- **Пакетная перехватка**:
  - Глубокая инспекция боевых пакетов через ProtocolLib без задержек основного тика сервера.
- **Интеллектуальные санкции**:
  - Гибкая система подозрения (`suspicion score`), страйков и авто-наказаний (предупреждения, кик, бан).
  - Динамические боевые санкции: отмена ударов, сброс спринта, штрафной нокбек.
- **Мониторинг и меню**:
  - Интерактивное GUI-меню подозрительных игроков (`/hybridac menu`).
  - Actionbar-монитор в реальном времени (`/hybridac monitor <player>`).
  - Просмотр подозрительных игроков (`/hybridac suspicious`).
- **Сборщик датасетов**:
  - Встроенная система записи и разметки ударов для дообучения нейросети (`/hybridac datacollector`).
- **Многоязычность**:
  - Полная поддержка русской (`ru.yml`) и английской (`en.yml`) локализаций.
- **Интеграция**:
  - Поддержка плейсхолдеров PlaceholderAPI (`%hybridac_suspicion%`, `%hybridac_alerts%` и др.).
  - Локальное SQLite-хранилище данных игроков и сессий.

---

## Требования

- **Java**: 21 (рекомендуется) или 17+
- **Сервер**: Paper, Purpur или Spigot 1.16.5 — 1.21.x
- **Зависимости**:
  - [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) 5.3.0+ (обязательно)
  - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (опционально)

---

## Сборка из исходников

```bash
./gradlew pluginJar
```
Скомпилированный `.jar` будет создан в каталоге `build/libs/hybridac-plugin-1.0.0.jar`.

---

## Команды и права

| Команда | Описание | Право |
|---|---|---|
| `/hybridac help` | Показать список доступных команд | `hybridac.use` |
| `/hybridac alerts` | Переключить получение уведомлений о читах | `hybridac.alerts` |
| `/hybridac menu` | Открыть GUI со списком подозреваемых | `hybridac.admin` |
| `/hybridac monitor <игрок>` | Включить HUD-мониторинг игрока в Actionbar | `hybridac.admin` |
| `/hybridac suspicious` | Показать игроков с высоким уровнем подозрения | `hybridac.admin` |
| `/hybridac datacollector <start\|stop>` | Запуск/остановка сбора датасетов | `hybridac.admin` |
| `/hybridac reload` | Перезагрузить конфигурацию и сообщения | `hybridac.admin` |

---

## Структура проекта

```
.
├── src/
│   ├── main/
│   │   ├── java/com/hybridac/
│   │   │   ├── bootstrap/          # Инициализация и контекст сервисов
│   │   │   ├── buffer/             # Скользящие буферы боевых сигналов
│   │   │   ├── check/              # Реестр проверок
│   │   │   ├── command/            # Команды и субкоманды
│   │   │   ├── config/             # Загрузчик и модели конфигурации
│   │   │   ├── debug/              # Поток алертов и отладки
│   │   │   ├── integration/        # PlaceholderAPI Expansion
│   │   │   ├── listener/           # Слушатели событий Bukkit & пакетов ProtocolLib
│   │   │   ├── menu/               # Инвентарный GUI подозреваемых
│   │   │   ├── message/            # Сервис локализации и сообщений
│   │   │   ├── ml/                 # Клиент и планировщик ML-инференса
│   │   │   ├── model/              # Модели данных ударов и боевых окон
│   │   │   ├── monitor/            # Actionbar-мониторинг игроков
│   │   │   ├── player/             # Данные игроков и боевые сессии
│   │   │   ├── recording/          # Запись и экспорт датасетов в JSONL
│   │   │   ├── stats/              # Статистика и метрики
│   │   │   ├── storage/            # SQLite-хранилище (HikariCP)
│   │   │   ├── util/               # Утилиты векторов, цветов, времени
│   │   │   └── violation/          # Менеджер нарушений, санкций и наказаний
│   │   └── resources/
│   │       ├── config.yml          # Основной конфигурационный файл
│   │       ├── plugin.yml          # Манифест плагина
│   │       └── messages/           # Файлы локализаций (ru.yml, en.yml)
│   └── test/                       # Модульные тесты (JUnit 5)
├── build.gradle                    # Конфигурация Gradle
└── settings.gradle
```

---

## Настройка

После первого запуска плагин создаёт папку `plugins/HybridAC/` с файлами:
- `config.yml` — настройки ML-сервиса, проверок, порогов подозрения, прямых страйков и авто-наказаний.
- `messages/` — файлы локализации (`ru.yml`, `en.yml`).
- `hybridac.db` — локальная база данных истории нарушений и привязки сервера.

---

## Лицензия

Все права защищены. Разработано для проекта **HybridAC**.
