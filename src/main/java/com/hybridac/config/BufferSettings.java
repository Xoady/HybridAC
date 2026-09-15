package com.hybridac.config;

public record BufferSettings(
        double defaultDecayPerSecond,
        double codeMax,
        double botMax,
        double hybridDecayPerSecond,
        int windowSize,
        int minimumEvidenceCount
) {
}
