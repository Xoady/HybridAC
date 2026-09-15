package com.hybridac.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.hybridac.config.HybridConfig;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.recording.RecordingService;
import com.hybridac.violation.DirectStrikeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CombatPacketListener extends PacketAdapter {

    private static final long MIN_START_GAP_MILLIS = 6L;
    private static final long MAX_START_GAP_MILLIS = 125L;
    private static final long MIN_STOP_GAP_MILLIS = 6L;
    private static final long MAX_STOP_GAP_MILLIS = 95L;
    private static final double MAX_SPRINT_RESET_DISTANCE = 3.85D;
    private static final double MIN_ATTACK_COOLDOWN = 0.84D;
    private static final int STRONG_PATTERN_MIN_SAMPLES = 2;
    private static final int REPEATED_PATTERN_MIN_SAMPLES = 3;
    private static final long MAX_STRONG_STOP_SPREAD_MILLIS = 18L;
    private static final long MAX_STRONG_START_SPREAD_MILLIS = 24L;
    private static final double MAX_STRONG_STOP_DEVIATION_MILLIS = 8.5D;
    private static final double MAX_STRONG_START_DEVIATION_MILLIS = 10.5D;
    private static final long MAX_REPEATED_STOP_SPREAD_MILLIS = 34L;
    private static final long MAX_REPEATED_START_SPREAD_MILLIS = 40L;
    private static final double MAX_REPEATED_STOP_DEVIATION_MILLIS = 15.0D;
    private static final double MAX_REPEATED_START_DEVIATION_MILLIS = 18.0D;
    private static final double MAX_REPEATED_CYCLE_MILLIS = 118.0D;

    private final PlayerDataService playerDataService;
    private final RecordingService recordingService;
    private volatile HybridConfig config;

    public CombatPacketListener(
            Plugin plugin,
            PlayerDataService playerDataService,
            RecordingService recordingService,
            HybridConfig config
    ) {
        super(plugin, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Client.ENTITY_ACTION);
        this.playerDataService = playerDataService;
        this.recordingService = recordingService;
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            handleEntityAction(event);
            return;
        }
        handleUseEntity(event);
    }

    private void handleUseEntity(PacketEvent event) {
        com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction wrappedAction = event.getPacket().getEnumEntityUseActions().readSafely(0);
        if (wrappedAction == null || wrappedAction.getAction() != EnumWrappers.EntityUseAction.ATTACK) {
            return;
        }
        int entityId = event.getPacket().getIntegers().read(0);
        Player player = event.getPlayer();
        com.hybridac.util.TaskUtil.runEntity(plugin, player, () -> handleAttackPacket(player, entityId));
    }

    private void handleEntityAction(PacketEvent event) {
        EnumWrappers.PlayerAction action = event.getPacket().getPlayerActions().readSafely(0);
        if (action != EnumWrappers.PlayerAction.START_SPRINTING && action != EnumWrappers.PlayerAction.STOP_SPRINTING) {
            return;
        }
        Player player = event.getPlayer();
        com.hybridac.util.TaskUtil.runEntity(plugin, player, () -> handleSprintAction(player, action));
    }

    private void handleAttackPacket(Player player, int entityId) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerData playerData = playerDataService.getOrCreate(player);
        if (!config.enabled() || !config.isWorldAllowed(player.getWorld().getName())) {
            playerData.clearSprintResetCandidate();
            return;
        }
        if (recordingService.isDetectionExempt(player.getUniqueId())) {
            playerData.clearSprintResetCandidate();
            return;
        }

        Player target = resolveTargetPlayer(player, entityId);
        if (target == null) {
            clearExpiredCandidate(playerData);
            return;
        }

        long now = System.currentTimeMillis();
        long sinceStop = now - playerData.lastSprintStopMillis();
        if (sinceStop < MIN_STOP_GAP_MILLIS || sinceStop > MAX_STOP_GAP_MILLIS) {
            clearExpiredCandidate(playerData);
            return;
        }

        double distance = player.getEyeLocation().distance(target.getEyeLocation());
        double attackCooldown = player.getAttackCooldown();
        if (distance > MAX_SPRINT_RESET_DISTANCE || attackCooldown < MIN_ATTACK_COOLDOWN) {
            clearExpiredCandidate(playerData);
            return;
        }
        playerData.armSprintResetCandidate(now, sinceStop, distance, attackCooldown);
    }

    private void handleSprintAction(Player player, EnumWrappers.PlayerAction action) {
        if (player == null || !player.isOnline()) {
            return;
        }
        PlayerData playerData = playerDataService.getOrCreate(player);
        long now = System.currentTimeMillis();
        if (action == EnumWrappers.PlayerAction.STOP_SPRINTING) {
            playerData.recordSprintStop(now);
            return;
        }

        playerData.recordSprintStart(now);
        if (!config.enabled() || !config.isWorldAllowed(player.getWorld().getName())) {
            playerData.clearSprintResetCandidate();
            return;
        }
        if (recordingService.isDetectionExempt(player.getUniqueId())) {
            playerData.clearSprintResetCandidate();
            return;
        }

        PlayerData.SprintResetEvidence evidence = playerData.consumeSprintResetEvidence(now, MIN_START_GAP_MILLIS, MAX_START_GAP_MILLIS);
        if (evidence == null) {
            return;
        }
        if (evidence.stopGapMillis() < MIN_STOP_GAP_MILLIS
                || evidence.stopGapMillis() > MAX_STOP_GAP_MILLIS
                || evidence.distanceToTarget() > MAX_SPRINT_RESET_DISTANCE
                || evidence.attackCooldown() < MIN_ATTACK_COOLDOWN
                || evidence.cycleMillis() > (MAX_STOP_GAP_MILLIS + MAX_START_GAP_MILLIS)) {
            return;
        }

        PlayerData.SprintResetPattern pattern = playerData.recordSprintResetEvidence(evidence, now);
        if (!isSprintResetPatternSuspicious(pattern)) {
            return;
        }

        playerData.lastSignals().put("sprint_reset", (double) pattern.sampleCount());
    }

    private boolean isSprintResetPatternSuspicious(PlayerData.SprintResetPattern pattern) {
        boolean strongTwoHitPattern = pattern.sampleCount() >= STRONG_PATTERN_MIN_SAMPLES
                && pattern.stopGapSpreadMillis() <= MAX_STRONG_STOP_SPREAD_MILLIS
                && pattern.startGapSpreadMillis() <= MAX_STRONG_START_SPREAD_MILLIS
                && pattern.stopGapMeanDeviationMillis() <= MAX_STRONG_STOP_DEVIATION_MILLIS
                && pattern.startGapMeanDeviationMillis() <= MAX_STRONG_START_DEVIATION_MILLIS
                && pattern.averageCycleMillis() <= 96.0D
                && pattern.averageCooldown() >= 0.88D
                && pattern.averageDistance() <= 3.55D;

        boolean repeatedMachinePattern = pattern.sampleCount() >= REPEATED_PATTERN_MIN_SAMPLES
                && pattern.stopGapSpreadMillis() <= MAX_REPEATED_STOP_SPREAD_MILLIS
                && pattern.startGapSpreadMillis() <= MAX_REPEATED_START_SPREAD_MILLIS
                && pattern.stopGapMeanDeviationMillis() <= MAX_REPEATED_STOP_DEVIATION_MILLIS
                && pattern.startGapMeanDeviationMillis() <= MAX_REPEATED_START_DEVIATION_MILLIS
                && pattern.averageCycleMillis() <= MAX_REPEATED_CYCLE_MILLIS
                && pattern.averageCooldown() >= 0.86D
                && pattern.averageDistance() <= 3.70D;

        return strongTwoHitPattern || repeatedMachinePattern;
    }

    private Player resolveTargetPlayer(Player attacker, int entityId) {
        return attacker.getWorld().getPlayers().stream()
                .filter(target -> target.getEntityId() == entityId)
                .findFirst()
                .orElse(null);
    }

    private void clearExpiredCandidate(PlayerData playerData) {
        long sinceStop = System.currentTimeMillis() - playerData.lastSprintStopMillis();
        if (sinceStop > MAX_STOP_GAP_MILLIS) {
            playerData.clearSprintResetCandidate();
        }
    }
}
