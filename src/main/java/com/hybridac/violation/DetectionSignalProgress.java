package com.hybridac.violation;

public record DetectionSignalProgress(double progress, double maxThreshold, boolean thresholdTriggered) {
    public int progressInt() {
        return (int) Math.round(progress);
    }

    public int maxThresholdInt() {
        return (int) Math.round(maxThreshold);
    }
}
