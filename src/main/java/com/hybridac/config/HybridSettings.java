package com.hybridac.config;

public record HybridSettings(
        double mlWeight,
        double alertThreshold,
        double punishThreshold,
        double hysteresis,
        int minimumHitWindow,
        double smoothingFactor
) {
}
