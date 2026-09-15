package com.hybridac.config;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public record DetectionPunishmentSettings(
        String mode,
        long sessionWindowSeconds,
        List<PunishmentThresholdSettings> thresholds
) {
    public DetectionPunishmentSettings {
        thresholds = thresholds.stream()
                .sorted(Comparator.comparingDouble(PunishmentThresholdSettings::threshold))
                .toList();
    }

    public static DetectionPunishmentSettings disabled() {
        return new DetectionPunishmentSettings("VL", 120L, List.of());
    }

    public boolean enabled() {
        return !thresholds.isEmpty();
    }

    public boolean percentMode() {
        return "PERCENT".equalsIgnoreCase(mode);
    }

    public int maxVlThreshold() {
        return thresholds.stream()
                .mapToInt(threshold -> (int) Math.round(threshold.threshold()))
                .max()
                .orElse(0);
    }

    public String normalizedMode() {
        return mode == null ? "VL" : mode.toUpperCase(Locale.ROOT);
    }
}
