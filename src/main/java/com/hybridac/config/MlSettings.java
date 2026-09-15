package com.hybridac.config;

public record MlSettings(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        boolean enforcePlanModel
) {
    public MlSettings(boolean enabled, String baseUrl, String apiKey) {
        this(enabled, baseUrl, apiKey, "standard", true);
    }

    public MlSettings(boolean enabled, String baseUrl, String apiKey, String model) {
        this(enabled, baseUrl, apiKey, model, true);
    }
}
