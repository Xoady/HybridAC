package com.hybridac.buffer;

public final class DecayingBuffer {

    private final double decayPerSecond;
    private final double maxValue;
    private double value;
    private long lastUpdateMillis;

    public DecayingBuffer(double decayPerSecond, double maxValue) {
        this.decayPerSecond = decayPerSecond;
        this.maxValue = maxValue;
        this.lastUpdateMillis = System.currentTimeMillis();
    }

    public synchronized double add(double delta) {
        decay();
        value = Math.min(maxValue, value + delta);
        return value;
    }

    public synchronized double current() {
        decay();
        return value;
    }

    public synchronized void reset() {
        value = 0.0D;
        lastUpdateMillis = System.currentTimeMillis();
    }

    public synchronized double normalized() {
        decay();
        return maxValue == 0.0D ? 0.0D : value / maxValue;
    }

    private void decay() {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - lastUpdateMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        double elapsedSeconds = elapsedMillis / 1000.0D;
        value = Math.max(0.0D, value - (elapsedSeconds * decayPerSecond));
        lastUpdateMillis = now;
    }
}
