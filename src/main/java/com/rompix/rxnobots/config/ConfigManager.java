package com.rompix.rxnobots.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ConfigManager {
    private final Path dataDirectory;
    private final Logger logger;
    private CommentedConfigurationNode rootNode;
    private final String fileName = "config.yml";

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        loadConfig();
    }

    public void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve(fileName);
            if (!Files.exists(configPath)) {
                try (InputStream in = getClass().getResourceAsStream("/" + fileName)) {
                    if (in != null) Files.copy(in, configPath);
                    else Files.createFile(configPath);
                }
            }
            rootNode = YamlConfigurationLoader.builder().path(configPath).build().load();
            logger.info("Config loaded successfully");
        } catch (IOException e) {
            logger.error("Could not load config.yml, using empty configuration", e);
            rootNode = CommentedConfigurationNode.root();
        }
    }

    public String getLanguage() { return getNodeValue("general", "language", String.class, "en"); }
    public boolean isDebug() { return getNodeValue("general", "debug", Boolean.class, false); }
    public String getDatabaseType() { return getNodeValue("database", "type", String.class, "sqlite"); }
    public String getSQLiteFile() { return getNodeValue("database", "sqlite", "file", String.class, "rxnobots.db"); }
    public String getMySQLHost() { return getNodeValue("database", "mysql", "host", String.class, "localhost"); }
    public int getMySQLPort() { return getNodeValue("database", "mysql", "port", Integer.class, 3306); }
    public String getMySQLDatabase() { return getNodeValue("database", "mysql", "database", String.class, "rxnobots"); }
    public String getMySQLUsername() { return getNodeValue("database", "mysql", "username", String.class, "root"); }
    public String getMySQLPassword() { return getNodeValue("database", "mysql", "password", String.class, ""); }
    public int getMySQLPoolSize() { return getNodeValue("database", "mysql", "connection-pool-size", Integer.class, 5); }
    public String getLimboHost() { return getNodeValue("limbo", "host", String.class, "127.0.0.1"); }
    public int getLimboPort() { return getNodeValue("limbo", "port", Integer.class, 25566); }
    public String getLimboBrand() { return getNodeValue("limbo", "brand-name", String.class, "&4RxNoBots &7Verification"); }
    public int getCodeLength() { return getNodeValue("verification", "code", "length", Integer.class, 4); }
    public String getCodeCharacters() { return getNodeValue("verification", "code", "characters", String.class, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"); }
    public boolean isCodeCaseSensitive() { return getNodeValue("verification", "code", "case-sensitive", Boolean.class, false); }
    public String getVerificationType() { return getNodeValue("verification", "type", String.class, "HYBRID"); }

    public List<String> getMovementDirections() {
        if (rootNode == null) return List.of("up:2", "left:2");
        try {
            return rootNode.node("verification", "movement", "directions")
                    .getList(String.class, List.of("up:2", "left:2"));
        } catch (SerializationException e) {
            logger.warn("Could not deserialize movement directions", e);
            return List.of("up:2", "left:2");
        }
    }

    public double getMovementTolerance() { return getNodeValue("verification", "movement", "tolerance", Double.class, 15.0); }
    public int getResponseTimeout() { return getNodeValue("verification", "movement", "response-timeout", Integer.class, 30); }
    public boolean isKickOnTimeout() { return getNodeValue("verification", "movement", "kick-on-timeout", Boolean.class, true); }

    public double getDirectionAngle(String direction, String type) {
        return getNodeValue("verification", "movement", "angles", direction, type, Double.class, 0.0);
    }

    public int getMaxAttempts() { return getNodeValue("verification", "attempts", "max-attempts", Integer.class, 3); }
    public int getMaxSessions() { return getNodeValue("verification", "attempts", "max-sessions", Integer.class, 3); }
    public int getTimeoutDuration() { return getNodeValue("verification", "timeout", "duration", Integer.class, 600); }
    public String getTargetServer() { return getNodeValue("verification", "success", "target-server", String.class, "lobby"); }
    public boolean isTrackByUser() { return getNodeValue("verification", "cooldown", "track-by-user", Boolean.class, true); }
    public boolean isTrackByIP() { return getNodeValue("verification", "cooldown", "track-by-ip", Boolean.class, true); }
    public int getCooldownDuration() { return getNodeValue("verification", "cooldown", "duration", Integer.class, 86400); }
    public int getRememberDuration() { return getNodeValue("verification", "success", "remember-duration", Integer.class, 86400); }

    public String getSuccessAction() {
        return getNodeValue("verification", "success", "action", String.class, "DISCONNECT");
    }

    public boolean isMovementRandom() {
        return getNodeValue("verification", "movement", "random", Boolean.class, false);
    }

    public int getMovementMinDuration() {
        return getNodeValue("verification", "movement", "min-duration", Integer.class, 2);
    }

    public int getMovementMaxDuration() {
        return getNodeValue("verification", "movement", "max-duration", Integer.class, 4);
    }

    public List<String> getMovementAvailableDirections() {
        if (rootNode == null) return List.of("up", "down", "left", "right");
        try {
            return rootNode.node("verification", "movement", "available-directions")
                    .getList(String.class, List.of("up", "down", "left", "right"));
        } catch (SerializationException e) {
            logger.warn("Could not deserialize available directions", e);
            return List.of("up", "down", "left", "right");
        }
    }

    public String getBypassPermission() { return getNodeValue("bypass", "permission", String.class, "rxnobots.bypass"); }

    public List<String> getIpWhitelist() {
        if (rootNode == null) return Collections.emptyList();
        try {
            return rootNode.node("bypass", "ip-whitelist").getList(String.class, Collections.emptyList());
        } catch (SerializationException e) {
            logger.warn("Could not load IP whitelist", e);
            return Collections.emptyList();
        }
    }

    public int getCleanupInterval() { return getNodeValue("performance", "cleanup-interval", Integer.class, 3600); }
    public int getMaxSessionsCount() { return getNodeValue("performance", "max-sessions", Integer.class, 500); }
    public int getSessionTimeout() { return getNodeValue("performance", "session-timeout", Integer.class, 300); }
    public int getMaxVerificationTime() { return getNodeValue("security", "max-verification-time", Integer.class, 120); }
    public int getAntiSpamDelay() { return getNodeValue("security", "anti-spam-delay", Integer.class, 1000); }
    public boolean isLogAttempts() { return getNodeValue("security", "log-attempts", Boolean.class, true); }

    @SuppressWarnings("unchecked")
    private <T> T getNodeValue(String first, String second, Class<T> type, T def) {
        if (rootNode == null) return def;
        try {
            Object v = rootNode.node(first, second).get(type);
            return v != null ? (T) v : def;
        } catch (SerializationException e) {
            logger.warn("Failed to read config value at {}.{}, using default", first, second, e);
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getNodeValue(String first, String second, String third, Class<T> type, T def) {
        if (rootNode == null) return def;
        try {
            Object v = rootNode.node(first, second, third).get(type);
            return v != null ? (T) v : def;
        } catch (SerializationException e) {
            logger.warn("Failed to read config value at {}.{}.{}, using default", first, second, third, e);
            return def;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getNodeValue(String first, String second, String third, String fourth, String fifth, Class<T> type, T def) {
        if (rootNode == null) return def;
        try {
            Object v = rootNode.node(first, second, third, fourth, fifth).get(type);
            return v != null ? (T) v : def;
        } catch (SerializationException e) {
            logger.warn("Failed to read config value at {}.{}.{}.{}.{}, using default", first, second, third, fourth, fifth, e);
            return def;
        }
    }

    public CommentedConfigurationNode getRoot() { return rootNode; }
}