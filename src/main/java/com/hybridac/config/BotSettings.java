package com.hybridac.config;

public record BotSettings(
        boolean enabled,
        int maxActiveBots,
        long spawnIntervalTicks,
        long followUpdateTicks,
        long tabHideDelayTicks,
        int minCombatHitsBeforeSpawn,
        double minDistance,
        double maxDistance,
        double followDistance,
        double strafeAmplitude,
        double strafeSpeed,
        double followSmoothing,
        double verticalOffset,
        int inactivityTimeoutSeconds,
        int confirmationHitCount,
        long firstHitConfirmWindowTicks,
        double firstHitScore,
        double repeatHitScore,
        int swingMinIntervalTicks,
        int swingMaxIntervalTicks,
        boolean lineOfSightRequired,
        String nickPrefix
) {
}
