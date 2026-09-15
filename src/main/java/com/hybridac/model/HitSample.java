package com.hybridac.model;

import java.util.Map;
import java.util.UUID;

public record HitSample(
        UUID attackerUuid,
        UUID targetUuid,
        String targetType,
        String world,
        long timestamp,
        int ping,
        double cpsContext,
        double yaw,
        double pitch,
        double yawDelta,
        double pitchDelta,
        double previousYawDelta,
        double previousPitchDelta,
        double angularAcceleration,
        double angularJerk,
        double distanceToTarget,
        VectorSnapshot lookVector,
        VectorSnapshot targetVector,
        double aimError,
        HitboxPart hitboxPart,
        boolean sprinting,
        boolean onGround,
        VectorSnapshot velocity,
        double strafeInput,
        double forwardInput,
        boolean jumping,
        boolean falling,
        int targetHurtTime,
        double attackCooldown,
        boolean lineOfSight,
        double raytraceAlignment,
        boolean botNearby,
        boolean botHitMatched,
        Map<String, Double> activeSignals
) {
}
