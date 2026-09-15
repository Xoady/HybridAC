package com.hybridac.player;

import com.hybridac.model.HitSample;
import com.hybridac.player.session.CombatSession;
import com.hybridac.violation.PlayerSuspicionState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlayerData {

    private final UUID playerUuid;
    private final CombatSession combatSession;
    private final PlayerSuspicionState suspicionState;
    private final Map<String, Double> lastSignals = new ConcurrentHashMap<>();
    private final List<HitSample> accumulatedHits = new CopyOnWriteArrayList<>();
    private volatile boolean collecting = false;

    public PlayerData(UUID playerUuid, PlayerSuspicionState suspicionState) {
        this.playerUuid = playerUuid;
        this.suspicionState = suspicionState;
        this.combatSession = new CombatSession();
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public CombatSession combatSession() {
        return combatSession;
    }

    public PlayerSuspicionState suspicionState() {
        return suspicionState;
    }

    public Map<String, Double> lastSignals() {
        return lastSignals;
    }

    public List<HitSample> accumulatedHits() {
        return accumulatedHits;
    }

    public boolean collecting() {
        return collecting;
    }

    public void collecting(boolean collecting) {
        this.collecting = collecting;
    }
}
