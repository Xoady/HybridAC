package com.hybridac.config;

import java.util.Locale;
import java.util.Map;

public record PunishmentSettings(
        boolean alertOnly,
        int minimumPunishIntervalSeconds,
        Map<String, DetectionPunishmentSettings> detections
) {
    public DetectionPunishmentSettings detection(String id) {
        if (id == null) {
            return DetectionPunishmentSettings.disabled();
        }
        return detections.getOrDefault(id.toLowerCase(Locale.ROOT), DetectionPunishmentSettings.disabled());
    }
}
