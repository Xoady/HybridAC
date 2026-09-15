package com.hybridac.recording;

import com.hybridac.model.HitSample;
import com.hybridac.model.RecordingLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordingSession {

    private final UUID sessionId;
    private final UUID playerUuid;
    private final String playerName;
    private final RecordingLabel label;
    private final String world;
    private final UUID initiatedByUuid;
    private final String initiatedByName;
    private final long startedAt;
    private final List<HitSample> hits = new ArrayList<>();
    private final String comment;
    private volatile long endedAt;

    public RecordingSession(UUID playerUuid, String playerName, RecordingLabel label, String world, UUID initiatedByUuid, String initiatedByName) {
        this(playerUuid, playerName, label, world, initiatedByUuid, initiatedByName, "Manual recording");
    }

    public RecordingSession(UUID playerUuid, String playerName, RecordingLabel label, String world, UUID initiatedByUuid, String initiatedByName, String comment) {
        this.sessionId = UUID.randomUUID();
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.label = label;
        this.world = world;
        this.initiatedByUuid = initiatedByUuid;
        this.initiatedByName = initiatedByName;
        this.comment = comment != null && !comment.isBlank() ? comment : "Manual recording";
        this.startedAt = System.currentTimeMillis();
    }

    public synchronized void append(HitSample hitSample) {
        hits.add(hitSample);
    }

    public synchronized List<HitSample> hits() {
        return List.copyOf(hits);
    }

    public UUID sessionId() {
        return sessionId;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String playerName() {
        return playerName;
    }

    public RecordingLabel label() {
        return label;
    }

    public String world() {
        return world;
    }

    public long startedAt() {
        return startedAt;
    }

    public UUID initiatedByUuid() {
        return initiatedByUuid;
    }

    public String initiatedByName() {
        return initiatedByName;
    }

    public long endedAt() {
        return endedAt;
    }

    public void close() {
        this.endedAt = System.currentTimeMillis();
    }

    public synchronized int hitCount() {
        return hits.size();
    }

    public String comment() {
        return comment;
    }
}
