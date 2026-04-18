package com.rompix.rxnobots.database;

import com.rompix.rxnobots.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.Map;

public class DatabaseManager {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}
    }

    private final ConfigManager configManager;
    private final Logger logger;
    private final Path dataDirectory;

    private volatile HikariDataSource dataSource;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private volatile boolean initialized = false;

    public DatabaseManager(ConfigManager configManager, Logger logger, Path dataDirectory) {
        this.configManager = configManager;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        initDatabase();
        startCleanupTask();
    }

    private void startCleanupTask() {
        long cacheTTL = 3600000; // 1 hour
        int interval = Math.max(60, configManager.getCleanupInterval());
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            playerDataCache.entrySet().removeIf(entry -> {
                PlayerData data = entry.getValue();
                return now - data.getLastAccess() > cacheTTL;
            });
            if (configManager.isDebug()) {
                logger.debug("Cache cleaned, current size: {}", playerDataCache.size());
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    private void initDatabase() {
        try {
            if ("mysql".equalsIgnoreCase(configManager.getDatabaseType())) {
                initMySQL();
            } else {
                initSQLite();
            }
            createTables();
            migrateTable();
            initialized = true;
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            initialized = false;
        }
    }

    private void initMySQL() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + configManager.getMySQLHost() + ":" + configManager.getMySQLPort() + "/" + configManager.getMySQLDatabase() + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(configManager.getMySQLUsername());
        config.setPassword(configManager.getMySQLPassword());
        config.setMaximumPoolSize(configManager.getMySQLPoolSize());
        config.setConnectionInitSql("SET NAMES utf8mb4");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(10000);
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(30000);
        dataSource = new HikariDataSource(config);
        logger.info("MySQL initialized.");
    }

    private void initSQLite() {
        HikariConfig config = new HikariConfig();
        File file = dataDirectory.resolve(configManager.getSQLiteFile()).toFile();
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        String jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setDriverClassName("org.sqlite.JDBC");
        dataSource = new HikariDataSource(config);
        logger.info("SQLite initialized. Database file: {}", file.getAbsolutePath());
    }

    private Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_verification (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "username VARCHAR(16), " +
                "verification_status INT DEFAULT 0, " +
                "total_attempts INT DEFAULT 0, " +
                "failed_attempts INT DEFAULT 0, " +
                "timeout_until TIMESTAMP NULL, " +
                "bypass_granted BOOLEAN DEFAULT 0, " +
                "last_ip VARCHAR(45), " +
                "verified_until TIMESTAMP NULL, " +
                "verification_stage VARCHAR(20), " +
                "chat_verified BOOLEAN DEFAULT 0, " +
                "movement_direction_index INT DEFAULT 0, " +
                "movement_direction_start_time BIGINT DEFAULT 0, " +
                "last_movement_yaw REAL DEFAULT 0, " +
                "last_movement_pitch REAL DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON player_verification(uuid)"); } catch (SQLException ignored) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_ip ON player_verification(last_ip)"); } catch (SQLException ignored) {}
            try { stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON player_verification(username)"); } catch (SQLException ignored) {}
            logger.info("Database tables created/verified.");
        } catch (SQLException e) {
            logger.error("Table creation error", e);
        }
    }

    private void migrateTable() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String tableName = "player_verification";
            addColumnIfNotExists(conn, meta, tableName, "verification_stage", "VARCHAR(20)");
            addColumnIfNotExists(conn, meta, tableName, "chat_verified", "BOOLEAN DEFAULT 0");
            addColumnIfNotExists(conn, meta, tableName, "movement_direction_index", "INT DEFAULT 0");
            addColumnIfNotExists(conn, meta, tableName, "movement_direction_start_time", "BIGINT DEFAULT 0");
            addColumnIfNotExists(conn, meta, tableName, "last_movement_yaw", "REAL DEFAULT 0");
            addColumnIfNotExists(conn, meta, tableName, "last_movement_pitch", "REAL DEFAULT 0");
        } catch (SQLException e) {
            logger.error("Table migration error", e);
        }
    }

    private void addColumnIfNotExists(Connection conn, DatabaseMetaData meta, String table, String column, String type) throws SQLException {
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (!rs.next()) {
                String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    logger.info("Added column {} to table {}", column, table);
                }
            }
        }
    }

    // ================= CACHE =================

    public Optional<PlayerData> getCachedPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.updateLastAccess();
        }
        return Optional.ofNullable(data);
    }

    public void cachePlayerData(PlayerData data) {
        if (data != null) {
            data.updateLastAccess();
            playerDataCache.put(data.getUuid(), data);
        }
    }

    public void removeCachedPlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    // ================= ATOMIC OPERATIONS =================

    public CompletableFuture<Void> incrementTotalAttempts(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_verification SET total_attempts = total_attempts + 1, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    PlayerData cached = playerDataCache.get(uuid);
                    if (cached != null) {
                        cached.incrementTotalAttempts();
                        cached.updateLastAccess();
                    }
                    if (configManager.isDebug()) {
                        logger.debug("Incremented total_attempts for {}", uuid);
                    }
                }
            } catch (Exception e) {
                logger.error("Error incrementing total_attempts for {}", uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> incrementFailedAttempts(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_verification SET failed_attempts = failed_attempts + 1, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    PlayerData cached = playerDataCache.get(uuid);
                    if (cached != null) {
                        cached.incrementFailedAttempts();
                        cached.updateLastAccess();
                    }
                    if (configManager.isDebug()) {
                        logger.debug("Incremented failed_attempts for {}", uuid);
                    }
                }
            } catch (Exception e) {
                logger.error("Error incrementing failed_attempts for {}", uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> resetTotalAttempts(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_verification SET total_attempts = 0, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                PlayerData cached = playerDataCache.get(uuid);
                if (cached != null) {
                    cached.resetTotalAttempts();
                    cached.updateLastAccess();
                }
            } catch (Exception e) {
                logger.error("Error resetting total_attempts for {}", uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> resetFailedAttempts(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_verification SET failed_attempts = 0, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                PlayerData cached = playerDataCache.get(uuid);
                if (cached != null) {
                    cached.resetFailedAttempts();
                    cached.updateLastAccess();
                }
            } catch (Exception e) {
                logger.error("Error resetting failed_attempts for {}", uuid, e);
            }
        }, executor);
    }

    // ================= MAIN CRUD =================

    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid) {
        PlayerData cached = playerDataCache.get(uuid);
        if (cached != null) {
            cached.updateLastAccess();
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                logger.warn("Database not initialized, cannot load player data for {}", uuid);
                return Optional.empty();
            }
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM player_verification WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PlayerData data = map(rs);
                    data.updateLastAccess();
                    playerDataCache.put(uuid, data);
                    return Optional.of(data);
                }
            } catch (Exception e) {
                logger.error("getPlayerData error for uuid: {}", uuid, e);
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<Optional<PlayerData>> getPlayerDataByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                logger.warn("Database not initialized, cannot load player data for username {}", username);
                return Optional.empty();
            }
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM player_verification WHERE LOWER(username)=LOWER(?)")) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    PlayerData data = map(rs);
                    data.updateLastAccess();
                    playerDataCache.put(data.getUuid(), data);
                    return Optional.of(data);
                }
            } catch (Exception e) {
                logger.error("getByUsername error for username: {}", username, e);
            }
            return Optional.empty();
        }, executor);
    }

    public CompletableFuture<PlayerData> getOrCreatePlayerData(UUID uuid, String username) {
        return getPlayerData(uuid).thenCompose(opt -> {
            if (opt.isPresent()) return CompletableFuture.completedFuture(opt.get());
            return createPlayerData(uuid, username)
                    .thenCompose(v -> getPlayerData(uuid))
                    .thenApply(o -> o.orElseThrow(() -> new RuntimeException("Failed to create player data")));
        });
    }

    public CompletableFuture<Void> createPlayerData(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            if (!initialized) {
                logger.error("Database not initialized, cannot create player data");
                return;
            }
            String sql = "mysql".equalsIgnoreCase(configManager.getDatabaseType()) ?
                    "INSERT IGNORE INTO player_verification (uuid, username) VALUES (?, ?)" :
                    "INSERT OR IGNORE INTO player_verification (uuid, username) VALUES (?, ?)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, username);
                ps.executeUpdate();
                if (configManager.isDebug()) {
                    logger.debug("Created player data for {} ({})", username, uuid);
                }
            } catch (Exception e) {
                logger.error("createPlayerData error for {} ({})", username, uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> updatePlayerData(PlayerData data) {
        if (data == null) return CompletableFuture.completedFuture(null);
        data.updateLastAccess();
        playerDataCache.put(data.getUuid(), data);
        return CompletableFuture.runAsync(() -> {
            if (!initialized) {
                logger.warn("Database not initialized, skipping update for {}", data.getUuid());
                return;
            }
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE player_verification SET username=?, verification_status=?, total_attempts=?, failed_attempts=?, timeout_until=?, bypass_granted=?, last_ip=?, verified_until=?, " +
                         "verification_stage=?, chat_verified=?, movement_direction_index=?, movement_direction_start_time=?, last_movement_yaw=?, last_movement_pitch=?, updated_at=CURRENT_TIMESTAMP WHERE uuid=?")) {
                ps.setString(1, data.getUsername() != null ? data.getUsername() : "unknown");
                ps.setInt(2, data.isVerified() ? 1 : 0);
                ps.setInt(3, data.getTotalAttempts());
                ps.setInt(4, data.getFailedAttempts());
                ps.setTimestamp(5, data.getTimeoutUntil());
                ps.setBoolean(6, data.isBypassGranted());
                ps.setString(7, data.getLastIP());
                ps.setTimestamp(8, data.getVerifiedUntil());
                ps.setString(9, data.getVerificationStage());
                ps.setBoolean(10, data.isChatVerified());
                ps.setInt(11, data.getMovementDirectionIndex());
                ps.setLong(12, data.getMovementDirectionStartTime());
                ps.setFloat(13, data.getLastMovementYaw());
                ps.setFloat(14, data.getLastMovementPitch());
                ps.setString(15, data.getUuid().toString());
                ps.executeUpdate();
            } catch (Exception e) {
                logger.error("update error for {} ({})", data.getUsername(), data.getUuid(), e);
            }
        }, executor);
    }

    // ================= STATS =================

    public CompletableFuture<Integer> getTotalPlayers() {
        return queryInt("SELECT COUNT(*) FROM player_verification");
    }

    public CompletableFuture<Integer> getVerifiedPlayers() {
        return queryInt("SELECT COUNT(*) FROM player_verification WHERE verification_status=1");
    }

    public CompletableFuture<Integer> getTimedOutPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return 0;
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM player_verification WHERE timeout_until > ?")) {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception e) {
                logger.error("timeout stats error", e);
                return 0;
            }
        }, executor);
    }

    private CompletableFuture<Integer> queryInt(String sql) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return 0;
            try (Connection c = getConnection();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception e) {
                logger.error("query error: {}", sql, e);
                return 0;
            }
        }, executor);
    }

    private PlayerData map(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getInt("verification_status") == 1,
                rs.getInt("total_attempts"),
                rs.getInt("failed_attempts"),
                rs.getTimestamp("timeout_until"),
                rs.getBoolean("bypass_granted"),
                rs.getString("last_ip"),
                rs.getTimestamp("verified_until"),
                rs.getString("verification_stage"),
                rs.getBoolean("chat_verified"),
                rs.getInt("movement_direction_index"),
                rs.getLong("movement_direction_start_time"),
                rs.getFloat("last_movement_yaw"),
                rs.getFloat("last_movement_pitch")
        );
    }

    public void close() {
        logger.info("Closing DatabaseManager...");
        initialized = false;
        
        // Shutdown executors
        cleanupExecutor.shutdown();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close datasource
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        logger.info("DatabaseManager closed.");
    }
}