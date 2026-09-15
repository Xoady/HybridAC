package com.hybridac.ml.dto;

import com.hybridac.model.HitSample;

import java.util.List;

public record DatasetUploadRequest(
        String sessionId,
        String playerUuid,
        String playerName,
        String label,
        String world,
        long createdAt,
        List<HitSample> hits,
        String model,
        String comment,
        String serverName
) {
    public DatasetUploadRequest(
            String sessionId,
            String playerUuid,
            String playerName,
            String label,
            String world,
            long createdAt,
            List<HitSample> hits,
            String model
    ) {
        this(sessionId, playerUuid, playerName, label, world, createdAt, hits, model, "Manual recording", "Minecraft Server");
    }

    public DatasetUploadRequest(
            String sessionId,
            String playerUuid,
            String playerName,
            String label,
            String world,
            long createdAt,
            List<HitSample> hits
    ) {
        this(sessionId, playerUuid, playerName, label, world, createdAt, hits, "standard", "Manual recording", "Minecraft Server");
    }
}
