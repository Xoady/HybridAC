package com.hybridac.debug;

import com.hybridac.check.base.CheckResult;
import com.hybridac.config.HybridConfig;
import com.hybridac.message.MessageService;
import com.hybridac.player.PlayerDataService;
import com.hybridac.violation.HybridVerdict;
import com.hybridac.violation.PlayerSuspicionState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class AlertStreamService {

    private static final long STREAM_COOLDOWN_MILLIS = 1_250L;

    private final Set<UUID> subscribers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastPublished = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final PlayerDataService playerDataService;
    private volatile HybridConfig config;

    public AlertStreamService(HybridConfig config, MessageService messageService, PlayerDataService playerDataService) {
        this.config = config;
        this.messageService = messageService;
        this.playerDataService = playerDataService;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
    }

    public boolean isSubscribed(UUID uuid) {
        return subscribers.contains(uuid);
    }

    public boolean toggle(Player moderator) {
        UUID uuid = moderator.getUniqueId();
        if (subscribers.contains(uuid)) {
            subscribers.remove(uuid);
            return false;
        } else {
            subscribers.add(uuid);
            return true;
        }
    }

    public void remove(UUID playerUuid) {
        subscribers.remove(playerUuid);
        lastPublished.remove(playerUuid);
    }

    public void publish(Player target, PlayerSuspicionState state, HybridVerdict verdict, List<CheckResult> checkResults) {
        if (subscribers.isEmpty() || !shouldPublish(state, verdict, checkResults)) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastPublished.getOrDefault(target.getUniqueId(), 0L);
        if (now - last < STREAM_COOLDOWN_MILLIS) {
            return;
        }
        lastPublished.put(target.getUniqueId(), now);

        String checks = checkResults.isEmpty()
                ? "-"
                : checkResults.stream().map(CheckResult::checkId).distinct().collect(Collectors.joining(","));
        String message = messageService.format("alerts.stream", Map.of(
                "player", target.getName(),
                "confidence", Integer.toString(percent(verdict.smoothedScore())),
                "probability", Integer.toString(percent(verdict.smoothedScore())),
                "checks", checks
        ));

        Bukkit.getOnlinePlayers().stream()
                .filter(player -> subscribers.contains(player.getUniqueId()))
                .filter(player -> player.hasPermission("hybridac.alerts") || player.hasPermission("hybridac.admin"))
                .forEach(player -> player.sendMessage(message));
    }

    private boolean shouldPublish(PlayerSuspicionState state, HybridVerdict verdict, List<CheckResult> checkResults) {
        return verdict.punish() || verdict.alert();
    }

    private int percent(double value) {
        double normalized = Math.max(0.0D, Math.min(1.0D, value));
        return (int) Math.round(normalized * 100.0D);
    }

    public void updatePlayerSuffixForAll(Player target) {
    }

    public void handlePlayerJoin(Player joined) {
        if (joined.hasPermission("hybridac.alerts")) {
            subscribers.add(joined.getUniqueId());
        }
    }

    public void handlePlayerQuit(Player quit) {
        remove(quit.getUniqueId());
    }
}
