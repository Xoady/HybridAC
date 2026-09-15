package com.hybridac.config;

import java.util.Map;
import java.util.Set;

public record HybridConfig(
        String language,
        boolean enabled,
        boolean debug,
        double verboseThreshold,
        Set<String> allowedWorlds,
        Set<String> blockedWorlds,
        AlertSettings alerts,
        Map<String, CheckRuntimeConfig> checks,
        BufferSettings buffers,
        HybridSettings hybrid,
        BotSettings bot,
        MlSettings ml,
        StorageSettings storage,
        PunishmentSettings punishment,
        MenuSettings menu
) {

    public boolean isWorldAllowed(String worldName) {
        if (blockedWorlds.contains(worldName.toLowerCase())) {
            return false;
        }
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName.toLowerCase());
    }

    public HybridConfig withMl(MlSettings newMl) {
        return new HybridConfig(
                language, enabled, debug, verboseThreshold, allowedWorlds, blockedWorlds,
                alerts, checks, buffers, hybrid, bot, newMl, storage, punishment, menu
        );
    }
}
