package com.hybridac.bootstrap;

import com.hybridac.check.CheckRegistry;
import com.hybridac.command.CommandContext;
import com.hybridac.config.ConfigLoader;
import com.hybridac.config.HybridConfig;
import com.hybridac.debug.AlertStreamService;
import com.hybridac.listener.AttackListener;
import com.hybridac.listener.CombatPacketListener;
import com.hybridac.message.MessageService;
import com.hybridac.ml.MlService;
import com.hybridac.ml.client.MlRequestExecutor;
import com.hybridac.player.PlayerDataService;
import com.hybridac.recording.RecordingService;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import com.hybridac.violation.DirectStrikeService;
import com.hybridac.violation.PunishmentService;
import com.hybridac.violation.ViolationManager;

import com.hybridac.monitor.ActionbarMonitorService;
import com.hybridac.storage.CredentialsStorage;

import java.util.concurrent.ExecutorService;

public record BootstrapContext(
        ConfigLoader configLoader,
        HybridConfig config,
        StorageService storageService,
        CredentialsStorage credentialsStorage,
        ActionbarMonitorService monitorService,
        StatsService statsService,
        PlayerDataService playerDataService,
        RecordingService recordingService,
        CheckRegistry checkRegistry,
        AlertStreamService alertStreamService,
        MessageService messageService,
        DirectStrikeService directStrikeService,
        PunishmentService punishmentService,
        ViolationManager violationManager,
        AttackListener attackListener,
        CombatPacketListener combatPacketListener,
        MlService mlService,
        MlRequestExecutor mlRequestExecutor,
        ExecutorService ioExecutor,
        ExecutorService mlExecutor,
        CommandContext commandContext
) {
}
