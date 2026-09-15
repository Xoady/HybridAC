package com.hybridac.config;

public record AlertSettings(
        String format,
        String streamFormat,
        double previewThreshold,
        boolean silentMode,
        int yellowThreshold,
        int redThreshold
) {
}
