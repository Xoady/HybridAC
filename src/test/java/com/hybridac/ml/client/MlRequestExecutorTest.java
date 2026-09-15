package com.hybridac.ml.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.hybridac.ml.dto.InferenceRequest;
import com.hybridac.ml.dto.InferenceResponse;
import com.hybridac.model.HitSample;
import com.hybridac.model.HitboxPart;
import com.hybridac.model.VectorSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MlRequestExecutorTest {

    @Test
    void serializesInferenceRequestUsingSnakeCase() throws Exception {
        InferenceRequest request = new InferenceRequest(
                UUID.randomUUID().toString(),
                40,
                List.of(sampleHit())
        );

        String json = MlRequestExecutor.createObjectMapper().writeValueAsString(request);
        JsonNode root = MlRequestExecutor.createObjectMapper().readTree(json);

        assertTrue(root.has("player_uuid"));
        assertTrue(root.has("window_size"));
        assertTrue(root.get("hits").get(0).has("yaw_delta"));
        assertTrue(root.get("hits").get(0).has("raytrace_alignment"));
    }

    @Test
    void deserializesSnakeCaseInferenceResponse() throws Exception {
        String json = """
                {
                  "probability_cheat": 0.91,
                  "label": "cheat",
                  "model_version": "model-1",
                  "feature_version": "window-v1"
                }
                """;

        InferenceResponse response = MlRequestExecutor.createObjectMapper().readValue(json, InferenceResponse.class);

        assertEquals(0.91D, response.probabilityCheat(), 1.0E-9D);
        assertEquals("cheat", response.label());
        assertEquals("model-1", response.modelVersion());
        assertEquals("window-v1", response.featureVersion());
    }

    private HitSample sampleHit() {
        return new HitSample(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PLAYER",
                "world",
                System.currentTimeMillis(),
                48,
                9.0D,
                10.0D,
                2.0D,
                1.5D,
                0.4D,
                1.1D,
                0.3D,
                0.2D,
                0.15D,
                2.8D,
                new VectorSnapshot(1.0D, 0.0D, 0.0D),
                new VectorSnapshot(0.8D, 0.1D, 0.1D),
                0.04D,
                HitboxPart.CHEST,
                true,
                true,
                new VectorSnapshot(0.0D, 0.0D, 0.0D),
                0.0D,
                1.0D,
                false,
                false,
                0,
                0.92D,
                true,
                0.97D,
                false,
                false,
                Map.of("aim_alignment", 0.45D)
        );
    }
}
