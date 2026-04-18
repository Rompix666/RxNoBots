package com.rompix.rxnobots.limbo;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.player.GameMode;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LimboFilter implements LimboSessionHandler {
    private final RxNoBotsPlugin plugin;
    private final Player player;
    private final AtomicBoolean spawned = new AtomicBoolean(false);
    private final AtomicReference<LimboPlayer> limboPlayerRef = new AtomicReference<>();
    private final Consumer<UUID> onLeaveCallback;
    
    private static final double SPAWN_X = 0.5;
    private static final double SPAWN_Y = 64.0;
    private static final double SPAWN_Z = 0.5;
    
    private volatile float lastYaw = 0.0f;
    private volatile float lastPitch = 0.0f;
    
    private ScheduledTask enforcerTask;
    private ScheduledTask fallbackTask;

    public LimboFilter(RxNoBotsPlugin plugin, Player player, Consumer<UUID> onLeaveCallback) {
        this.plugin = plugin;
        this.player = player;
        this.onLeaveCallback = onLeaveCallback;
        
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("LimboFilter created for player: {}", player.getUsername());
        }
        
        // Fallback на случай, если onSpawn не вызовется
        fallbackTask = plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (!spawned.get()) {
                    plugin.getLogger().warn("No spawn callback called for {} - using fallback", player.getUsername());
                    handleSpawn(null);
                }
            })
            .delay(java.time.Duration.ofSeconds(2))
            .schedule();
    }

    public void onSpawn(Limbo server, LimboPlayer limboPlayer) {
        handleSpawn(limboPlayer);
    }
    
    public void onSpawn(LimboPlayer limboPlayer) {
        handleSpawn(limboPlayer);
    }
    
    private void handleSpawn(LimboPlayer limboPlayer) {
        if (!spawned.compareAndSet(false, true)) {
            return;
        }
        
        // Отменяем fallback, если он ещё не выполнился
        if (fallbackTask != null) {
            fallbackTask.cancel();
            fallbackTask = null;
        }
        
        plugin.getLogger().info("Player {} spawned in Limbo!", player.getUsername());
        
        if (limboPlayer != null) {
            limboPlayerRef.set(limboPlayer);
            try {
                limboPlayer.setGameMode(GameMode.ADVENTURE);
                limboPlayer.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, 0.0f, 0.0f);
                lastYaw = 0.0f;
                lastPitch = 0.0f;
                startPositionEnforcer();
            } catch (Exception e) {
                plugin.getLogger().warn("Could not set gamemode/position for {}: {}", player.getUsername(), e.getMessage());
            }
        }
        
        try {
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome"));
        } catch (Exception e) {
            plugin.getLogger().error("Error sending welcome message to " + player.getUsername(), e);
        }
        
        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Starting verification for {}", player.getUsername());
                }
                plugin.getVerificationManager().startVerification(player);
            })
            .delay(java.time.Duration.ofSeconds(1))
            .schedule();
    }
    
    private void startPositionEnforcer() {
        enforcerTask = plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                LimboPlayer lp = limboPlayerRef.get();
                if (lp != null && spawned.get()) {
                    try {
                        lp.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, lastYaw, lastPitch);
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getLogger().debug("Teleport enforcer failed for {}: {}", player.getUsername(), e.getMessage());
                        }
                    }
                }
            })
            .repeat(java.time.Duration.ofMillis(200))
            .schedule();
    }
    
    private void stopPositionEnforcer() {
        if (enforcerTask != null) {
            enforcerTask.cancel();
            enforcerTask = null;
        }
        if (fallbackTask != null) {
            fallbackTask.cancel();
            fallbackTask = null;
        }
    }
    
    @Override
    public void onDisconnect() {
        stopPositionEnforcer();
        plugin.getLogger().info("Player {} disconnected from Limbo", player.getUsername());
        // Приостанавливаем сессию верификации
        plugin.getVerificationManager().pauseSession(player.getUniqueId());
        limboPlayerRef.set(null);
        
        // Сообщаем LimboManager, что игрок покинул лимбо
        if (onLeaveCallback != null) {
            onLeaveCallback.accept(player.getUniqueId());
        }
    }
    
    @Override
    public void onChat(String message) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Player {} sent chat in Limbo: {}", player.getUsername(), message);
        }
        var session = plugin.getVerificationManager().getSession(player.getUniqueId());
        if (session != null) {
            session.handleChatMessage(message);
        }
    }
    
    @Override
    public void onMove(double x, double y, double z, float yaw, float pitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        
        double deltaX = Math.abs(x - SPAWN_X);
        double deltaZ = Math.abs(z - SPAWN_Z);
        double deltaY = Math.abs(y - SPAWN_Y);
        
        if (deltaX > 0.02 || deltaZ > 0.02 || deltaY > 0.05) {
            LimboPlayer lp = limboPlayerRef.get();
            if (lp != null) {
                try {
                    lp.teleport(SPAWN_X, SPAWN_Y, SPAWN_Z, yaw, pitch);
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().debug("Could not teleport {} back to spawn: {}", player.getUsername(), e.getMessage());
                    }
                }
            }
        }
        
        var session = plugin.getVerificationManager().getSession(player.getUniqueId());
        if (session != null) {
            session.handleMovement(SPAWN_X, SPAWN_Y, SPAWN_Z, yaw, pitch);
        }
    }
    
    @Override
    public String toString() {
        return "LimboFilter{player=" + player.getUsername() + ", spawned=" + spawned.get() + "}";
    }
}