package com.rompix.rxnobots.limbo;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.chunk.Dimension;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LimboManager {
    private final RxNoBotsPlugin plugin;
    private final LimboFactory limboFactory;
    private final VirtualWorld limboWorld;
    private final Limbo limbo;
    
    // Множество UUID игроков, которые уже в лимбо или в процессе отправки
    private final Set<UUID> limboPlayers = ConcurrentHashMap.newKeySet();

    public LimboManager(RxNoBotsPlugin plugin) {
        this.plugin = plugin;
        this.limboFactory = (LimboFactory) plugin.getServer().getPluginManager()
                .getPlugin("limboapi")
                .flatMap(container -> container.getInstance())
                .orElseThrow(() -> new RuntimeException("LimboAPI not found! Please install LimboAPI plugin."));

        double spawnX = 0.5;
        double spawnY = 64.0;
        double spawnZ = 0.5;
        float yaw = 0.0f;
        float pitch = 0.0f;

        this.limboWorld = limboFactory.createVirtualWorld(
                Dimension.OVERWORLD,
                spawnX, spawnY, spawnZ,
                yaw, pitch
        );

        this.limbo = limboFactory.createLimbo(limboWorld);
        plugin.getLogger().info("LimboManager initialized with virtual world at ({}, {}, {})", spawnX, spawnY, spawnZ);
    }

    /**
     * Отправляет игрока в лимбо для прохождения верификации.
     * Защищён от повторных вызовов.
     * @param player игрок
     * @return true если отправка начата, false если игрок уже в лимбо или верификации
     */
    public boolean sendToLimbo(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Проверяем, не в лимбо ли уже игрок
        if (!limboPlayers.add(uuid)) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player {} is already in Limbo or being sent, ignoring", player.getUsername());
            }
            return false;
        }
        
        // Проверяем, не идёт ли уже верификация
        if (plugin.getVerificationManager().getSession(uuid) != null) {
            limboPlayers.remove(uuid); // откатываем добавление
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Player {} is already in verification, ignoring duplicate request", player.getUsername());
            }
            return false;
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Sending player {} to Limbo world", player.getUsername());
        }

        // Небольшая задержка, чтобы игрок успел завершить текущие события
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    // Проверяем, что игрок всё ещё онлайн
                    if (!player.isActive()) {
                        plugin.getLogger().info("Player {} disconnected before entering Limbo", player.getUsername());
                        limboPlayers.remove(uuid);
                        return;
                    }

                    try {
                        // Отправляем в Limbo с коллбэком для очистки
                        limbo.spawnPlayer(player, new LimboFilter(plugin, player, this::onPlayerLeftLimbo));
                    } catch (Exception e) {
                        plugin.getLogger().error("Failed to send player {} to Limbo: {}", player.getUsername(), e.getMessage(), e);
                        limboPlayers.remove(uuid);
                        if (player.isActive()) {
                            player.disconnect(plugin.getLanguageManager().getMessage("errors.limbo-error"));
                        }
                    }
                })
                .delay(Duration.ofMillis(200))
                .schedule();
        
        return true;
    }
    
    /**
     * Вызывается, когда игрок покидает лимбо (дисконнект или успешная верификация).
     */
    public void onPlayerLeftLimbo(UUID uuid) {
        limboPlayers.remove(uuid);
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().debug("Player {} removed from limbo tracking", uuid);
        }
    }
    
    /**
     * Проверяет, находится ли игрок в лимбо.
     */
    public boolean isPlayerInLimbo(UUID uuid) {
        return limboPlayers.contains(uuid);
    }
}