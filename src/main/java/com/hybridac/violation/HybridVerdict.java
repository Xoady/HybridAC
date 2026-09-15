package com.hybridac.violation;

import java.util.List;

public record HybridVerdict(
        double rawScore,
        double smoothedScore,
        boolean alert,
        boolean punish,
        boolean verbose,
        List<String> reasons
) {
}
