package com.rompix.rxnobots.verification;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.rompix.rxnobots.database.PlayerData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VerificationManager {
    private final RxNoBotsPlugin plugin;
    private final Map<UUID, VerificationSession> sessions = new ConcurrentHashMap<>();

    public VerificationManager(RxNoBotsPlugin plugin) {
        this.plugin = plugin;
    }

    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Начинает верификацию для игрока или восстанавливает прерванную сессию.
     * @param player игрок
     */
    public void startVerification(Player player) {
        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player {} already has a verification session, ignoring duplicate start", player.getUsername());
            }
            return;
        }

        // Загружаем данные игрока
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                // Проверяем, есть ли сохранённый прогресс верификации
                if (data.getVerificationStage() != null && !data.getVerificationStage().equals("COMPLETED")) {
                    // Восстанавливаем сессию
                    restoreSession(player, data);
                    return;
                }

                // Новая сессия: увеличиваем счётчик сессионных попыток
                data.incrementTotalAttempts();
                // Сбрасываем старый прогресс, если был
                data.resetVerificationProgress();
                plugin.getDatabaseManager().updatePlayerData(data).thenRun(() -> {
                    VerificationSession session = new VerificationSession(player, plugin);
                    sessions.put(uuid, session);
                    plugin.getLogger().info("Started verification session for {} (attempts: {})", player.getUsername(), data.getTotalAttempts());
                }).exceptionally(ex -> {
                    plugin.getLogger().error("Failed to update attempts count for " + player.getUsername(), ex);
                    player.disconnect(plugin.getLanguageManager().getMessage("errors.database-error"));
                    return null;
                });
            } else {
                // Данных нет – создаём запись
                plugin.getDatabaseManager().createPlayerData(uuid, player.getUsername()).thenRun(() -> {
                    startVerification(player);
                }).exceptionally(ex -> {
                    plugin.getLogger().error("Failed to create player data for " + player.getUsername(), ex);
                    player.disconnect(plugin.getLanguageManager().getMessage("errors.database-error"));
                    return null;
                });
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error loading player data for startVerification " + player.getUsername(), ex);
            player.disconnect(plugin.getLanguageManager().getMessage("errors.database-error"));
            return null;
        });
    }

    /**
     * Восстанавливает прерванную сессию верификации.
     */
    private void restoreSession(Player player, PlayerData data) {
        UUID uuid = player.getUniqueId();
        plugin.getLogger().info("Restoring verification session for {} at stage: {}", player.getUsername(), data.getVerificationStage());

        VerificationSession session = new VerificationSession(player, plugin, data);
        sessions.put(uuid, session);
    }

    /**
     * Сохраняет прогресс сессии в базу данных и удаляет из памяти.
     * Вызывается при отключении игрока во время верификации.
     */
    public void pauseSession(UUID uuid) {
        VerificationSession session = sessions.remove(uuid);
        if (session != null) {
            session.saveProgressToDatabase();
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().debug("Paused verification session for {}", uuid);
            }
        }
        // При паузе игрок сам дисконнектится, LimboFilter.onDisconnect уже уведомит LimboManager
    }

    public VerificationSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    /**
     * Полностью удаляет сессию и сбрасывает прогресс в БД.
     * Также уведомляет LimboManager, что игрок больше не в лимбо.
     */
    public void removeSession(UUID uuid) {
        VerificationSession session = sessions.remove(uuid);
        if (session != null) {
            session.cancel();
        }
        // Сбрасываем сохранённый прогресс в БД
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.resetVerificationProgress();
                plugin.getDatabaseManager().updatePlayerData(data);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().debug("Cleared verification progress in DB for {}", uuid);
                }
            }
        });
        
        // Уведомляем LimboManager, что игрок покинул лимбо (если он был там)
        plugin.getLimboManager().onPlayerLeftLimbo(uuid);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().debug("Removed verification session for {}", uuid);
        }
    }

    /**
     * Сохраняет текущий прогресс сессии в БД без удаления из памяти.
     */
    public void saveSessionProgress(UUID uuid) {
        VerificationSession session = sessions.get(uuid);
        if (session != null) {
            session.saveProgressToDatabase();
        }
    }

    public void handleSuccess(Player player) {
        UUID uuid = player.getUniqueId();
        // removeSession вызовет onPlayerLeftLimbo
        removeSession(uuid);

        String playerIP = player.getRemoteAddress().getAddress().getHostAddress();
        String username = player.getUsername();

        CompletableFuture<Void> dbUpdate = plugin.getDatabaseManager().getPlayerData(uuid)
            .thenCompose(optData -> {
                if (optData.isPresent()) {
                    PlayerData data = optData.get();
                    data.setVerified(true);
                    data.resetFailedAttempts();
                    data.resetTotalAttempts();
                    data.resetVerificationProgress(); // очищаем прогресс после успеха

                    long cooldownMillis = System.currentTimeMillis() + (plugin.getConfigManager().getCooldownDuration() * 1000L);
                    data.setVerifiedUntil(new Timestamp(cooldownMillis));
                    data.setLastIP(playerIP);
                    data.setUsername(username);

                    return plugin.getDatabaseManager().updatePlayerData(data);
                } else {
                    plugin.getLogger().warn("No player data found for {} on success, creating new record", username);
                    return plugin.getDatabaseManager().createPlayerData(uuid, username);
                }
            });

        dbUpdate.thenRun(() -> {
            plugin.getLogger().info("Player {} ({}) verified successfully. Cooldown until: {}",
                    username, playerIP, new Timestamp(System.currentTimeMillis() + plugin.getConfigManager().getCooldownDuration() * 1000L));

            String successAction = plugin.getConfigManager().getSuccessAction();
            if ("SERVER".equalsIgnoreCase(successAction)) {
                String targetServerName = plugin.getConfigManager().getTargetServer();
                Optional<RegisteredServer> target = plugin.getServer().getServer(targetServerName);
                if (target.isPresent()) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome-server"));
                    player.createConnectionRequest(target.get()).connect().thenAccept(result -> {
                        if (!result.isSuccessful()) {
                            plugin.getLogger().warn("Failed to send {} to target server {}, disconnecting", username, targetServerName);
                            player.disconnect(plugin.getLanguageManager().getMessage("verification.final-success"));
                        }
                    });
                } else {
                    plugin.getLogger().warn("Target server {} not found, disconnecting {}", targetServerName, username);
                    player.disconnect(plugin.getLanguageManager().getMessage("verification.final-success"));
                }
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.reconnect"));
                plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> player.disconnect(plugin.getLanguageManager().getMessage("verification.final-success")))
                    .delay(java.time.Duration.ofSeconds(2))
                    .schedule();
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error updating database on verification success for " + username, ex);
            player.disconnect(Component.text("Internal error during verification. Please reconnect."));
            return null;
        });
    }

    public void handleFail(Player player, int attemptsLeft) {
        if (attemptsLeft <= 0) {
            handleTimeout(player);
        } else {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player {} failed verification, attempts left: {}", player.getUsername(), attemptsLeft);
            }
        }
    }

    public void handleTimeout(Player player) {
        UUID uuid = player.getUniqueId();
        // removeSession вызовет onPlayerLeftLimbo
        removeSession(uuid);

        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                long timeoutMillis = System.currentTimeMillis() + (plugin.getConfigManager().getTimeoutDuration() * 1000L);
                data.setTimeoutUntil(new Timestamp(timeoutMillis));
                data.incrementFailedAttempts();
                data.resetVerificationProgress(); // сбрасываем прогресс при таймауте
                plugin.getDatabaseManager().updatePlayerData(data);
                plugin.getLogger().info("Player {} timed out for {} minutes", player.getUsername(), plugin.getConfigManager().getTimeoutDuration() / 60);
            } else {
                plugin.getLogger().warn("No player data for {} on timeout", player.getUsername());
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error updating timeout in DB for " + player.getUsername(), ex);
            return null;
        });

        Map<String, String> placeholders = new HashMap<>();
        int timeoutMinutes = plugin.getConfigManager().getTimeoutDuration() / 60;
        placeholders.put("time", String.valueOf(timeoutMinutes));
        player.disconnect(plugin.getLanguageManager().getMessage("verification.timeout", placeholders));
    }
}