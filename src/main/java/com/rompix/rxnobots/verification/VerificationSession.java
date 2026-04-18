package com.rompix.rxnobots.verification;

import com.rompix.rxnobots.RxNoBotsPlugin;
import com.rompix.rxnobots.database.PlayerData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificationSession {
    private final Player player;
    private final RxNoBotsPlugin plugin;
    private String targetCode;
    private int attempts = 0;
    private final int maxAttempts;
    private final Random random = new Random();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final String verificationType;

    private VerificationStage currentStage = VerificationStage.CHAT;
    private boolean chatCompleted = false;
    private boolean movementCompleted = false;

    private final List<String> movementDirections;
    private int currentDirectionIndex = 0;
    private long currentDirectionStartTime = 0;
    private String currentDirection = "";
    private int currentDirectionDuration = 0;

    private long lastActionTime = System.currentTimeMillis();
    private final AtomicBoolean timeoutHandled = new AtomicBoolean(false);
    private ScheduledTask timeoutTask;

    // Флаг, указывающий, что сессия восстановлена (чтобы не генерировать заново код и направления)
    private final boolean restored;

    public enum VerificationStage {
        CHAT,
        MOVEMENT,
        COMPLETED
    }

    /**
     * Конструктор для новой сессии.
     */
    public VerificationSession(Player player, RxNoBotsPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        this.verificationType = plugin.getConfigManager().getVerificationType().toUpperCase();
        this.restored = false;

        // Генерация списка направлений
        if (plugin.getConfigManager().isMovementRandom()) {
            this.movementDirections = generateRandomMovementSequence();
        } else {
            this.movementDirections = new ArrayList<>(plugin.getConfigManager().getMovementDirections());
        }

        generateTargetCode();
        incrementSessionAttemptsInDB();

        startVerification();
        startTimeoutChecker();
        saveProgressToDatabase(); // Сохраняем начальное состояние
    }

    /**
     * Конструктор для восстановления сессии из сохранённых данных.
     */
    public VerificationSession(Player player, RxNoBotsPlugin plugin, PlayerData data) {
        this.player = player;
        this.plugin = plugin;
        this.maxAttempts = plugin.getConfigManager().getMaxAttempts();
        this.verificationType = plugin.getConfigManager().getVerificationType().toUpperCase();
        this.restored = true;

        // Восстанавливаем список направлений
        if (plugin.getConfigManager().isMovementRandom()) {
            // При восстановлении случайной последовательности генерируем новую и сбрасываем индекс
            this.movementDirections = generateRandomMovementSequence();
            data.setMovementDirectionIndex(0);
            data.setMovementDirectionStartTime(0);
        } else {
            this.movementDirections = new ArrayList<>(plugin.getConfigManager().getMovementDirections());
        }

        // Восстанавливаем состояние
        String stage = data.getVerificationStage();
        if (stage != null) {
            switch (stage) {
                case "CHAT" -> {
                    this.currentStage = VerificationStage.CHAT;
                    this.chatCompleted = false;
                }
                case "MOVEMENT" -> {
                    this.currentStage = VerificationStage.MOVEMENT;
                    this.chatCompleted = data.isChatVerified();
                }
                default -> {
                    this.currentStage = VerificationStage.CHAT;
                }
            }
        } else {
            this.currentStage = VerificationStage.CHAT;
        }

        this.chatCompleted = data.isChatVerified();
        this.currentDirectionIndex = data.getMovementDirectionIndex();
        this.currentDirectionStartTime = data.getMovementDirectionStartTime();

        // Если восстанавливаемся на этапе MOVEMENT, нужно установить текущее направление
        if (this.currentStage == VerificationStage.MOVEMENT && currentDirectionIndex < movementDirections.size()) {
            String directionData = movementDirections.get(currentDirectionIndex);
            String[] parts = directionData.split(":");
            this.currentDirection = parts[0].toLowerCase();
            this.currentDirectionDuration = Integer.parseInt(parts[1]);
        }

        // Генерируем код (новый для чата, если нужно)
        generateTargetCode();

        // Запускаем соответствующий этап
        if (this.currentStage == VerificationStage.CHAT) {
            startChatVerification();
        } else if (this.currentStage == VerificationStage.MOVEMENT) {
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.movement-stage"));
            // Сбрасываем таймер направления, чтобы игрок проходил этап честно с начала текущего направления
            long now = System.currentTimeMillis();
            currentDirectionStartTime = now;
            lastActionTime = now;
            String messageKey = "verification.movement-" + currentDirection;
            player.sendMessage(plugin.getLanguageManager().getMessage(messageKey));
        }

        startTimeoutChecker();
        saveProgressToDatabase();

        plugin.getLogger().info("Restored verification session for {} at stage {}",
                player.getUsername(), currentStage);
    }

    /**
     * Сохраняет текущий прогресс сессии в базу данных.
     */
    public void saveProgressToDatabase() {
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.setVerificationStage(currentStage.name());
                data.setChatVerified(chatCompleted);
                data.setMovementDirectionIndex(currentDirectionIndex);
                data.setMovementDirectionStartTime(currentDirectionStartTime);
                plugin.getDatabaseManager().updatePlayerData(data);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().debug("Saved verification progress for {}: stage={}, chatDone={}, dirIndex={}",
                            player.getUsername(), currentStage, chatCompleted, currentDirectionIndex);
                }
            }
        });
    }

    private void incrementSessionAttemptsInDB() {
        if (restored) return;
        plugin.getDatabaseManager().getPlayerData(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isPresent()) {
                PlayerData data = optData.get();
                data.incrementSessionAttempts();
                plugin.getDatabaseManager().updatePlayerData(data);
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().info("Incremented session attempts for {} to {}",
                        player.getUsername(), data.getSessionAttempts());
                }
            } else {
                plugin.getLogger().warn("No player data found for {} when incrementing session attempts", player.getUsername());
            }
        }).exceptionally(ex -> {
            plugin.getLogger().error("Failed to increment session attempts for " + player.getUsername(), ex);
            return null;
        });
    }

    private List<String> generateRandomMovementSequence() {
        List<String> available = plugin.getConfigManager().getMovementAvailableDirections();
        if (available.isEmpty()) {
            return List.of("up:2", "left:2");
        }

        int minCount = 3;
        int maxCount = 5;
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        count = Math.min(count, available.size());

        List<String> shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled);
        List<String> selected = shuffled.subList(0, count);

        int minDur = plugin.getConfigManager().getMovementMinDuration();
        int maxDur = plugin.getConfigManager().getMovementMaxDuration();
        List<String> result = new ArrayList<>();
        for (String dir : selected) {
            int duration = minDur + random.nextInt(maxDur - minDur + 1);
            result.add(dir + ":" + duration);
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Generated random movement sequence for {}: {}", player.getUsername(), result);
        }
        return result;
    }

    private void generateTargetCode() {
        String characters = plugin.getConfigManager().getCodeCharacters();
        int length = plugin.getConfigManager().getCodeLength();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(characters.charAt(random.nextInt(characters.length())));
        }
        this.targetCode = code.toString();
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Generated code for {}: {}", player.getUsername(), targetCode);
        }
    }

    private void startVerification() {
        switch (verificationType) {
            case "MOVEMENT_ONLY":
                startMovementVerification();
                break;
            case "CHAT_ONLY":
                startChatVerification();
                break;
            default:
                startChatVerification();
                break;
        }
    }

    private void startChatVerification() {
        currentStage = VerificationStage.CHAT;
        if (!restored) {
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome"));
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-stage"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-stage"));
        }

        Map<String, String> p = new HashMap<>();
        p.put("code", targetCode);
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-instruction", p));
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-hint"));

        saveProgressToDatabase();
    }

    private void startMovementVerification() {
        currentStage = VerificationStage.MOVEMENT;
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.movement-stage"));

        if (movementDirections.isEmpty()) {
            completeMovementVerification();
        } else {
            startNextDirection();
        }
        saveProgressToDatabase();
    }

    private void startNextDirection() {
        if (currentDirectionIndex >= movementDirections.size()) {
            completeMovementVerification();
            return;
        }

        String directionData = movementDirections.get(currentDirectionIndex);
        String[] parts = directionData.split(":");
        currentDirection = parts[0].toLowerCase();
        currentDirectionDuration = Integer.parseInt(parts[1]);
        currentDirectionStartTime = System.currentTimeMillis();
        lastActionTime = System.currentTimeMillis();

        String messageKey = "verification.movement-" + currentDirection;
        player.sendMessage(plugin.getLanguageManager().getMessage(messageKey));

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Direction {}/{}: {} for {}s",
                currentDirectionIndex + 1, movementDirections.size(), currentDirection, currentDirectionDuration);
        }
        saveProgressToDatabase();
    }

    private void completeMovementVerification() {
        movementCompleted = true;
        player.sendMessage(plugin.getLanguageManager().getMessage("verification.movement-success"));
        currentStage = VerificationStage.COMPLETED;
        saveProgressToDatabase();
        stopTimeoutChecker(); // Останавливаем таймер

        plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (completed.compareAndSet(false, true)) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("verification.complete"));
                    player.sendMessage(plugin.getLanguageManager().getMessage("verification.welcome-server"));
                    plugin.getVerificationManager().handleSuccess(player);
                }
            })
            .delay(java.time.Duration.ofMillis(500))
            .schedule();
    }

    private void startTimeoutChecker() {
        timeoutTask = plugin.getServer().getScheduler()
            .buildTask(plugin, () -> {
                if (currentStage == VerificationStage.COMPLETED || completed.get()) return;
                if (timeoutHandled.get()) return;

                long now = System.currentTimeMillis();
                int timeoutSeconds = plugin.getConfigManager().getResponseTimeout();

                if (now - lastActionTime > timeoutSeconds * 1000L) {
                    if (plugin.getConfigManager().isKickOnTimeout()) {
                        timeoutHandled.set(true);
                        stopTimeoutChecker(); // Останавливаем задачу
                        plugin.getLogger().info("Player {} timed out during verification", player.getUsername());
                        plugin.getVerificationManager().handleTimeout(player);
                    }
                }
            })
            .repeat(java.time.Duration.ofSeconds(1))
            .schedule();
    }

    private void stopTimeoutChecker() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    public void handleChatMessage(String message) {
        if (currentStage != VerificationStage.CHAT) return;
        if (completed.get()) return;

        lastActionTime = System.currentTimeMillis();

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Chat verification attempt by {}: {}", player.getUsername(), message);
        }

        String userInput = message.trim();
        String expected = targetCode;
        if (!plugin.getConfigManager().isCodeCaseSensitive()) {
            userInput = userInput.toUpperCase();
            expected = expected.toUpperCase();
        }

        if (userInput.equals(expected)) {
            chatCompleted = true;
            player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-success"));
            saveProgressToDatabase();

            if ("CHAT_ONLY".equals(verificationType)) {
                currentStage = VerificationStage.COMPLETED;
                stopTimeoutChecker(); // Останавливаем таймер
                if (completed.compareAndSet(false, true)) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("verification.complete"));
                    plugin.getVerificationManager().handleSuccess(player);
                }
            } else {
                startMovementVerification();
            }
        } else {
            attempts++;
            int remaining = maxAttempts - attempts;

            if (plugin.getConfigManager().isLogAttempts()) {
                plugin.getLogger().info("Failed verification attempt for {} (IP: {}) | Input: '{}' | Expected: '{}' | Attempts: {}/{}",
                    player.getUsername(), player.getRemoteAddress().getAddress().getHostAddress(),
                    message, targetCode, attempts, maxAttempts);
            }

            if (remaining > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("code", targetCode);
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-wrong", p));

                Map<String, String> p2 = new HashMap<>();
                p2.put("attempts", String.valueOf(remaining));
                player.sendMessage(plugin.getLanguageManager().getMessage("verification.chat-attempts", p2));
            } else {
                stopTimeoutChecker(); // Останавливаем таймер
                plugin.getVerificationManager().handleFail(player, 0);
            }
        }
    }

    public void handleMovement(double x, double y, double z, float yaw, float pitch) {
        if (currentStage != VerificationStage.MOVEMENT) return;
        if (completed.get()) return;
        if (currentDirectionIndex >= movementDirections.size()) return;

        lastActionTime = System.currentTimeMillis();

        // НЕ сохраняем углы в БД при каждом движении! Только при паузе или завершении.
        // Это исключает лавину запросов к БД.

        boolean isLookingCorrect = false;

        float normalizedYaw = yaw % 360;
        if (normalizedYaw < 0) normalizedYaw += 360;

        switch (currentDirection) {
            case "up":
                double upMin = plugin.getConfigManager().getDirectionAngle("up", "pitch-min");
                double upMax = plugin.getConfigManager().getDirectionAngle("up", "pitch-max");
                isLookingCorrect = pitch >= upMin && pitch <= upMax;
                break;

            case "down":
                double downMin = plugin.getConfigManager().getDirectionAngle("down", "pitch-min");
                double downMax = plugin.getConfigManager().getDirectionAngle("down", "pitch-max");
                isLookingCorrect = pitch >= downMin && pitch <= downMax;
                break;

            case "left":
                double leftMin = plugin.getConfigManager().getDirectionAngle("left", "yaw-min");
                double leftMax = plugin.getConfigManager().getDirectionAngle("left", "yaw-max");
                isLookingCorrect = normalizedYaw >= leftMin && normalizedYaw <= leftMax;
                break;

            case "right":
                double rightMin = plugin.getConfigManager().getDirectionAngle("right", "yaw-min");
                double rightMax = plugin.getConfigManager().getDirectionAngle("right", "yaw-max");
                if (rightMin < 0) {
                    double positiveMin = rightMin + 360;
                    isLookingCorrect = (normalizedYaw >= positiveMin && normalizedYaw < 360) ||
                                       (normalizedYaw >= 0 && normalizedYaw <= rightMax);
                } else {
                    isLookingCorrect = normalizedYaw >= rightMin && normalizedYaw <= rightMax;
                }
                break;
        }

        if (isLookingCorrect) {
            long holdTime = System.currentTimeMillis() - currentDirectionStartTime;
            if (holdTime >= currentDirectionDuration * 1000L) {
                currentDirectionIndex++;
                startNextDirection();
            } else {
                // Сохраняем только текущий прогресс (без углов) при необходимости, но не слишком часто
                // Можно оставить saveProgressToDatabase() здесь, но лучше делать это только при смене направления
                // или по таймеру. Однако для надёжности восстановления оставим, но частота вызова уже не критична,
                // так как мы убрали updatePlayerData внутри. saveProgressToDatabase() и так вызывает updatePlayerData.
                saveProgressToDatabase();
            }
        } else {
            currentDirectionStartTime = System.currentTimeMillis();
            saveProgressToDatabase();
        }
    }

    public void cancel() {
        if (completed.compareAndSet(false, true)) {
            stopTimeoutChecker();
            currentStage = VerificationStage.COMPLETED;
        }
    }

    public boolean isChatCompleted() { return chatCompleted; }
    public boolean isMovementCompleted() { return movementCompleted; }
    public VerificationStage getCurrentStage() { return currentStage; }
    public String getTargetCode() { return targetCode; }
}