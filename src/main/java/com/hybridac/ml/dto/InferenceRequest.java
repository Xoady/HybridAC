package com.hybridac.ml.dto;

import com.hybridac.model.HitSample;

import java.util.List;

public record InferenceRequest(String playerUuid, int windowSize, List<HitSample> hits, String model) {
    public InferenceRequest(String playerUuid, int windowSize, List<HitSample> hits) {
        this(playerUuid, windowSize, hits, "standard");
    }
}
