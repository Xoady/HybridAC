package com.hybridac.violation;

import com.hybridac.config.BufferSettings;
import com.hybridac.config.HybridSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedHybridEvaluatorTest {

    @Test
    void sustainedMlSignalReachesAlertThreshold() {
        PlayerSuspicionState state = new PlayerSuspicionState(0.1D, 12.0D, 8.0D, 0.1D);
        state.recordMlVerdict(new com.hybridac.model.MlInferenceVerdict(0.9D, "cheat", "model-1", "window-v1", java.time.Instant.now()));

        WeightedHybridEvaluator evaluator = new WeightedHybridEvaluator(
                new BufferSettings(0.12D, 12.0D, 8.0D, 0.08D, 40, 6),
                new HybridSettings(1.0D, 0.72D, 0.86D, 0.06D, 24, 1.0D),
                0.65D
        );

        HybridVerdict verdict = evaluator.evaluate(state, 40);
        assertTrue(verdict.alert());
        assertTrue(verdict.verbose());
    }

    @Test
    void highConfidenceMlSignalPunishes() {
        PlayerSuspicionState state = new PlayerSuspicionState(0.1D, 12.0D, 8.0D, 0.1D);
        state.recordMlVerdict(new com.hybridac.model.MlInferenceVerdict(0.95D, "cheat", "model-1", "window-v1", java.time.Instant.now()));

        WeightedHybridEvaluator evaluator = new WeightedHybridEvaluator(
                new BufferSettings(0.12D, 12.0D, 8.0D, 0.08D, 40, 6),
                new HybridSettings(1.0D, 0.72D, 0.86D, 0.06D, 24, 1.0D),
                0.65D
        );

        HybridVerdict verdict = evaluator.evaluate(state, 10);
        assertTrue(verdict.punish());
    }

    @Test
    void staleMlSignalDoesNotKeepAlertRed() {
        PlayerSuspicionState state = new PlayerSuspicionState(0.1D, 12.0D, 8.0D, 0.1D);
        state.recordMlVerdict(new com.hybridac.model.MlInferenceVerdict(
                0.95D,
                "cheat",
                "model-1",
                "window-v1",
                java.time.Instant.now().minusSeconds(10)
        ));

        WeightedHybridEvaluator evaluator = new WeightedHybridEvaluator(
                new BufferSettings(0.12D, 12.0D, 8.0D, 0.08D, 40, 6),
                new HybridSettings(1.0D, 0.72D, 0.86D, 0.06D, 24, 1.0D),
                0.65D
        );

        HybridVerdict verdict = evaluator.evaluate(state, 40);
        assertFalse(verdict.alert());
    }
}