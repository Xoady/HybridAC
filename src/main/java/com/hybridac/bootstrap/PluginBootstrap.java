package com.hybridac.bootstrap;

import com.hybridac.HybridACPlugin;
import com.hybridac.check.CheckRegistry;
import com.hybridac.command.CommandContext;
import com.hybridac.config.ConfigLoader;
import com.hybridac.config.HybridConfig;
import com.hybridac.debug.AlertStreamService;
import com.hybridac.listener.AttackListener;
import com.hybridac.listener.CombatPacketListener;
import com.hybridac.listener.HitSampleFactory;
import com.hybridac.listener.MovementListener;
import com.hybridac.listener.PlayerConnectionListener;
import com.hybridac.listener.MenuListener;
import com.hybridac.listener.SanctionEffectListener;
import com.hybridac.listener.SprintToggleListener;
import com.hybridac.message.MessageService;
import com.hybridac.ml.MlService;
import com.hybridac.ml.client.MlClient;
import com.hybridac.ml.client.MlRequestExecutor;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.player.session.CombatSessionManager;
import com.hybridac.recording.RecordingService;
import com.hybridac.recording.export.JsonlDatasetWriter;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import com.hybridac.storage.sqlite.SQLiteStorageService;
import com.hybridac.violation.DirectStrikeService;
import com.hybridac.violation.PunishmentService;
import com.hybridac.violation.SanctionService;
import com.hybridac.violation.ViolationManager;
import org.bukkit.Bukkit;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PluginBootstrap {

    public BootstrapContext bootstrap(HybridACPlugin plugin) throws Exception {
        ConfigLoader configLoader = new ConfigLoader();
        HybridConfig config = configLoader.load(plugin);

        if (plugin.mlEndpoint() != null && plugin.mlBearer() != null) {
            com.hybridac.config.MlSettings originalMl = config.ml();
            config = config.withMl(new com.hybridac.config.MlSettings(
                    originalMl.enabled(),
                    plugin.mlEndpoint(),
                    plugin.mlBearer(),
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
        MlService mlService = new MlService(plugin.getLogger(), mlClient, storageService, statsService, config);

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
                plugin.getLogger(),
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

        plugin.getServer().getPluginManager().registerEvents(new MovementListener(combatSessionManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SanctionEffectListener(sanctionService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SprintToggleListener(playerDataService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerDataService, recordingService, alertStreamService, directStrikeService, punishmentService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(attackListener, plugin);

        CombatPacketListener combatPacketListener = null;
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            combatPacketListener = new CombatPacketListener(plugin, playerDataService, recordingService, config);
            com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(combatPacketListener);
        }

        com.hybridac.storage.CredentialsStorage credentialsStorage = plugin.credentialsStorage() != null ? plugin.credentialsStorage() : new com.hybridac.storage.CredentialsStorage(plugin);
        com.hybridac.monitor.ActionbarMonitorService monitorService = new com.hybridac.monitor.ActionbarMonitorService(plugin, playerDataService, messageService);

        if (credentialsStorage.hasCredentials()) {
            var creds = credentialsStorage.load().orElse(null);
            if (creds != null) {
                String dynamicKey = (creds.mlBearer() != null && !creds.mlBearer().isBlank()) ? creds.mlBearer() : creds.apiKey();
                if (dynamicKey != null && !dynamicKey.isBlank()) {
                    config = config.withMl(new com.hybridac.config.MlSettings(
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
            new com.hybridac.integration.HybridACExpansion(playerDataService, config).register();
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
}
