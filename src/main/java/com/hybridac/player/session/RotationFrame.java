package com.hybridac.player.session;

public record RotationFrame(
        long timestamp,
        double yaw,
        double pitch,
        double yawDelta,
        double pitchDelta,
        double angularAcceleration,
        double angularJerk
) {
}
