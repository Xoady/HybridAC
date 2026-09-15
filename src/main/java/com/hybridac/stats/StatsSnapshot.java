package com.hybridac.stats;

public record StatsSnapshot(
        long totalLegitHits,
        long totalCheatHits,
        long totalRecordings,
        long totalPunishments,
        long totalCodeDetections,
        long totalMlInferences,
        long totalBotHitEvidences,
        String lastModelVersion,
        String lastTrainingTime
) {
}
