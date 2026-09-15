package com.hybridac.listener;

import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

public final class SprintToggleListener implements Listener {

    private final PlayerDataService playerDataService;

    public SprintToggleListener(PlayerDataService playerDataService) {
        this.playerDataService = playerDataService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        PlayerData playerData = playerDataService.getOrCreate(event.getPlayer());
        long now = System.currentTimeMillis();
        if (event.isSprinting()) {
            playerData.recordSprintStart(now);
            return;
        }
        playerData.recordSprintStop(now);
    }
}
