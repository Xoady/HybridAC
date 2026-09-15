package com.hybridac.violation;

import com.hybridac.config.HybridConfig;
import com.hybridac.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PunishmentService {

    private static final long ALERT_COOLDOWN_MILLIS = 1_250L;

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final SanctionService sanctionService;
    private final Map<UUID, Long> lastAlerted = new ConcurrentHashMap<>();
    private volatile HybridConfig config;

    public PunishmentService(JavaPlugin plugin, MessageService messageService, SanctionService sanctionService, HybridConfig config) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.sanctionService = sanctionService;
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
        this.sanctionService.reconfigure(config);
    }

    public void clear(UUID playerUuid) {
        lastAlerted.remove(playerUuid);
        sanctionService.clear(playerUuid);
    }

    public void handle(Player player, PlayerSuspicionState state, HybridVerdict verdict) {
        if (verdict.verbose() || state.hasFreshMlVerdict(1_500L)) {
            sendAlert(player, state, verdict);
        }
        if (!state.hasFreshMlVerdict(2_500L)) {
            return;
        }
        double confidence = Math.min(1.0D, Math.max(0.0D, Math.max(verdict.rawScore(), verdict.smoothedScore())));
        sanctionService.registerPercent(
                player,
                "ml",
                confidence * 100.0D,
                "ml=" + percent(confidence) + "%"
        );
    }

    private void sendAlert(Player player, PlayerSuspicionState state, HybridVerdict verdict) {
        if (config.alerts().silentMode()) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastAlerted.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < ALERT_COOLDOWN_MILLIS) {
            return;
        }
        lastAlerted.put(player.getUniqueId(), now);

        String message = messageService.format("alerts.notify", Map.of(
                "player", player.getName(),
                "confidence", Integer.toString(percent(Math.max(verdict.rawScore(), verdict.smoothedScore()))),
                "ml", Integer.toString(percent(state.lastMlProbability())),
                "bot", Integer.toString(percent(state.botNormalized())),
                "reasons", String.join(",", verdict.reasons())
        ));
        Bukkit.getOnlinePlayers().stream()
                .filter(online -> online.hasPermission("hybridac.admin"))
                .forEach(online -> online.sendMessage(message));
    }

    private int percent(double value) {
        double normalized = Math.max(0.0D, Math.min(1.0D, value));
        return (int) Math.round(normalized * 100.0D);
    }
}
