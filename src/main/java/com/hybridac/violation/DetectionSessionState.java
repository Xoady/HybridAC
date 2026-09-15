package com.hybridac.violation;

import java.util.HashSet;
import java.util.Set;

final class DetectionSessionState {

    private double progress;
    private long lastSignalMillis;
    private final Set<Double> appliedThresholds = new HashSet<>();

    synchronized double recordVl(long now, long windowMillis) {
        resetIfExpired(now, windowMillis);
        progress += 1.0D;
        lastSignalMillis = now;
        return progress;
    }

    synchronized double recordPercent(double percent, long now, long windowMillis) {
        resetIfExpired(now, windowMillis);
        progress = percent;
        lastSignalMillis = now;
        return progress;
    }

    synchronized boolean markThresholdTriggered(double threshold) {
        return appliedThresholds.add(threshold);
    }

    synchronized void clear() {
        progress = 0.0D;
        lastSignalMillis = 0L;
        appliedThresholds.clear();
    }

    private void resetIfExpired(long now, long windowMillis) {
        if (lastSignalMillis > 0L && windowMillis > 0L && now - lastSignalMillis > windowMillis) {
            clear();
        }
    }
}
