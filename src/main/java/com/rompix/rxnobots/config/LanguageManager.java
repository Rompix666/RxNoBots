package com.rompix.rxnobots.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final Path dataDirectory;
    private final Logger logger;
    private String currentLanguage;
    private CommentedConfigurationNode mainNode;     // узел для выбранного языка
    private CommentedConfigurationNode fallbackNode; // узел для en.yml
    private final Map<String, String> messageCache = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LanguageManager(Path dataDirectory, String language, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.currentLanguage = language;
        this.logger = logger;
        loadLanguages();
    }

    /**
     * Перезагружает языковые файлы (очищает кэш и загружает заново).
     */
    public void reload() {
        loadLanguages();
        logger.info("Language files reloaded (current: {})", currentLanguage);
    }

    private void loadLanguages() {
        Path langDir = dataDirectory.resolve("lang");
        try {
            if (!Files.exists(langDir)) {
                Files.createDirectories(langDir);
            }

            // Сохраняем стандартные языки из ресурсов
            saveResource(langDir, "tr.yml");
            saveResource(langDir, "en.yml");

            // Загружаем основной язык
            Path mainLangFile = langDir.resolve(currentLanguage + ".yml");
            if (!Files.exists(mainLangFile)) {
                logger.warn("Language file {}.yml not found, falling back to en.yml", currentLanguage);
                mainLangFile = langDir.resolve("en.yml");
                currentLanguage = "en";
            }
            YamlConfigurationLoader mainLoader = YamlConfigurationLoader.builder()
                    .path(mainLangFile)
                    .build();
            mainNode = mainLoader.load();

            // Загружаем fallback (английский), если он не совпадает с основным
            if (!currentLanguage.equals("en")) {
                Path enFile = langDir.resolve("en.yml");
                if (Files.exists(enFile)) {
                    YamlConfigurationLoader enLoader = YamlConfigurationLoader.builder()
                            .path(enFile)
                            .build();
                    fallbackNode = enLoader.load();
                } else {
                    fallbackNode = null;
                    logger.warn("Fallback en.yml not found, some messages may be missing");
                }
            } else {
                fallbackNode = null; // fallback не нужен
            }

            messageCache.clear();
            logger.info("Loaded language: {}", currentLanguage);
        } catch (IOException e) {
            logger.error("Could not load language files", e);
            mainNode = null;
            fallbackNode = null;
        }
    }

    private void saveResource(Path langDir, String resourceName) {
        Path target = langDir.resolve(resourceName);
        if (!Files.exists(target)) {
            try (InputStream in = getClass().getResourceAsStream("/lang/" + resourceName)) {
                if (in != null) {
                    Files.copy(in, target);
                    logger.info("Created default language file: {}", resourceName);
                } else {
                    logger.warn("Resource /lang/{} not found in JAR", resourceName);
                }
            } catch (IOException e) {
                logger.error("Could not save default language file " + resourceName, e);
            }
        }
    }

    /**
     * Получить сырое сообщение (без подстановок) по пути, например "verification.welcome".
     * Сначала ищет в основном языке, потом в fallback (en). Если не найдено – возвращает заглушку.
     */
    public String getRawMessage(String path) {
        // Проверяем кэш
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }

        String fullPath = path.startsWith("messages.") ? path : "messages." + path;
        String message = null;

        // Ищем в основном языке
        if (mainNode != null) {
            message = getMessageFromNode(mainNode, fullPath.split("\\."));
        }

        // Если не нашли и есть fallback – ищем там
        if (message == null && fallbackNode != null) {
            message = getMessageFromNode(fallbackNode, fullPath.split("\\."));
            if (message != null) {
                logger.debug("Message '{}' used from fallback en.yml", path);
            }
        }

        // Если всё равно нет – заглушка
        if (message == null) {
            message = "Missing message: " + path;
            logger.warn("Missing message key: {}", path);
        }

        messageCache.put(path, message);
        return message;
    }

    private String getMessageFromNode(CommentedConfigurationNode node, String[] path) {
        CommentedConfigurationNode current = node;
        for (String key : path) {
            current = current.node(key);
            if (current.virtual()) { // если узла нет
                return null;
            }
        }
        return current.getString();
    }

    /**
     * Получить Component сообщение с поддержкой MiniMessage и legacy & кодов.
     */
    public Component getMessage(String path) {
        String raw = getRawMessage(path);
        return parseMessage(raw);
    }

    /**
     * Получить сообщение с плейсхолдерами (%ключ%).
     * @param path ключ сообщения
     * @param placeholders Map с заменами
     */
    public Component getMessage(String path, Map<String, String> placeholders) {
        String raw = getRawMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            raw = raw.replace("%" + entry.getKey() + "%", value);
        }
        return parseMessage(raw);
    }

    /**
     * Удобный вариант: getMessage("key", "player", "Alex", "time", "5").
     * Чередуются ключ-значение.
     */
    public Component getMessage(String path, String... replacements) {
        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Replacements must be key-value pairs");
        }
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            map.put(replacements[i], replacements[i + 1]);
        }
        return getMessage(path, map);
    }

    /**
     * Парсит строку с поддержкой MiniMessage и legacy (&) форматов.
     * Сначала пробует MiniMessage, при ошибке – парсит как legacy.
     */
    private Component parseMessage(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        try {
            // Пытаемся распарсить как MiniMessage (поддерживает <...> теги)
            return miniMessage.deserialize(raw);
        } catch (Exception e) {
            // Если не удалось, парсим как legacy с символом '&'
            try {
                return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
            } catch (Exception ex) {
                logger.warn("Failed to parse message '{}' as both MiniMessage and legacy", raw, ex);
                return Component.text(raw); // fallback — обычный текст
            }
        }
    }
}