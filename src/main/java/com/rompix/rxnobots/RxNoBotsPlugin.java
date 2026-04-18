package com.rompix.rxnobots;

import com.google.inject.Inject;
import com.rompix.rxnobots.commands.AdminCommands;
import com.rompix.rxnobots.config.ConfigManager;
import com.rompix.rxnobots.config.LanguageManager;
import com.rompix.rxnobots.database.DatabaseManager;
import com.rompix.rxnobots.events.PlayerConnectionHandler;
import com.rompix.rxnobots.limbo.LimboManager;
import com.rompix.rxnobots.verification.VerificationManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "rxnobots",
    name = "RxNoBots",
    version = "1.0.0",
    authors = {"rompix"},
    description = "The best plugin for eliminating bots in Velocity using LimboAPI",
    dependencies = {
        @com.velocitypowered.api.plugin.Dependency(id = "limboapi", optional = false)
    }
)
public class RxNoBotsPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private VerificationManager verificationManager;
    private LimboManager limboManager;
    @SuppressWarnings("unused")
    private Metrics metrics;
    private final Metrics.Factory metricsFactory;

    private boolean initialized = false;

    @Inject
    public RxNoBotsPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (initialized) {
            logger.warn("Plugin already initialized, ignoring duplicate call");
            return;
        }
        logger.info("Loading RxNoBots...");

        // Config
        this.configManager = new ConfigManager(dataDirectory, logger);
        this.languageManager = new LanguageManager(dataDirectory, configManager.getLanguage(), logger);

        // Database
        this.databaseManager = new DatabaseManager(configManager, logger, dataDirectory);

        // Managers
        this.limboManager = new LimboManager(this);
        this.verificationManager = new VerificationManager(this);

        // Events
        server.getEventManager().register(this, new PlayerConnectionHandler(this));

        // Commands
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("rxnobots").aliases("rnb").build(),
                new AdminCommands(this)
        );

        // bStats metrics
        try {
            this.metrics = metricsFactory.make(this, 28400); // Оставь ID или замени на свой
            logger.info("bStats metrics initialized for RxNoBots (ID: 28400)");
        } catch (Exception e) {
            logger.warn("Could not initialize bStats metrics: " + e.getMessage());
        }

        initialized = true;
        logger.info("RxNoBots loaded successfully!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down RxNoBots...");
        if (verificationManager != null) {
            logger.info("Active verification sessions: {}", verificationManager.getSessionCount());
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        logger.info("RxNoBots shutdown complete.");
    }

    public void reload() {
        logger.info("Reloading RxNoBots...");
        if (configManager != null) {
            configManager.loadConfig();
        }
        if (languageManager != null) {
            languageManager.reload();
        } else {
            languageManager = new LanguageManager(dataDirectory, configManager.getLanguage(), logger);
        }
        logger.info("RxNoBots reloaded. Debug mode: {}", isDebug());
    }

    public boolean isDebug() {
        return configManager != null && configManager.isDebug();
    }

    // Getters
    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public VerificationManager getVerificationManager() { return verificationManager; }
    public LimboManager getLimboManager() { return limboManager; }
}