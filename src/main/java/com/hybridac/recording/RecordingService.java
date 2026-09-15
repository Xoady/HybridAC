package com.hybridac.recording;

import com.hybridac.message.MessageService;
import com.hybridac.ml.MlService;
import com.hybridac.model.HitSample;
import com.hybridac.model.RecordingLabel;
import com.hybridac.recording.export.DatasetExportService;
import com.hybridac.recording.export.ExportedRecording;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public final class RecordingService {

    private final Map<UUID, RecordingSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> detectionExemptPlayers = ConcurrentHashMap.newKeySet();
    private final DatasetExportService datasetExportService;
    private final StorageService storageService;
    private final StatsService statsService;
    private final MlService mlService;
    private final ExecutorService ioExecutor;
    private final Logger logger;
    private final MessageService messageService;

    public RecordingService(
            DatasetExportService datasetExportService,
            StorageService storageService,
            StatsService statsService,
            MlService mlService,
            ExecutorService ioExecutor,
            Logger logger,
            MessageService messageService
    ) {
        this.datasetExportService = datasetExportService;
        this.storageService = storageService;
        this.statsService = statsService;
        this.mlService = mlService;
        this.ioExecutor = ioExecutor;
        this.logger = logger;
        this.messageService = messageService;
    }

    public boolean start(Player player, RecordingLabel label, CommandSender initiatedBy) {
        return start(player, label, initiatedBy, "Manual recording");
    }

    public boolean start(Player player, RecordingLabel label, CommandSender initiatedBy, String comment) {
        UUID initiatedByUuid = initiatedBy instanceof Player commandPlayer ? commandPlayer.getUniqueId() : null;
        RecordingSession session = new RecordingSession(
                player.getUniqueId(),
                player.getName(),
                label,
                player.getWorld().getName(),
                initiatedByUuid,
                initiatedBy.getName(),
                comment
        );
        boolean started = sessions.putIfAbsent(player.getUniqueId(), session) == null;
        if (started) {
            detectionExemptPlayers.add(player.getUniqueId());
        }
        return started;
    }

    public Optional<RecordingSession> getSession(UUID playerUuid) {
        return Optional.ofNullable(sessions.get(playerUuid));
    }

    public boolean isDetectionExempt(UUID playerUuid) {
        return detectionExemptPlayers.contains(playerUuid);
    }

    public void recordHit(UUID playerUuid, HitSample sample) {
        RecordingSession session = sessions.get(playerUuid);
        if (session != null) {
            session.append(sample);
            notifyProgress(session);
        }
    }

    public Optional<RecordingSession> stop(UUID playerUuid) {
        detectionExemptPlayers.remove(playerUuid);
        RecordingSession session = sessions.remove(playerUuid);
        if (session == null) {
            return Optional.empty();
        }
        session.close();
        ioExecutor.submit(() -> persist(session));
        return Optional.of(session);
    }

    public void shutdown() {
        sessions.values().forEach(RecordingSession::close);
        sessions.clear();
        detectionExemptPlayers.clear();
    }

    private void persist(RecordingSession session) {
        try {
            ExportedRecording exportedRecording = datasetExportService.export(session);
            storageService.saveRecordingMetadata(session, exportedRecording.jsonlPath().toString());
            statsService.incrementRecordings();
            if (session.label() == RecordingLabel.LEGIT) {
                statsService.incrementLegitHits(session.hitCount());
            } else {
                statsService.incrementCheatHits(session.hitCount());
            }
            mlService.uploadDatasetAsync(session, exportedRecording.jsonlPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist recording " + session.sessionId(), exception);
        }
    }

    private void notifyProgress(RecordingSession session) {
        var placeholders = Map.of(
                "label", session.label().apiValue(),
                "player", session.playerName(),
                "hits", Integer.toString(session.hitCount()),
                "initiator", session.initiatedByName()
        );
        String plainMessage = "HybridAC record " + session.label().apiValue() + " for " + session.playerName() + ": " + session.hitCount() + " hits";
        if (session.initiatedByUuid() != null) {
            Player initiator = Bukkit.getPlayer(session.initiatedByUuid());
            if (initiator != null && initiator.isOnline()) {
                messageService.sendActionBar(initiator, "recording.progress", placeholders);
                return;
            }
        }
        logger.info(plainMessage + " [started by " + session.initiatedByName() + "]");
    }
}
