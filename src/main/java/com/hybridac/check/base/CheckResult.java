package com.hybridac.check.base;

public record CheckResult(
        String checkId,
        boolean triggered,
        double rawScore,
        double weightedScore,
        String detail
) {
}
