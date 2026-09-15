package com.hybridac.config;

import java.util.List;

public record MenuSettings(
        String title,
        int size,
        double minProbability,
        String headMaterial,
        String headName,
        List<String> headLore,
        String borderMaterial,
        String borderName,
        List<Integer> borderSlots
) {
}
