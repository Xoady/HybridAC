package com.hybridac.player;

import com.hybridac.player.session.CombatSession;
import com.hybridac.violation.PlayerSuspicionState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerData {

    private static final long SPRINT_PATTERN_WINDOW_MILLIS = 6_500L;

    private final UUID playerUuid;
    private final CombatSession combatSession;
    private final PlayerSuspicionState suspicionState;
    private final Map<String, Double> lastSignals = new ConcurrentHashMap<>();
    private final Deque<TimedSprintResetEvidence> recentSprintResetEvidence = new ArrayDeque<>();

    private volatile long lastSprintStopMillis;
    private volatile long lastSprintStartMillis;
    private volatile long pendingSprintResetAttackMillis;
    private volatile long pendingSprintResetStopGapMillis;
    private volatile double pendingSprintResetDistance;
    private volatile double pendingSprintResetCooldown;

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

    public long lastSprintStopMillis() {
        return lastSprintStopMillis;
    }

    public void recordSprintStop(long now) {
        this.lastSprintStopMillis = now;
        if (now - pendingSprintResetAttackMillis > 360L) {
            clearSprintResetCandidate();
        }
    }

    public void recordSprintStart(long now) {
        this.lastSprintStartMillis = now;
    }

    public long lastSprintStartMillis() {
        return lastSprintStartMillis;
    }

    public void armSprintResetCandidate(long now, long stopGapMillis, double distanceToTarget, double attackCooldown) {
        this.pendingSprintResetAttackMillis = now;
        this.pendingSprintResetStopGapMillis = stopGapMillis;
        this.pendingSprintResetDistance = distanceToTarget;
        this.pendingSprintResetCooldown = attackCooldown;
    }

    public void clearSprintResetCandidate() {
        this.pendingSprintResetAttackMillis = 0L;
        this.pendingSprintResetStopGapMillis = 0L;
        this.pendingSprintResetDistance = 0.0D;
        this.pendingSprintResetCooldown = 0.0D;
    }

    public SprintResetEvidence consumeSprintResetEvidence(long now, long minStartGapMillis, long maxStartGapMillis) {
        long pending = pendingSprintResetAttackMillis;
        long stopGap = pendingSprintResetStopGapMillis;
        double distanceToTarget = pendingSprintResetDistance;
        double attackCooldown = pendingSprintResetCooldown;
        clearSprintResetCandidate();
        if (pending <= 0L || now < pending) {
            return null;
        }
        long startGap = now - pending;
        if (startGap < minStartGapMillis || startGap > maxStartGapMillis) {
            return null;
        }
        return new SprintResetEvidence(stopGap, startGap, distanceToTarget, attackCooldown);
    }

    public synchronized SprintResetPattern recordSprintResetEvidence(SprintResetEvidence evidence, long now) {
        recentSprintResetEvidence.addLast(new TimedSprintResetEvidence(evidence, now));
        trimSprintResetEvidence(now);
        if (recentSprintResetEvidence.isEmpty()) {
            return new SprintResetPattern(0, Long.MAX_VALUE, Long.MAX_VALUE, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        long minStopGap = Long.MAX_VALUE;
        long maxStopGap = Long.MIN_VALUE;
        long minStartGap = Long.MAX_VALUE;
        long maxStartGap = Long.MIN_VALUE;
        double avgStopGap = 0.0D;
        double avgStartGap = 0.0D;
        double avgDistance = 0.0D;
        double avgCooldown = 0.0D;
        double avgCycle = 0.0D;

        for (TimedSprintResetEvidence sample : recentSprintResetEvidence) {
            minStopGap = Math.min(minStopGap, sample.evidence.stopGapMillis());
            maxStopGap = Math.max(maxStopGap, sample.evidence.stopGapMillis());
            minStartGap = Math.min(minStartGap, sample.evidence.startGapMillis());
            maxStartGap = Math.max(maxStartGap, sample.evidence.startGapMillis());
            avgStopGap += sample.evidence.stopGapMillis();
            avgStartGap += sample.evidence.startGapMillis();
            avgDistance += sample.evidence.distanceToTarget();
            avgCooldown += sample.evidence.attackCooldown();
            avgCycle += sample.evidence.cycleMillis();
        }

        int size = recentSprintResetEvidence.size();
        avgStopGap /= size;
        avgStartGap /= size;
        avgDistance /= size;
        avgCooldown /= size;
        avgCycle /= size;

        double stopDeviation = 0.0D;
        double startDeviation = 0.0D;
        for (TimedSprintResetEvidence sample : recentSprintResetEvidence) {
            stopDeviation += Math.abs(sample.evidence.stopGapMillis() - avgStopGap);
            startDeviation += Math.abs(sample.evidence.startGapMillis() - avgStartGap);
        }
        stopDeviation /= size;
        startDeviation /= size;

        return new SprintResetPattern(
                size,
                maxStopGap - minStopGap,
                maxStartGap - minStartGap,
                stopDeviation,
                startDeviation,
                avgDistance,
                avgCooldown,
                avgCycle
        );
    }

    private void trimSprintResetEvidence(long now) {
        while (!recentSprintResetEvidence.isEmpty() && now - recentSprintResetEvidence.peekFirst().timestampMillis > SPRINT_PATTERN_WINDOW_MILLIS) {
            recentSprintResetEvidence.removeFirst();
        }
    }

    public synchronized void resetSprintTracking() {
        lastSprintStopMillis = 0L;
        lastSprintStartMillis = 0L;
        recentSprintResetEvidence.clear();
        clearSprintResetCandidate();
    }

    public record SprintResetEvidence(long stopGapMillis, long startGapMillis, double distanceToTarget, double attackCooldown) {
        public long cycleMillis() {
            return stopGapMillis + startGapMillis;
        }
    }

    public record SprintResetPattern(
            int sampleCount,
            long stopGapSpreadMillis,
            long startGapSpreadMillis,
            double stopGapMeanDeviationMillis,
            double startGapMeanDeviationMillis,
            double averageDistance,
            double averageCooldown,
            double averageCycleMillis
    ) {
    }

    private record TimedSprintResetEvidence(SprintResetEvidence evidence, long timestampMillis) {
    }

    private final java.util.List<com.hybridac.model.HitSample> accumulatedHits = new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile boolean collecting = false;

    public java.util.List<com.hybridac.model.HitSample> accumulatedHits() {
        return accumulatedHits;
    }

    public boolean collecting() {
        return collecting;
    }

    public void collecting(boolean collecting) {
        this.collecting = collecting;
    }
}
