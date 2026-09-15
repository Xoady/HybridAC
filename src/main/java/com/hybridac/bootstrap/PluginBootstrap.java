package com.hybridac.bootstrap;

import com.comphenix.protocol.ProtocolLibrary;
import com.hybridac.ACPlugin;
import com.hybridac.check.CheckRegistry;
import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACCommand;
import com.hybridac.config.ConfigLoader;
import com.hybridac.config.HybridConfig;
import com.hybridac.config.MlSettings;
import com.hybridac.debug.AlertStreamService;
import com.hybridac.integration.HybridACExpansion;
import com.hybridac.listener.AttackListener;
import com.hybridac.listener.CombatPacketListener;
import com.hybridac.listener.HitSampleFactory;
import com.hybridac.listener.MenuListener;
import com.hybridac.listener.MovementListener;
import com.hybridac.listener.PlayerConnectionListener;
import com.hybridac.listener.SanctionEffectListener;
import com.hybridac.listener.SprintToggleListener;
import com.hybridac.message.MessageService;
import com.hybridac.ml.MlService;
import com.hybridac.ml.client.MlClient;
import com.hybridac.ml.client.MlRequestExecutor;
import com.hybridac.monitor.ActionbarMonitorService;
import com.hybridac.player.PlayerDataService;
import com.hybridac.player.session.CombatSessionManager;
import com.hybridac.recording.RecordingService;
import com.hybridac.recording.export.JsonlDatasetWriter;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.CredentialsStorage;
import com.hybridac.storage.StorageService;
import com.hybridac.storage.sqlite.SQLiteStorageService;
import com.hybridac.violation.DirectStrikeService;
import com.hybridac.violation.PunishmentService;
import com.hybridac.violation.SanctionService;
import com.hybridac.violation.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginBootstrap {

    private final ACPlugin plugin;
    private final Logger logger;
    private BootstrapContext context;
    private HybridConfig runtimeConfig;
    private CredentialsStorage credentialsStorage;
    private String mlEndpoint;
    private String mlBearer;

    public PluginBootstrap(ACPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public BootstrapContext enable() {
        plugin.saveDefaultConfig();
        this.credentialsStorage = new CredentialsStorage(plugin);

        String apiKey = credentialsStorage.load()
                .map(r -> (r.apiKey() != null && !r.apiKey().isBlank()) ? r.apiKey() : r.mlBearer())
                .orElse(null);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = plugin.getConfig().getString("ml.api-key", "");
        }

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            verifyLicense(apiKey);
        } else {
            logger.info("=================================================");
            logger.info("[HybridAC] Сервер ещё не привязан к проекту!");
            logger.info("[HybridAC] Чтобы активировать защиту, введите:");
            logger.info("[HybridAC]   /hybridac auth <пароль_с_сайта>");
            logger.info("=================================================");
        }

        try {
            resetScoreboardObjectives();
            this.context = initializeContext();
            this.runtimeConfig = context.config();
            registerCommand(context.commandContext());
            logger.info("HybridAC enabled");
            return this.context;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to enable HybridAC", exception);
        }
    }

    public void disable() {
        if (context == null) {
            return;
        }
        if (context.monitorService() != null) {
            context.monitorService().shutdown();
        }
        context.recordingService().shutdown();
        closeQuietly(context.storageService());
        shutdownExecutor(context.ioExecutor());
        shutdownExecutor(context.mlExecutor());
    }

    public void reload() {
        if (context == null) {
            return;
        }
        HybridConfig config = context.configLoader().load(plugin);

        if (credentialsStorage != null && credentialsStorage.hasCredentials()) {
            var creds = credentialsStorage.load().orElse(null);
            if (creds != null) {
                String dynamicKey = (creds.mlBearer() != null && !creds.mlBearer().isBlank()) ? creds.mlBearer() : creds.apiKey();
                if (dynamicKey != null && !dynamicKey.isBlank()) {
                    config = config.withMl(new MlSettings(
                            config.ml().enabled(),
                            config.ml().baseUrl(),
                            dynamicKey,
                            creds.model() != null && !creds.model().isBlank() ? creds.model() : config.ml().model(),
                            config.ml().enforcePlanModel()
                    ));
                }
            }
        } else if (mlEndpoint != null && mlBearer != null) {
            MlSettings originalMl = config.ml();
            config = config.withMl(new MlSettings(
                    originalMl.enabled(),
                    mlEndpoint,
                    mlBearer,
                    originalMl.model(),
                    originalMl.enforcePlanModel()
            ));
        }

        this.runtimeConfig = config;
        context.messageService().reload(config.language());

        context.playerDataService().reconfigure(config);
        context.checkRegistry().reconfigure(config);
        context.attackListener().reconfigure(config);
        if (context.combatPacketListener() != null) {
            context.combatPacketListener().reconfigure(config);
        }
        context.alertStreamService().reconfigure(config);
        context.directStrikeService().reconfigure(config);
        context.punishmentService().reconfigure(config);
        context.violationManager().reconfigure(config);
        context.mlService().reconfigure(config);
        context.mlRequestExecutor().reconfigure(config.ml());

        logger.info("HybridAC runtime config reloaded");
    }

    private BootstrapContext initializeContext() throws Exception {
        ConfigLoader configLoader = new ConfigLoader();
        HybridConfig config = configLoader.load(plugin);

        if (mlEndpoint != null && mlBearer != null) {
            MlSettings originalMl = config.ml();
            config = config.withMl(new MlSettings(
                    originalMl.enabled(),
                    mlEndpoint,
                    mlBearer,
                    originalMl.model(),
                    originalMl.enforcePlanModel()
            ));
        }

        MessageService messageService = new MessageService(plugin);
        messageService.load(config.language());

        StorageService storageService = new SQLiteStorageService(Path.of(config.storage().sqlitePath()));
        storageService.initialize();
        StatsService statsService = new StatsService(storageService);

        ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "HybridAC-IO"));
        ExecutorService mlExecutor = Executors.newFixedThreadPool(2, runnable -> new Thread(runnable, "HybridAC-ML"));

        PlayerDataService playerDataService = new PlayerDataService(config);
        MlRequestExecutor mlRequestExecutor = new MlRequestExecutor(config.ml(), mlExecutor);
        MlClient mlClient = new MlClient(mlRequestExecutor);
        MlService mlService = new MlService(logger, mlClient, storageService, statsService, config);

        JsonlDatasetWriter datasetWriter = new JsonlDatasetWriter(
                Path.of(config.storage().datasetDirectory()),
                Path.of(config.storage().sessionDirectory())
        );
        RecordingService recordingService = new RecordingService(
                datasetWriter,
                storageService,
                statsService,
                mlService,
                ioExecutor,
                logger,
                messageService
        );

        CheckRegistry checkRegistry = new CheckRegistry(List.of());
        checkRegistry.reconfigure(config);

        AlertStreamService alertStreamService = new AlertStreamService(config, messageService, playerDataService);
        SanctionService sanctionService = new SanctionService(plugin, storageService, statsService, config);
        DirectStrikeService directStrikeService = new DirectStrikeService(messageService, sanctionService, config);
        PunishmentService punishmentService = new PunishmentService(plugin, messageService, sanctionService, config);
        ViolationManager violationManager = new ViolationManager(storageService, statsService, punishmentService, alertStreamService, config);

        CombatSessionManager combatSessionManager = new CombatSessionManager(playerDataService);
        HitSampleFactory hitSampleFactory = new HitSampleFactory();
        AttackListener attackListener = new AttackListener(playerDataService, recordingService, checkRegistry, violationManager, directStrikeService, mlService, hitSampleFactory, config);

        var pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new MovementListener(combatSessionManager), plugin);
        pm.registerEvents(new SanctionEffectListener(sanctionService), plugin);
        pm.registerEvents(new SprintToggleListener(playerDataService), plugin);
        pm.registerEvents(new PlayerConnectionListener(playerDataService, recordingService, alertStreamService, directStrikeService, punishmentService), plugin);
        pm.registerEvents(new MenuListener(), plugin);
        pm.registerEvents(attackListener, plugin);

        CombatPacketListener combatPacketListener = null;
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            combatPacketListener = new CombatPacketListener(plugin, playerDataService, recordingService, config);
            ProtocolLibrary.getProtocolManager().addPacketListener(combatPacketListener);
        }

        ActionbarMonitorService monitorService = new ActionbarMonitorService(plugin, playerDataService, messageService);

        if (credentialsStorage.hasCredentials()) {
            var creds = credentialsStorage.load().orElse(null);
            if (creds != null) {
                String dynamicKey = (creds.mlBearer() != null && !creds.mlBearer().isBlank()) ? creds.mlBearer() : creds.apiKey();
                if (dynamicKey != null && !dynamicKey.isBlank()) {
                    config = config.withMl(new MlSettings(
                            config.ml().enabled(),
                            config.ml().baseUrl(),
                            dynamicKey,
                            creds.model() != null && !creds.model().isBlank() ? creds.model() : config.ml().model(),
                            config.ml().enforcePlanModel()
                    ));
                }
            }
        }

        CommandContext commandContext = new CommandContext(
                plugin,
                playerDataService,
                recordingService,
                statsService,
                violationManager,
                checkRegistry,
                alertStreamService,
                messageService
        );

        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new HybridACExpansion(playerDataService, config).register();
        }

        return new BootstrapContext(
                configLoader,
                config,
                storageService,
                credentialsStorage,
                monitorService,
                statsService,
                playerDataService,
                recordingService,
                checkRegistry,
                alertStreamService,
                messageService,
                directStrikeService,
                punishmentService,
                violationManager,
                attackListener,
                combatPacketListener,
                mlService,
                mlRequestExecutor,
                ioExecutor,
                mlExecutor,
                commandContext
        );
    }

    private void verifyLicense(String apiKey) {
        try {
            String baseUrl = plugin.getConfig().getString("ml.base-url", "https://hybridac.ru");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/license/check"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"api_key\":\"" + apiKey + "\"}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() == 200 && body.contains("\"valid\":true")) {
                Matcher endpointMatcher = Pattern.compile("\"ml_endpoint\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (endpointMatcher.find()) {
                    this.mlEndpoint = endpointMatcher.group(1).replace("\\/", "/").replace("/infer", "");
                }
                Matcher bearerMatcher = Pattern.compile("\"ml_bearer\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (bearerMatcher.find()) {
                    this.mlBearer = bearerMatcher.group(1).replace("\\/", "/");
                }
                logger.info("HybridAC license verified successfully!");
            } else {
                logger.warning("License check returned status " + response.statusCode() + ". Running in fallback mode.");
            }
        } catch (Exception e) {
            logger.warning("Could not reach license server on startup (" + e.getMessage() + "). Running in cached mode.");
        }
    }

    private void resetScoreboardObjectives() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard mainScoreboard = manager.getMainScoreboard();
            Objective below = mainScoreboard.getObjective("hac_below");
            if (below != null) {
                below.unregister();
            }
            Objective list = mainScoreboard.getObjective("hac_list");
            if (list != null) {
                list.unregister();
            }
        }
    }

    private void registerCommand(CommandContext commandContext) {
        PluginCommand command = plugin.getCommand("hybridac");
        if (command == null) {
            throw new IllegalStateException("hybridac command not found in plugin.yml");
        }
        HybridACCommand hybridACCommand = new HybridACCommand(commandContext);
        command.setExecutor(hybridACCommand);
        command.setTabCompleter(hybridACCommand);
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception exception) {
            logger.warning("Failed to close resource: " + exception.getMessage());
        }
    }

    private void shutdownExecutor(ExecutorService executorService) {
        if (executorService == null) return;
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    public BootstrapContext context() {
        return context;
    }

    public HybridConfig runtimeConfig() {
        return runtimeConfig;
    }

    public CredentialsStorage credentialsStorage() {
        return credentialsStorage;
    }

    public String mlEndpoint() {
        return mlEndpoint;
    }

    public String mlBearer() {
        return mlBearer;
    }
}
