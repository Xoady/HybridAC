package com.hybridac.player.session;

import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.util.MathUtil;
import org.bukkit.entity.Player;

public final class CombatSessionManager {

    private final PlayerDataService playerDataService;

    public CombatSessionManager(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    public RotationFrame recordRotation(Player player, float yaw, float pitch) {
        PlayerData data = playerDataService.getOrCreate(player);
        RotationFrame previous = data.combatSession().latestRotation();
        double yawDelta = previous == null ? 0.0D : MathUtil.wrapAngleTo180(yaw - previous.yaw());
        double pitchDelta = previous == null ? 0.0D : pitch - previous.pitch();
        double acceleration = previous == null ? 0.0D : yawDelta - previous.yawDelta();
        double jerk = previous == null ? 0.0D : acceleration - previous.angularAcceleration();

        RotationFrame frame = new RotationFrame(
                System.currentTimeMillis(),
                yaw,
                pitch,
                yawDelta,
                pitchDelta,
                acceleration,
                jerk
        );
        data.combatSession().recordRotation(frame);
        return frame;
    }
}
