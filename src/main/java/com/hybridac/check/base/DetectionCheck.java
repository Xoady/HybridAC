package com.hybridac.check.base;

import com.hybridac.config.CheckRuntimeConfig;

public interface DetectionCheck {

    String id();

    String description();

    boolean enabled();

    void configure(CheckRuntimeConfig config);

    CheckResult evaluate(CheckContext context);
}
