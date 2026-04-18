package com.rompix.rxnobots.commands;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.rompix.rxnobots.verification.VerificationSession;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AdminCommands implements SimpleCommand {

    private final RxNoBotsPlugin plugin;

    public AdminCommands(RxNoBotsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("rxnobots.admin")) {
            source.sendMessage(plugin.getLanguageManager().getMessage("errors.no-permission"));
            return;
        }

        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                source.sendMessage(plugin.getLanguageManager().getMessage("admin.reload-success"));
            }

            case "verify" -> {
                if (args.length < 2) {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.usage-verify"));
                    return;
                }
                handleVerify(source, args[1]);
            }

            case "reset" -> {
                if (args.length < 2) {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.usage-reset"));
                    return;
                }
                handleReset(source, args[1]);
            }

            case "timeout" -> {
                if (args.length < 3) {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.usage-timeout"));
                    return;
                }
                handleTimeout(source, args[1], args[2]);
            }

            case "bypass" -> {
                if (args.length < 2) {
                    source.sendMessage(plugin.getLanguageManager().getMessage("admin.usage-bypass"));
                    return;
                }
                handleBypass(source, args[1]);
            }

            case "stats" -> handleStats(source);

            case "session" -> {
                if (args.length < 3) {
                    sendSessionHelp(source);
                    return;
                }
                handleSession(source, args);
            }

            case "cache" -> {
                if (args.length < 2) {
                    source.sendMessage(Component.text("§cUsage: /rnb cache clear <player>"));
                    return;
                }
                handleCache(source, args[1]);
            }

            default -> source.sendMessage(plugin.getLanguageManager().getMessage("errors.unknown-command"));
        }
    }

    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("§6=== RxNoBots Help ==="));
        source.sendMessage(Component.text("§e/rnb reload §7- Reload config"));
        source.sendMessage(Component.text("§e/rnb verify <player> §7- Mark player as verified"));
        source.sendMessage(Component.text("§e/rnb reset <player> §7- Reset player's verification data"));
        source.sendMessage(Component.text("§e/rnb timeout <player> <seconds> §7- Apply timeout"));
        source.sendMessage(Component.text("§e/rnb bypass <player> §7- Toggle bypass"));
        source.sendMessage(Component.text("§e/rnb stats §7- Show statistics"));
        source.sendMessage(Component.text("§e/rnb session info <player> §7- Show session details"));
        source.sendMessage(Component.text("§e/rnb session end <player> §7- Force end verification session"));
        source.sendMessage(Component.text("§e/rnb cache clear <player> §7- Clear cached player data"));
    }

    private void sendSessionHelp(CommandSource source) {
        source.sendMessage(Component.text("§6=== Session Commands ==="));
        source.sendMessage(Component.text("§e/rnb session info <player> §7- Show session details"));
        source.sendMessage(Component.text("§e/rnb session end <player> §7- Force end verification session"));
    }

    // ===== VERIFY =====
    private void handleVerify(CommandSource source, String playerName) {
        Optional<Player> online = plugin.getServer().getPlayer(playerName);

        if (online.isPresent()) {
            updateVerification(source, online.get().getUniqueId(), playerName, true);
            return;
        }

        plugin.getDatabaseManager().getPlayerDataByUsername(playerName)
                .exceptionally(e -> {
                    plugin.getLogger().error("DB error", e);
                    return Optional.empty();
                })
                .thenAccept(opt -> {
                    if (opt.isPresent()) {
                        var data = opt.get();
                        data.setVerified(true);
                        data.resetFailedAttempts();
                        data.resetTotalAttempts();
                        plugin.getDatabaseManager().updatePlayerData(data);
                        source.sendMessage(Component.text("§aPlayer verified: " + playerName));
                    } else {
                        source.sendMessage(Component.text("§cPlayer not found"));
                    }
                });
    }

    // ===== RESET =====
    private void handleReset(CommandSource source, String playerName) {
        Optional<Player> online = plugin.getServer().getPlayer(playerName);

        if (online.isPresent()) {
            plugin.getVerificationManager().removeSession(online.get().getUniqueId());
            resetPlayer(source, online.get().getUniqueId(), playerName);
            return;
        }

        plugin.getDatabaseManager().getPlayerDataByUsername(playerName)
                .thenAccept(opt -> {
                    if (opt.isPresent()) {
                        var data = opt.get();
                        data.setVerified(false);
                        data.resetFailedAttempts();
                        data.resetTotalAttempts();
                        data.setTimeoutUntil(null);
                        data.setVerifiedUntil(null);

                        plugin.getDatabaseManager().updatePlayerData(data);
                        source.sendMessage(Component.text("§ePlayer reset: " + playerName));
                    } else {
                        source.sendMessage(Component.text("§cPlayer not found"));
                    }
                });
    }

    // ===== TIMEOUT =====
    private void handleTimeout(CommandSource source, String playerName, String secondsStr) {
        int seconds;

        try {
            seconds = Integer.parseInt(secondsStr);
        } catch (Exception e) {
            source.sendMessage(Component.text("§cInvalid number"));
            return;
        }

        Optional<Player> online = plugin.getServer().getPlayer(playerName);

        if (online.isPresent()) {
            plugin.getVerificationManager().removeSession(online.get().getUniqueId());
            applyTimeout(source, online.get().getUniqueId(), playerName, seconds);
            return;
        }

        plugin.getDatabaseManager().getPlayerDataByUsername(playerName)
                .thenAccept(opt -> {
                    if (opt.isPresent()) {
                        var data = opt.get();
                        data.setTimeoutUntil(new Timestamp(System.currentTimeMillis() + seconds * 1000L));
                        plugin.getDatabaseManager().updatePlayerData(data);
                        source.sendMessage(Component.text("§cTimeout applied: " + playerName));
                    } else {
                        source.sendMessage(Component.text("§cPlayer not found"));
                    }
                });
    }

    // ===== BYPASS =====
    private void handleBypass(CommandSource source, String playerName) {
        Optional<Player> online = plugin.getServer().getPlayer(playerName);

        if (online.isPresent()) {
            toggleBypass(source, online.get().getUniqueId(), playerName);
            return;
        }

        plugin.getDatabaseManager().getPlayerDataByUsername(playerName)
                .thenAccept(opt -> {
                    if (opt.isPresent()) {
                        var data = opt.get();
                        boolean state = !data.isBypassGranted();
                        data.setBypassGranted(state);
                        plugin.getDatabaseManager().updatePlayerData(data);
                        source.sendMessage(Component.text("§eBypass " + (state ? "enabled" : "disabled") + ": " + playerName));
                    } else {
                        source.sendMessage(Component.text("§cPlayer not found"));
                    }
                });
    }

    // ===== SESSION MANAGEMENT =====
    private void handleSession(CommandSource source, String[] args) {
        if (args.length < 3) {
            sendSessionHelp(source);
            return;
        }
        String subCmd = args[1].toLowerCase();
        String playerName = args[2];
        Optional<Player> online = plugin.getServer().getPlayer(playerName);

        switch (subCmd) {
            case "info" -> {
                if (online.isPresent()) {
                    showSessionInfo(source, online.get());
                } else {
                    source.sendMessage(Component.text("§cPlayer not online"));
                }
            }
            case "end" -> {
                if (online.isPresent()) {
                    UUID uuid = online.get().getUniqueId();
                    plugin.getVerificationManager().removeSession(uuid);
                    source.sendMessage(Component.text("§aVerification session ended for " + playerName));
                } else {
                    source.sendMessage(Component.text("§cPlayer not online"));
                }
            }
            default -> sendSessionHelp(source);
        }
    }

    private void showSessionInfo(CommandSource source, Player player) {
        VerificationSession session = plugin.getVerificationManager().getSession(player.getUniqueId());
        if (session == null) {
            source.sendMessage(Component.text("§eNo active verification session for " + player.getUsername()));
            return;
        }
        source.sendMessage(Component.text("§6=== Session Info for " + player.getUsername() + " ==="));
        source.sendMessage(Component.text("§eStage: §f" + session.getCurrentStage()));
        source.sendMessage(Component.text("§eChat completed: §f" + session.isChatCompleted()));
        source.sendMessage(Component.text("§eMovement completed: §f" + session.isMovementCompleted()));
        source.sendMessage(Component.text("§eTarget code: §f" + session.getTargetCode()));
    }

    // ===== CACHE MANAGEMENT =====
    private void handleCache(CommandSource source, String playerName) {
        Optional<Player> online = plugin.getServer().getPlayer(playerName);
        if (online.isPresent()) {
            plugin.getDatabaseManager().removeCachedPlayerData(online.get().getUniqueId());
            source.sendMessage(Component.text("§aCache cleared for " + playerName));
        } else {
            plugin.getDatabaseManager().getPlayerDataByUsername(playerName)
                    .thenAccept(opt -> {
                        if (opt.isPresent()) {
                            plugin.getDatabaseManager().removeCachedPlayerData(opt.get().getUuid());
                            source.sendMessage(Component.text("§aCache cleared for " + playerName));
                        } else {
                            source.sendMessage(Component.text("§cPlayer not found"));
                        }
                    });
        }
    }

    // ===== STATS =====
    private void handleStats(CommandSource source) {
        CompletableFuture<Integer> total = safe(plugin.getDatabaseManager().getTotalPlayers());
        CompletableFuture<Integer> verified = safe(plugin.getDatabaseManager().getVerifiedPlayers());
        CompletableFuture<Integer> timeout = safe(plugin.getDatabaseManager().getTimedOutPlayers());

        int active = plugin.getVerificationManager().getSessionCount();

        CompletableFuture.allOf(total, verified, timeout).thenRun(() -> {
            source.sendMessage(Component.text("§6=== Stats ==="));
            source.sendMessage(Component.text("Total: " + total.join()));
            source.sendMessage(Component.text("Verified: " + verified.join()));
            source.sendMessage(Component.text("Timeout: " + timeout.join()));
            source.sendMessage(Component.text("Active: " + active));
        });
    }

    private <T> CompletableFuture<T> safe(CompletableFuture<T> future) {
        return future.exceptionally(e -> {
            plugin.getLogger().error("Async error", e);
            return null;
        });
    }

    // ===== ONLINE HELPERS =====
    private void updateVerification(CommandSource source, UUID uuid, String name, boolean verified) {
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(opt -> {
            if (opt.isPresent()) {
                var data = opt.get();
                data.setVerified(verified);
                plugin.getDatabaseManager().updatePlayerData(data);
                source.sendMessage(Component.text("§aUpdated: " + name));
            } else {
                source.sendMessage(Component.text("§cPlayer data not found"));
            }
        });
    }

    private void resetPlayer(CommandSource source, UUID uuid, String name) {
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(opt -> {
            if (opt.isPresent()) {
                var data = opt.get();
                data.setVerified(false);
                data.resetFailedAttempts();
                data.resetTotalAttempts();
                plugin.getDatabaseManager().updatePlayerData(data);
                source.sendMessage(Component.text("§eReset: " + name));
            } else {
                source.sendMessage(Component.text("§cPlayer data not found"));
            }
        });
    }

    private void applyTimeout(CommandSource source, UUID uuid, String name, int seconds) {
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(opt -> {
            if (opt.isPresent()) {
                var data = opt.get();
                data.setTimeoutUntil(new Timestamp(System.currentTimeMillis() + seconds * 1000L));
                plugin.getDatabaseManager().updatePlayerData(data);
                source.sendMessage(Component.text("§cTimeout: " + name));
            } else {
                source.sendMessage(Component.text("§cPlayer data not found"));
            }
        });
    }

    private void toggleBypass(CommandSource source, UUID uuid, String name) {
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(opt -> {
            if (opt.isPresent()) {
                var data = opt.get();
                boolean state = !data.isBypassGranted();
                data.setBypassGranted(state);
                plugin.getDatabaseManager().updatePlayerData(data);
                source.sendMessage(Component.text("§eBypass " + (state ? "ON" : "OFF") + ": " + name));
            } else {
                source.sendMessage(Component.text("§cPlayer data not found"));
            }
        });
    }

    // TAB COMPLETION
    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("reload", "verify", "reset", "timeout", "bypass", "stats", "session", "cache");
        }

        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("session")) {
                return List.of("info", "end");
            }
            if (first.equals("cache")) {
                return List.of("clear");
            }
            return plugin.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .toList();
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("session") || args[0].equalsIgnoreCase("cache"))) {
            return plugin.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .toList();
        }

        return Collections.emptyList();
    }
}