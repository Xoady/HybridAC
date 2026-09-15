package com.hybridac.model;

import java.util.List;

public record SuspicionSnapshot(
        double codeScore,
        double mlScore,
        double botScore,
        double hybridScore,
        double smoothedConfidence,
        int evidenceCount,
        List<String> reasons
) {
}
