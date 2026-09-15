package com.hybridac.listener;

import com.hybridac.player.session.CombatSessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MovementListener implements Listener {

    private final CombatSessionManager combatSessionManager;

    public MovementListener(CombatSessionManager combatSessionManager) {
        this.combatSessionManager = combatSessionManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getYaw() == event.getTo().getYaw() && event.getFrom().getPitch() == event.getTo().getPitch()) {
            return;
        }
        combatSessionManager.recordRotation(event.getPlayer(), event.getTo().getYaw(), event.getTo().getPitch());
    }
}
