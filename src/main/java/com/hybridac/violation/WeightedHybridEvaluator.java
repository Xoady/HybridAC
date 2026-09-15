package com.hybridac.violation;

import com.hybridac.config.BufferSettings;
import com.hybridac.config.HybridSettings;

import java.util.ArrayList;
import java.util.List;

public final class WeightedHybridEvaluator {

    private final BufferSettings bufferSettings;
    private final HybridSettings hybridSettings;
    private final double verboseThreshold;

    public WeightedHybridEvaluator(BufferSettings bufferSettings, HybridSettings hybridSettings, double verboseThreshold) {
        this.bufferSettings = bufferSettings;
        this.hybridSettings = hybridSettings;
        this.verboseThreshold = verboseThreshold;
    }

    public HybridVerdict evaluate(PlayerSuspicionState state, int hitWindowSize) {
        double rawScore = state.lastMlProbability();
        double smoothed = state.updateSmoothedConfidence(rawScore, hybridSettings.smoothingFactor());
        double alertFloor = Math.max(0.0D, hybridSettings.alertThreshold() - hybridSettings.hysteresis());
        double punishFloor = Math.max(0.0D, hybridSettings.punishThreshold() - hybridSettings.hysteresis());

        List<String> reasons = new ArrayList<>(state.recentReasons());

        boolean alert = smoothed >= hybridSettings.alertThreshold() && rawScore >= alertFloor;
        boolean punish = smoothed >= hybridSettings.punishThreshold() && rawScore >= punishFloor;
        boolean verbose = smoothed >= verboseThreshold || rawScore >= alertFloor;

        return new HybridVerdict(rawScore, smoothed, alert, punish, verbose, reasons);
    }
}
