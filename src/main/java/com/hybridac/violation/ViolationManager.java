package com.hybridac.violation;

import com.hybridac.check.base.CheckResult;
import com.hybridac.config.HybridConfig;
import com.hybridac.debug.AlertStreamService;
import com.hybridac.model.SuspicionSnapshot;
import com.hybridac.player.PlayerData;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import org.bukkit.entity.Player;

import java.util.List;

public final class ViolationManager {

    private final StorageService storageService;
    private final StatsService statsService;
    private final PunishmentService punishmentService;
    private final AlertStreamService alertStreamService;
    private volatile WeightedHybridEvaluator evaluator;
    private volatile HybridConfig config;

    public ViolationManager(StorageService storageService, StatsService statsService, PunishmentService punishmentService, AlertStreamService alertStreamService, HybridConfig config) {
        this.storageService = storageService;
        this.statsService = statsService;
        this.punishmentService = punishmentService;
        this.alertStreamService = alertStreamService;
        this.config = config;
        this.evaluator = new WeightedHybridEvaluator(config.buffers(), config.hybrid(), config.verboseThreshold());
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
        this.evaluator = new WeightedHybridEvaluator(config.buffers(), config.hybrid(), config.verboseThreshold());
        this.punishmentService.reconfigure(config);
    }

    public HybridVerdict process(Player player, PlayerData playerData, List<CheckResult> results) {
        for (CheckResult result : results) {
            playerData.lastSignals().put(result.checkId(), result.weightedScore());
            playerData.suspicionState().recordCodeSignal(result.checkId(), result.weightedScore());
        }

        HybridVerdict verdict = evaluator.evaluate(playerData.suspicionState(), playerData.combatSession().hitCount());
        storageService.saveSuspicionSummary(player.getUniqueId(), playerData.suspicionState().snapshot(verdict.rawScore()));
        alertStreamService.publish(player, playerData.suspicionState(), verdict, results);
        punishmentService.handle(player, playerData.suspicionState(), verdict);
        alertStreamService.updatePlayerSuffixForAll(player);
        return verdict;
    }

    public HybridVerdict processMlVerdict(Player player, PlayerData playerData, com.hybridac.model.MlInferenceVerdict verdict) {
        playerData.suspicionState().recordMlVerdict(verdict);
        playerData.lastSignals().put("ml", verdict.probabilityCheat());

        HybridVerdict hybridVerdict = evaluator.evaluate(playerData.suspicionState(), playerData.combatSession().hitCount());
        storageService.saveSuspicionSummary(player.getUniqueId(), playerData.suspicionState().snapshot(hybridVerdict.rawScore()));

        List<com.hybridac.check.base.CheckResult> results = List.of(
                new com.hybridac.check.base.CheckResult("ml", true, verdict.probabilityCheat(), verdict.probabilityCheat(), "ml_inference")
        );

        alertStreamService.publish(player, playerData.suspicionState(), hybridVerdict, results);
        punishmentService.handle(player, playerData.suspicionState(), hybridVerdict);
        alertStreamService.updatePlayerSuffixForAll(player);
        return hybridVerdict;
    }

    public SuspicionSnapshot snapshot(PlayerData playerData) {
        HybridVerdict verdict = evaluator.evaluate(playerData.suspicionState(), playerData.combatSession().hitCount());
        return playerData.suspicionState().snapshot(verdict.rawScore());
    }
}
