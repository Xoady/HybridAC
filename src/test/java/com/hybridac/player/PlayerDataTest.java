package com.hybridac.player;

import com.hybridac.violation.PlayerSuspicionState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataTest {

    @Test
    void acceptsSprintResetEvidenceWithinServerWindow() {
        PlayerData playerData = new PlayerData(UUID.randomUUID(), new PlayerSuspicionState(1.0D, 1.0D, 1.0D, 1.0D));

        playerData.recordSprintStop(1_000L);
        playerData.armSprintResetCandidate(1_090L, 90L, 3.40D, 0.88D);

        PlayerData.SprintResetEvidence evidence = playerData.consumeSprintResetEvidence(1_190L, 0L, 165L);
        assertNotNull(evidence);
        assertEquals(90L, evidence.stopGapMillis());
        assertEquals(100L, evidence.startGapMillis());
    }

    @Test
    void buildsConsistentPatternAcrossMultipleCycles() {
        PlayerData playerData = new PlayerData(UUID.randomUUID(), new PlayerSuspicionState(1.0D, 1.0D, 1.0D, 1.0D));

        PlayerData.SprintResetPattern first = playerData.recordSprintResetEvidence(new PlayerData.SprintResetEvidence(82L, 104L, 3.2D, 0.90D), 2_000L);
        PlayerData.SprintResetPattern second = playerData.recordSprintResetEvidence(new PlayerData.SprintResetEvidence(88L, 97L, 3.3D, 0.91D), 2_800L);
        PlayerData.SprintResetPattern third = playerData.recordSprintResetEvidence(new PlayerData.SprintResetEvidence(79L, 109L, 3.1D, 0.89D), 3_500L);

        assertEquals(1, first.sampleCount());
        assertEquals(2, second.sampleCount());
        assertEquals(3, third.sampleCount());
        assertTrue(third.stopGapSpreadMillis() <= 9L);
        assertTrue(third.startGapSpreadMillis() <= 12L);
        assertTrue(third.stopGapMeanDeviationMillis() <= 4.0D);
        assertTrue(third.startGapMeanDeviationMillis() <= 5.0D);
        assertTrue(third.averageCycleMillis() <= 190.0D);
        assertTrue(third.averageCooldown() >= 0.89D);
    }
}
