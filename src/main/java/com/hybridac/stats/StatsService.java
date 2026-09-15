package com.hybridac.stats;

import com.hybridac.storage.StorageService;

import java.util.concurrent.atomic.AtomicLong;

public final class StatsService {

    private final StorageService storageService;
    private final AtomicLong totalLegitHits;
    private final AtomicLong totalCheatHits;
    private final AtomicLong totalRecordings;
    private final AtomicLong totalPunishments;
    private final AtomicLong totalCodeDetections;
    private final AtomicLong totalMlInferences;
    private final AtomicLong totalBotHitEvidences;
    private volatile String lastModelVersion;
    private volatile String lastTrainingTime;

    public StatsService(StorageService storageService) {
        this.storageService = storageService;
        StatsSnapshot snapshot = storageService.loadStatsSnapshot();
        this.totalLegitHits = new AtomicLong(snapshot.totalLegitHits());
        this.totalCheatHits = new AtomicLong(snapshot.totalCheatHits());
        this.totalRecordings = new AtomicLong(snapshot.totalRecordings());
        this.totalPunishments = new AtomicLong(snapshot.totalPunishments());
        this.totalCodeDetections = new AtomicLong(snapshot.totalCodeDetections());
        this.totalMlInferences = new AtomicLong(snapshot.totalMlInferences());
        this.totalBotHitEvidences = new AtomicLong(snapshot.totalBotHitEvidences());
        this.lastModelVersion = snapshot.lastModelVersion();
        this.lastTrainingTime = snapshot.lastTrainingTime();
    }

    public void incrementLegitHits(long delta) {
        totalLegitHits.addAndGet(delta);
        storageService.incrementCounter("total_legit_hits", delta);
    }

    public void incrementCheatHits(long delta) {
        totalCheatHits.addAndGet(delta);
        storageService.incrementCounter("total_cheat_hits", delta);
    }

    public void incrementRecordings() {
        totalRecordings.incrementAndGet();
        storageService.incrementCounter("total_recordings", 1L);
    }

    public void incrementPunishments() {
        totalPunishments.incrementAndGet();
        storageService.incrementCounter("total_punishments", 1L);
    }

    public void incrementCodeDetections() {
        totalCodeDetections.incrementAndGet();
        storageService.incrementCounter("total_code_detections", 1L);
    }

    public void incrementMlInferences() {
        totalMlInferences.incrementAndGet();
        storageService.incrementCounter("total_ml_inferences", 1L);
    }

    public void incrementBotHitEvidence() {
        totalBotHitEvidences.incrementAndGet();
        storageService.incrementCounter("total_bot_hit_evidences", 1L);
    }

    public void updateModelMetadata(String modelVersion, String trainingTime) {
        this.lastModelVersion = modelVersion;
        this.lastTrainingTime = trainingTime;
        storageService.saveModelMetadata(modelVersion, trainingTime);
    }

    public StatsSnapshot snapshot() {
        return new StatsSnapshot(
                totalLegitHits.get(),
                totalCheatHits.get(),
                totalRecordings.get(),
                totalPunishments.get(),
                totalCodeDetections.get(),
                totalMlInferences.get(),
                totalBotHitEvidences.get(),
                lastModelVersion,
                lastTrainingTime
        );
    }
}
