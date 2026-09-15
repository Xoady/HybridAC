package com.hybridac.listener;

import com.hybridac.debug.AlertStreamService;
import com.hybridac.player.PlayerDataService;
import com.hybridac.recording.RecordingService;
import com.hybridac.violation.DirectStrikeService;
import com.hybridac.violation.PunishmentService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final PlayerDataService playerDataService;
    private final RecordingService recordingService;
    private final AlertStreamService alertStreamService;
    private final DirectStrikeService directStrikeService;
    private final PunishmentService punishmentService;

    public PlayerConnectionListener(
            PlayerDataService playerDataService,
            RecordingService recordingService,
            AlertStreamService alertStreamService,
            DirectStrikeService directStrikeService,
            PunishmentService punishmentService
    ) {
        this.playerDataService = playerDataService;
        this.recordingService = recordingService;
        this.alertStreamService = alertStreamService;
        this.directStrikeService = directStrikeService;
        this.punishmentService = punishmentService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataService.getOrCreate(event.getPlayer());
        alertStreamService.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        recordingService.stop(event.getPlayer().getUniqueId());
        alertStreamService.handlePlayerQuit(event.getPlayer());
        alertStreamService.remove(event.getPlayer().getUniqueId());
        directStrikeService.clear(event.getPlayer().getUniqueId());
        punishmentService.clear(event.getPlayer().getUniqueId());
        playerDataService.remove(event.getPlayer().getUniqueId());
    }
}
