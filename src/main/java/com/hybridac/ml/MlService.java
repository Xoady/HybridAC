package com.hybridac.ml;

import com.hybridac.config.HybridConfig;
import com.hybridac.ml.client.MlClient;
import com.hybridac.ml.dto.DatasetUploadRequest;
import com.hybridac.ml.dto.InferenceRequest;
import com.hybridac.model.HitSample;
import com.hybridac.model.MlInferenceVerdict;
import com.hybridac.player.PlayerData;
import com.hybridac.recording.RecordingSession;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MlService {

    private final Logger logger;
    private final MlClient mlClient;
    private final StorageService storageService;
    private final StatsService statsService;
    private final MlWindowScheduler windowScheduler;
    private volatile HybridConfig config;

    public MlService(Logger logger, MlClient mlClient, StorageService storageService, StatsService statsService, HybridConfig config) {
        this.logger = logger;
        this.mlClient = mlClient;
        this.storageService = storageService;
        this.statsService = statsService;
        this.windowScheduler = new MlWindowScheduler(config);
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
        this.windowScheduler.reconfigure(config);
    }

    public void uploadDatasetAsync(RecordingSession session, Path filePath) {
        if (!config.ml().enabled() || session.hitCount() == 0) {
            return;
        }

        DatasetUploadRequest request = new DatasetUploadRequest(
                session.sessionId().toString(),
                session.playerUuid().toString(),
                session.playerName(),
                session.label().apiValue(),
                session.world(),
                session.startedAt(),
                session.hits(),
                config.ml().model(),
                session.comment(),
                org.bukkit.Bukkit.getServer().getName()
        );

        mlClient.uploadDataset(request).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                logger.log(Level.WARNING, "Failed to upload recording " + filePath + " (ML service unreachable: " + cause.toString() + ")");
                storageService.saveMlRequest(session.playerUuid(), "dataset_upload", session.hitCount(), 500, "upload_failed");
                return;
            }
            String modelVersion = result.body() == null ? "upload_only" : "upload_only";
            storageService.saveMlRequest(session.playerUuid(), "dataset_upload", session.hitCount(), result.statusCode(), modelVersion);
        });
    }

    public void handleHitForInference(Player player, PlayerData playerData, HitSample sample) {
        if (!playerData.collecting()) {
            playerData.collecting(true);
            playerData.accumulatedHits().clear();
            var plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.hybridac.HybridACPlugin.class);
            com.hybridac.util.TaskUtil.runEntityDelayed(plugin, player, () -> {
                if (player.isOnline()) {
                    List<HitSample> hitsToSend = new java.util.ArrayList<>(playerData.accumulatedHits());
                    playerData.accumulatedHits().clear();
                    playerData.collecting(false);
                    if (!hitsToSend.isEmpty()) {
                        submitWindowInference(player, playerData, hitsToSend);
                    }
                }
            }, 200L);
        }
        playerData.accumulatedHits().add(sample);
    }

    public void submitWindowInference(Player player, PlayerData playerData, List<HitSample> hits) {
        if (!config.ml().enabled()) {
            return;
        }

        InferenceRequest request = new InferenceRequest(player.getUniqueId().toString(), hits.size(), hits, config.ml().model());
        mlClient.infer(request).whenComplete((result, throwable) -> {
            if (throwable != null) {
                Throwable cause = throwable;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                logger.log(Level.WARNING, "Failed to infer window for " + player.getName() + " (ML service unreachable: " + cause.toString() + ")");
                storageService.saveMlRequest(player.getUniqueId(), "infer", hits.size(), 500, "infer_failed");
                return;
            }
            if (result.body() == null) {
                storageService.saveMlRequest(player.getUniqueId(), "infer", hits.size(), result.statusCode(), "empty_response");
                return;
            }

            String modelVer = (result.body().modelVersion() != null && !result.body().modelVersion().isBlank())
                    ? result.body().modelVersion()
                    : (config.ml().model() != null ? config.ml().model() : "standard");
            String featureVer = (result.body().featureVersion() != null && !result.body().featureVersion().isBlank())
                    ? result.body().featureVersion()
                    : "v1.0";

            MlInferenceVerdict verdict = new MlInferenceVerdict(
                    result.body().probabilityCheat(),
                    result.body().label(),
                    modelVer,
                    featureVer,
                    Instant.now()
            );
            statsService.incrementMlInferences();
            storageService.saveMlRequest(player.getUniqueId(), "infer", hits.size(), result.statusCode(), modelVer);

            var plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.hybridac.HybridACPlugin.class);
            com.hybridac.util.TaskUtil.runEntity(plugin, player, () -> {
                Player onlinePlayer = org.bukkit.Bukkit.getPlayer(player.getUniqueId());
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    plugin.bootstrapContext().violationManager().processMlVerdict(onlinePlayer, playerData, verdict);
                    if (plugin.bootstrapContext().monitorService() != null) {
                        plugin.bootstrapContext().monitorService().notifyInference(
                                onlinePlayer.getUniqueId(),
                                onlinePlayer.getName(),
                                verdict.probabilityCheat(),
                                playerData.combatSession().estimatedCps(),
                                verdict.modelVersion()
                        );
                    }
                }
            });
        });
    }
}
