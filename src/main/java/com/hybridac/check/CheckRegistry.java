package com.hybridac.check;

import com.hybridac.check.base.CheckContext;
import com.hybridac.check.base.CheckResult;
import com.hybridac.check.base.DetectionCheck;
import com.hybridac.config.HybridConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CheckRegistry {

    private final Map<String, DetectionCheck> checks = new LinkedHashMap<>();
    private final Map<String, com.hybridac.config.CheckRuntimeConfig> runtimeConfigs = new LinkedHashMap<>();

    public CheckRegistry(Collection<DetectionCheck> checks) {
        checks.forEach(check -> this.checks.put(check.id(), check));
    }

    public void reconfigure(HybridConfig config) {
        for (DetectionCheck check : checks.values()) {
            com.hybridac.config.CheckRuntimeConfig runtimeConfig = config.checks().getOrDefault(check.id(), new com.hybridac.config.CheckRuntimeConfig(true, 1.0D, 0.6D));
            runtimeConfigs.put(check.id(), runtimeConfig);
            check.configure(runtimeConfig);
        }
    }

    public List<CheckResult> evaluate(CheckContext context) {
        return checks.values().stream()
                .filter(DetectionCheck::enabled)
                .map(check -> check.evaluate(context))
                .filter(CheckResult::triggered)
                .toList();
    }

    public List<String> describeChecks() {
        return checks.values().stream()
                .map(check -> check.id() + "=" + (check.enabled() ? "enabled" : "disabled") + " (" + check.description() + ")")
                .toList();
    }

    public boolean toggle(String id) {
        DetectionCheck check = checks.get(id);
        if (check == null) {
            return false;
        }
        com.hybridac.config.CheckRuntimeConfig current = runtimeConfigs.getOrDefault(id, new com.hybridac.config.CheckRuntimeConfig(true, 1.0D, 0.6D));
        com.hybridac.config.CheckRuntimeConfig updated = new com.hybridac.config.CheckRuntimeConfig(!current.enabled(), current.weight(), current.triggerThreshold());
        runtimeConfigs.put(id, updated);
        check.configure(updated);
        return true;
    }

    public DetectionCheck get(String id) {
        return checks.get(id);
    }
}
