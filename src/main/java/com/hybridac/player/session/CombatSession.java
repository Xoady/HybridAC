package com.hybridac.player.session;

import com.hybridac.model.HitSample;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class CombatSession {

    private static final int MAX_ROTATIONS = 80;
    private static final int MAX_HITS = 80;

    private final Deque<RotationFrame> rotations = new ArrayDeque<>();
    private final Deque<HitSample> hits = new ArrayDeque<>();
    private final Deque<Long> attackTimestamps = new ArrayDeque<>();

    public synchronized void recordRotation(RotationFrame frame) {
        rotations.addLast(frame);
        while (rotations.size() > MAX_ROTATIONS) {
            rotations.removeFirst();
        }
    }

    public synchronized void recordHit(HitSample sample) {
        hits.addLast(sample);
        attackTimestamps.addLast(sample.timestamp());

        while (hits.size() > MAX_HITS) {
            hits.removeFirst();
        }
        while (attackTimestamps.size() > MAX_HITS) {
            attackTimestamps.removeFirst();
        }
    }

    public synchronized void reset() {
        rotations.clear();
        hits.clear();
        attackTimestamps.clear();
    }

    public synchronized RotationFrame latestRotation() {
        return rotations.peekLast();
    }

    public synchronized RotationFrame previousRotation() {
        return rotations.size() < 2 ? null : rotations.stream().skip(rotations.size() - 2L).findFirst().orElse(null);
    }

    public synchronized List<RotationFrame> recentRotations() {
        return new ArrayList<>(rotations);
    }

    public synchronized List<RotationFrame> recentRotations(int windowSize) {
        List<RotationFrame> copy = new ArrayList<>(rotations);
        if (copy.size() <= windowSize) {
            return copy;
        }
        return copy.subList(copy.size() - windowSize, copy.size());
    }

    public synchronized List<HitSample> recentHits(int windowSize) {
        List<HitSample> copy = new ArrayList<>(hits);
        if (copy.size() <= windowSize) {
            return copy;
        }
        return copy.subList(copy.size() - windowSize, copy.size());
    }

    public synchronized int hitCount() {
        return hits.size();
    }

    public synchronized double cpsContext() {
        if (attackTimestamps.size() < 2) {
            return 0.0D;
        }

        long first = attackTimestamps.peekFirst();
        long last = attackTimestamps.peekLast();
        if (last <= first) {
            return 0.0D;
        }
        double seconds = (last - first) / 1000.0D;
        return seconds <= 0.0D ? 0.0D : attackTimestamps.size() / seconds;
    }

    public synchronized double estimatedCps() {
        return cpsContext();
    }
}
