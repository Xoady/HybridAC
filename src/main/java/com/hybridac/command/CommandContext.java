package com.hybridac.command;

import com.hybridac.HybridACPlugin;
import com.hybridac.check.CheckRegistry;
import com.hybridac.config.HybridConfig;
import com.hybridac.debug.AlertStreamService;
import com.hybridac.message.MessageService;
import com.hybridac.player.PlayerDataService;
import com.hybridac.recording.RecordingService;
import com.hybridac.stats.StatsService;
import com.hybridac.violation.ViolationManager;

public record CommandContext(
        HybridACPlugin plugin,
        PlayerDataService playerDataService,
        RecordingService recordingService,
        StatsService statsService,
        ViolationManager violationManager,
        CheckRegistry checkRegistry,
        AlertStreamService alertStreamService,
        MessageService messageService
) {
    public HybridConfig config() {
        return plugin.runtimeConfig();
    }
}
