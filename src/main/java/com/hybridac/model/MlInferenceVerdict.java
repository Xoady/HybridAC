package com.hybridac.model;

import java.time.Instant;

public record MlInferenceVerdict(
        double probabilityCheat,
        String label,
        String modelVersion,
        String featureVersion,
        Instant inferredAt
) {
}
