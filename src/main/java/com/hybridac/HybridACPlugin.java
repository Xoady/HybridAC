package com.hybridac;

import com.hybridac.bootstrap.BootstrapContext;
import com.hybridac.bootstrap.PluginBootstrap;
import com.hybridac.command.HybridACCommand;
import com.hybridac.config.HybridConfig;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class HybridACPlugin extends JavaPlugin {

    private BootstrapContext bootstrapContext;
    private HybridConfig runtimeConfig;
    private com.hybridac.storage.CredentialsStorage credentialsStorage;
    private String mlEndpoint;
    private String mlBearer;

    public String mlEndpoint() { return mlEndpoint; }
    public String mlBearer() { return mlBearer; }
    public com.hybridac.storage.CredentialsStorage credentialsStorage() { return credentialsStorage; }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.credentialsStorage = new com.hybridac.storage.CredentialsStorage(this);

        String apiKey = credentialsStorage.load()
                .map(r -> (r.apiKey() != null && !r.apiKey().isBlank()) ? r.apiKey() : r.mlBearer())
                .orElse(null);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = getConfig().getString("ml.api-key", "");
        }

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String baseUrl = getConfig().getString("ml.base-url", "https://hybridac.ru");
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(3))
                        .build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(baseUrl + "/api/v1/license/check"))
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(4))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"api_key\":\"" + apiKey + "\"}"))
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (response.statusCode() == 200 && body.contains("\"valid\":true")) {
                    java.util.regex.Matcher endpointMatcher = java.util.regex.Pattern.compile("\"ml_endpoint\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                    if (endpointMatcher.find()) {
                        this.mlEndpoint = endpointMatcher.group(1).replace("\\/", "/").replace("/infer", "");
                    }
                    java.util.regex.Matcher bearerMatcher = java.util.regex.Pattern.compile("\"ml_bearer\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                    if (bearerMatcher.find()) {
                        this.mlBearer = bearerMatcher.group(1).replace("\\/", "/");
                    }
                    getLogger().info("HybridAC license verified successfully!");
                } else {
                    getLogger().warning("License check returned status " + response.statusCode() + ". Running in fallback mode.");
                }
            } catch (Exception e) {
                getLogger().warning("Could not reach license server on startup (" + e.getMessage() + "). Running in cached mode.");
            }
        } else {
            getLogger().info("=================================================");
            getLogger().info("[HybridAC] Сервер ещё не привязан к проекту!");
            getLogger().info("[HybridAC] Чтобы активировать защиту, введите:");
            getLogger().info("[HybridAC]   /hybridac auth <пароль_с_сайта>");
            getLogger().info("=================================================");
        }

        try {
            org.bukkit.scoreboard.ScoreboardManager manager = org.bukkit.Bukkit.getScoreboardManager();
            if (manager != null) {
                org.bukkit.scoreboard.Scoreboard mainScoreboard = manager.getMainScoreboard();
                org.bukkit.scoreboard.Objective below = mainScoreboard.getObjective("hac_below");
                if (below != null) {
                    below.unregister();
                }
                org.bukkit.scoreboard.Objective list = mainScoreboard.getObjective("hac_list");
                if (list != null) {
                    list.unregister();
                }
            }

            this.bootstrapContext = new PluginBootstrap().bootstrap(this);
            this.runtimeConfig = bootstrapContext.config();

            PluginCommand command = getCommand("hybridac");
            if (command == null) {
                throw new IllegalStateException("hybridac command not found in plugin.yml");
            }
            HybridACCommand hybridACCommand = new HybridACCommand(bootstrapContext.commandContext());
            command.setExecutor(hybridACCommand);
            command.setTabCompleter(hybridACCommand);
            getLogger().info("HybridAC enabled");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to enable HybridAC", exception);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrapContext == null) {
            return;
        }
        if (bootstrapContext.monitorService() != null) {
            bootstrapContext.monitorService().shutdown();
        }
        bootstrapContext.recordingService().shutdown();
        closeQuietly(bootstrapContext.storageService());
        shutdownExecutor(bootstrapContext.ioExecutor());
        shutdownExecutor(bootstrapContext.mlExecutor());
    }

    public void reloadRuntime() {
        HybridConfig config = bootstrapContext.configLoader().load(this);

        if (credentialsStorage != null && credentialsStorage.hasCredentials()) {
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
        } else if (mlEndpoint != null && mlBearer != null) {
            com.hybridac.config.MlSettings originalMl = config.ml();
            config = config.withMl(new com.hybridac.config.MlSettings(
                    originalMl.enabled(),
                    mlEndpoint,
                    mlBearer,
                    originalMl.model(),
                    originalMl.enforcePlanModel()
            ));
        }

        this.runtimeConfig = config;
        bootstrapContext.messageService().reload(config.language());

        bootstrapContext.playerDataService().reconfigure(config);
        bootstrapContext.checkRegistry().reconfigure(config);
        bootstrapContext.attackListener().reconfigure(config);
        if (bootstrapContext.combatPacketListener() != null) {
            bootstrapContext.combatPacketListener().reconfigure(config);
        }
        bootstrapContext.alertStreamService().reconfigure(config);
        bootstrapContext.directStrikeService().reconfigure(config);
        bootstrapContext.punishmentService().reconfigure(config);
        bootstrapContext.violationManager().reconfigure(config);
        bootstrapContext.mlService().reconfigure(config);
        bootstrapContext.mlRequestExecutor().reconfigure(config.ml());

        getLogger().info("HybridAC runtime config reloaded");
    }

    public HybridConfig runtimeConfig() {
        return runtimeConfig;
    }

    public BootstrapContext bootstrapContext() {
        return bootstrapContext;
    }

    private void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception exception) {
            getLogger().warning("Failed to close resource: " + exception.getMessage());
        }
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
