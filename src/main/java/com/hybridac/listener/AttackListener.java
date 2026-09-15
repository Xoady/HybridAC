package com.hybridac.listener;

import com.hybridac.check.CheckRegistry;
import com.hybridac.check.base.CheckContext;
import com.hybridac.check.base.CheckResult;
import com.hybridac.config.HybridConfig;
import com.hybridac.ml.MlService;
import com.hybridac.model.HitSample;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.recording.RecordingService;
import com.hybridac.violation.DirectStrikeService;
import com.hybridac.violation.ViolationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

public final class AttackListener implements Listener {

    private final PlayerDataService playerDataService;
    private final RecordingService recordingService;
    private final CheckRegistry checkRegistry;
    private final ViolationManager violationManager;
    private final DirectStrikeService directStrikeService;
    private final MlService mlService;
    private final HitSampleFactory hitSampleFactory;
    private volatile HybridConfig config;

    public AttackListener(
            PlayerDataService playerDataService,
            RecordingService recordingService,
            CheckRegistry checkRegistry,
            ViolationManager violationManager,
            DirectStrikeService directStrikeService,
            MlService mlService,
            HitSampleFactory hitSampleFactory,
            HybridConfig config
    ) {
        this.playerDataService = playerDataService;
        this.recordingService = recordingService;
        this.checkRegistry = checkRegistry;
        this.violationManager = violationManager;
        this.directStrikeService = directStrikeService;
        this.mlService = mlService;
        this.hitSampleFactory = hitSampleFactory;
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!config.enabled() || !config.isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        PlayerData playerData = playerDataService.getOrCreate(player);
        boolean detectionExempt = recordingService.isDetectionExempt(player.getUniqueId());
        if (detectionExempt) {
            directStrikeService.clear(player.getUniqueId());
            playerData.lastSignals().clear();
        }

        HitSample sample = hitSampleFactory.create(player, target, playerData);
        playerData.combatSession().recordHit(sample);
        recordingService.recordHit(player.getUniqueId(), sample);
        if (detectionExempt) {
            return;
        }

        List<CheckResult> results = checkRegistry.evaluate(new CheckContext(player, target, sample, playerData));
        violationManager.process(player, playerData, results);
        directStrikeService.processChecks(player, results);

        mlService.handleHitForInference(player, playerData, sample);
    }
}
