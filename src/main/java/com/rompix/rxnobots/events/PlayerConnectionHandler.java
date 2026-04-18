package com.rompix.rxnobots.events;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.rompix.rxnobots.database.PlayerData;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerConnectionHandler {
    private final RxNoBotsPlugin plugin;

    public PlayerConnectionHandler(RxNoBotsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();

        plugin.getDatabaseManager().createPlayerData(uuid, username)
                .thenRun(() -> plugin.getDatabaseManager().getPlayerData(uuid)
                        .thenAccept(optData -> {
                            if (optData.isPresent()) {
                                PlayerData data = optData.get();
                                if (data.updateUsernameIfNeeded(username))
                                    plugin.getDatabaseManager().updatePlayerData(data);
                                if (data.isTimedOut()) {
                                    long remaining = (data.getTimeoutUntil().getTime() - System.currentTimeMillis()) / 1000 / 60 + 1;
                                    event.setResult(LoginEvent.ComponentResult.denied(
                                            plugin.getLanguageManager().getMessage("verification.timeout",
                                                    java.util.Collections.singletonMap("time", String.valueOf(remaining)))));
                                }
                            }
                        }).exceptionally(ex -> {
                            plugin.getLogger().error("Error loading player data for " + username, ex);
                            return null;
                        }))
                .exceptionally(ex -> {
                    plugin.getLogger().error("Error creating player data for " + username, ex);
                    return null;
                });
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        // Только первое подключение (не реконнект между серверами)
        if (event.getPreviousServer() != null) return;

        Player player = event.getPlayer();
        String playerIP = player.getRemoteAddress().getAddress().getHostAddress();
        String username = player.getUsername();

        // Быстрая проверка байпаса и активной сессии
        if (player.hasPermission(plugin.getConfigManager().getBypassPermission()) ||
                plugin.getConfigManager().getIpWhitelist().contains(playerIP) ||
                plugin.getVerificationManager().getSession(player.getUniqueId()) != null) {
            if (plugin.getConfigManager().isDebug())
                plugin.getLogger().info("Skipping verification for {}", username);
            return;
        }

        plugin.getLogger().info("Player {} ({}) attempting initial server connect", username, playerIP);

        // Асинхронная проверка необходимости верификации (без изменения БД)
        checkIfVerificationNeeded(player, playerIP, username).thenAccept(needs -> {
            if (needs) {
                // Запрещаем подключение к целевому серверу
                event.setResult(ServerPreConnectEvent.ServerResult.denied());

                // Отправляем в лимбо с небольшой задержкой, чтобы событие завершилось
                plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> plugin.getLimboManager().sendToLimbo(player))
                        .delay(java.time.Duration.ofMillis(100))
                        .schedule();
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Error checking verification for " + username, ex);
            return null;
        });
    }

    /**
     * Проверяет, нужно ли игроку проходить верификацию.
     * НЕ изменяет базу данных – только читает.
     * @return true если нужно отправить в лимбо
     */
    private CompletableFuture<Boolean> checkIfVerificationNeeded(Player player, String playerIP, String username) {
        UUID uuid = player.getUniqueId();

        CompletableFuture<Optional<PlayerData>> futureData = plugin.getDatabaseManager().getCachedPlayerData(uuid)
                .<CompletableFuture<Optional<PlayerData>>>map(data -> CompletableFuture.completedFuture(Optional.of(data)))
                .orElseGet(() -> plugin.getDatabaseManager().getPlayerData(uuid));

        return futureData.thenApply(optData -> {
            if (!optData.isPresent()) {
                // Новый игрок – нужна верификация
                return true;
            }

            PlayerData data = optData.get();

            // Обновляем имя, если изменилось (только чтение, запись будет позже при необходимости)
            if (data.updateUsernameIfNeeded(username)) {
                plugin.getDatabaseManager().updatePlayerData(data);
            }

            // 1. Байпас по флагу
            if (data.isBypassGranted()) {
                return false;
            }

            // 2. Кулдаун (cooldown) после успешной верификации
            if (data.isInCooldown()) {
                boolean trackUser = plugin.getConfigManager().isTrackByUser();
                boolean trackIP = plugin.getConfigManager().isTrackByIP();
                String lastIP = data.getLastIP();
                boolean ipMatch = lastIP != null && lastIP.equals(playerIP);
                boolean userMatch = username.equalsIgnoreCase(data.getUsername());

                if ((trackUser && trackIP && ipMatch && userMatch) ||
                    (trackUser && userMatch) ||
                    (trackIP && ipMatch)) {
                    return false;
                }
            }

            // 3. Если у игрока есть сохранённый прогресс верификации, ему нужно продолжить
            if (data.getVerificationStage() != null && !data.getVerificationStage().equals("COMPLETED")) {
                return true;
            }

            // 4. Превышение лимита попыток (maxSessions) без активного кулдауна
            int maxSessions = plugin.getConfigManager().getMaxSessions();
            int attempts = data.getSessionAttempts();
            if (attempts >= maxSessions && !data.isInCooldown()) {
                data.setTimeoutUntil(new Timestamp(System.currentTimeMillis() + plugin.getConfigManager().getTimeoutDuration() * 1000L));
                plugin.getDatabaseManager().updatePlayerData(data);
                return false;
            }

            // 5. Если есть активный таймаут (timeout) – не пускаем
            if (data.isTimedOut()) {
                return false;
            }

            // В остальных случаях – нужна верификация
            return true;
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Вместо полного удаления сессии – приостанавливаем, сохраняя прогресс в БД
        plugin.getVerificationManager().pauseSession(uuid);
        plugin.getDatabaseManager().removeCachedPlayerData(uuid);
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().debug("Player {} disconnected, session paused", event.getPlayer().getUsername());
        }
    }
}