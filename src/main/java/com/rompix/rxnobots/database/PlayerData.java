package com.rompix.rxnobots.database;

import java.sql.Timestamp;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String username;
    private boolean verified;
    private int totalAttempts;      // общее количество попыток сессии
    private int failedAttempts;     // количество неудачных попыток ввода кода
    private Timestamp timeoutUntil; // таймаут после превышения попыток
    private boolean bypassGranted;  // флаг байпаса
    
    // IP-based tracking
    private String lastIP;
    private Timestamp verifiedUntil; // когда истекает кулдаун (cooldown)

    // Новое поле – время последнего доступа к данным (для очистки кэша)
    private long lastAccess;

    // ====== НОВЫЕ ПОЛЯ ДЛЯ СОХРАНЕНИЯ ПРОГРЕССА ВЕРИФИКАЦИИ ======
    private String verificationStage;        // "CHAT", "MOVEMENT", "COMPLETED", null
    private boolean chatVerified;            // пройден ли этап чата
    private int movementDirectionIndex;      // индекс текущего направления (0-based)
    private long movementDirectionStartTime; // время начала текущего направления в миллисекундах
    private float lastMovementYaw;           // последний yaw для восстановления
    private float lastMovementPitch;         // последний pitch для восстановления

    // Основной конструктор (все поля) – расширен новыми параметрами
    public PlayerData(UUID uuid, String username, boolean verified, int totalAttempts, int failedAttempts,
                     Timestamp timeoutUntil, boolean bypassGranted, String lastIP, Timestamp verifiedUntil,
                     String verificationStage, boolean chatVerified, int movementDirectionIndex,
                     long movementDirectionStartTime, float lastMovementYaw, float lastMovementPitch) {
        this.uuid = uuid;
        this.username = username;
        this.verified = verified;
        this.totalAttempts = totalAttempts;
        this.failedAttempts = failedAttempts;
        this.timeoutUntil = timeoutUntil;
        this.bypassGranted = bypassGranted;
        this.lastIP = lastIP;
        this.verifiedUntil = verifiedUntil;
        this.lastAccess = System.currentTimeMillis();
        this.verificationStage = verificationStage;
        this.chatVerified = chatVerified;
        this.movementDirectionIndex = movementDirectionIndex;
        this.movementDirectionStartTime = movementDirectionStartTime;
        this.lastMovementYaw = lastMovementYaw;
        this.lastMovementPitch = lastMovementPitch;
    }
    
    // Конструктор для обратной совместимости (без IP, verifiedUntil и новых полей)
    public PlayerData(UUID uuid, String username, boolean verified, int totalAttempts, int failedAttempts, 
                     Timestamp timeoutUntil, boolean bypassGranted) {
        this(uuid, username, verified, totalAttempts, failedAttempts, timeoutUntil, bypassGranted, 
             null, null, null, false, 0, 0L, 0.0f, 0.0f);
    }
    
    // Минимальный конструктор для новых игроков
    public PlayerData(UUID uuid, String username) {
        this(uuid, username, false, 0, 0, null, false, null, null, 
             null, false, 0, 0L, 0.0f, 0.0f);
    }

    // Геттеры и сеттеры
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public int getTotalAttempts() { return totalAttempts; }
    public void incrementTotalAttempts() { this.totalAttempts++; }
    public void resetTotalAttempts() { this.totalAttempts = 0; }
    
    public int getSessionAttempts() { return totalAttempts; }
    public void incrementSessionAttempts() { incrementTotalAttempts(); }
    
    public int getFailedAttempts() { return failedAttempts; }
    public void incrementFailedAttempts() { this.failedAttempts++; }
    public void resetFailedAttempts() { this.failedAttempts = 0; }
    
    public Timestamp getTimeoutUntil() { return timeoutUntil; }
    public void setTimeoutUntil(Timestamp timeoutUntil) { this.timeoutUntil = timeoutUntil; }
    public boolean isTimedOut() {
        return timeoutUntil != null && timeoutUntil.after(new Timestamp(System.currentTimeMillis()));
    }
    public boolean hasTimeout() { return isTimedOut(); }
    
    public boolean isBypassGranted() { return bypassGranted; }
    public void setBypassGranted(boolean bypassGranted) { this.bypassGranted = bypassGranted; }
    
    public String getLastIP() { return lastIP; }
    public void setLastIP(String lastIP) { this.lastIP = lastIP; }
    
    public Timestamp getVerifiedUntil() { return verifiedUntil; }
    public void setVerifiedUntil(Timestamp verifiedUntil) { this.verifiedUntil = verifiedUntil; }
    public boolean isInCooldown() {
        return verifiedUntil != null && verifiedUntil.after(new Timestamp(System.currentTimeMillis()));
    }
    public boolean isOnCooldown() { return isInCooldown(); }
    
    public long getLastAccess() { return lastAccess; }
    public void setLastAccess(long lastAccess) { this.lastAccess = lastAccess; }
    public void updateLastAccess() { this.lastAccess = System.currentTimeMillis(); }
    
    // ====== НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ======
    public String getVerificationStage() { return verificationStage; }
    public void setVerificationStage(String verificationStage) { this.verificationStage = verificationStage; }
    
    public boolean isChatVerified() { return chatVerified; }
    public void setChatVerified(boolean chatVerified) { this.chatVerified = chatVerified; }
    
    public int getMovementDirectionIndex() { return movementDirectionIndex; }
    public void setMovementDirectionIndex(int movementDirectionIndex) { this.movementDirectionIndex = movementDirectionIndex; }
    
    public long getMovementDirectionStartTime() { return movementDirectionStartTime; }
    public void setMovementDirectionStartTime(long movementDirectionStartTime) { this.movementDirectionStartTime = movementDirectionStartTime; }
    
    public float getLastMovementYaw() { return lastMovementYaw; }
    public void setLastMovementYaw(float lastMovementYaw) { this.lastMovementYaw = lastMovementYaw; }
    
    public float getLastMovementPitch() { return lastMovementPitch; }
    public void setLastMovementPitch(float lastMovementPitch) { this.lastMovementPitch = lastMovementPitch; }
    
    // ====== УДОБНЫЕ МЕТОДЫ ДЛЯ СБРОСА ПРОГРЕССА ======
    /**
     * Сбрасывает только прогресс верификации (этап, флаги, индекс движения)
     */
    public void resetVerificationProgress() {
        this.verificationStage = null;
        this.chatVerified = false;
        this.movementDirectionIndex = 0;
        this.movementDirectionStartTime = 0L;
        this.lastMovementYaw = 0.0f;
        this.lastMovementPitch = 0.0f;
    }
    
    /**
     * Полный сброс данных (кроме UUID и username)
     */
    public void reset() {
        this.verified = false;
        this.totalAttempts = 0;
        this.failedAttempts = 0;
        this.timeoutUntil = null;
        this.bypassGranted = false;
        this.lastIP = null;
        this.verifiedUntil = null;
        resetVerificationProgress();
    }
    
    public boolean updateUsernameIfNeeded(String newUsername) {
        if (newUsername != null && !newUsername.equals(this.username)) {
            this.username = newUsername;
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                ", verified=" + verified +
                ", totalAttempts=" + totalAttempts +
                ", failedAttempts=" + failedAttempts +
                ", timeoutUntil=" + timeoutUntil +
                ", bypassGranted=" + bypassGranted +
                ", lastIP='" + lastIP + '\'' +
                ", verifiedUntil=" + verifiedUntil +
                ", lastAccess=" + lastAccess +
                ", verificationStage='" + verificationStage + '\'' +
                ", chatVerified=" + chatVerified +
                ", movementDirectionIndex=" + movementDirectionIndex +
                ", movementDirectionStartTime=" + movementDirectionStartTime +
                ", lastMovementYaw=" + lastMovementYaw +
                ", lastMovementPitch=" + lastMovementPitch +
                '}';
    }
}