package com.hybridac.config;

import java.util.List;
import java.util.Locale;

public record PunishmentActionSettings(
        String type,
        double multiplier,
        long durationSeconds,
        List<String> commands
) {
    public String normalizedType() {
        return type == null ? "" : type.toUpperCase(Locale.ROOT);
    }
}
