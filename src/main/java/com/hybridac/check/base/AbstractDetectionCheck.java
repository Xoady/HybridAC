package com.hybridac.check.base;

import com.hybridac.config.CheckRuntimeConfig;

public abstract class AbstractDetectionCheck implements DetectionCheck {

    private final String id;
    private final String description;
    private volatile CheckRuntimeConfig config;

    protected AbstractDetectionCheck(String id, String description) {
        this.id = id;
        this.description = description;
        this.config = new CheckRuntimeConfig(true, 1.0D, 0.6D);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean enabled() {
        return config.enabled();
    }

    @Override
    public void configure(CheckRuntimeConfig config) {
        this.config = config;
    }

    protected CheckRuntimeConfig config() {
        return config;
    }

    protected CheckResult result(double rawScore, String detail) {
        boolean triggered = enabled() && rawScore >= config.triggerThreshold();
        return new CheckResult(id, triggered, rawScore, rawScore * config.weight(), detail);
    }
}
