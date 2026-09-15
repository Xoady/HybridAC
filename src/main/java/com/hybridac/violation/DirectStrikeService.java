package com.hybridac.violation;

import com.hybridac.check.base.CheckResult;
import com.hybridac.config.DetectionPunishmentSettings;
import com.hybridac.config.HybridConfig;
import com.hybridac.message.MessageService;
import com.hybridac.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DirectStrikeService {

    private static final long ALERT_COOLDOWN_MILLIS = 900L;

    private final MessageService messageService;
    private final SanctionService sanctionService;
    private final Map<UUID, Long> lastAlertMillis = new ConcurrentHashMap<>();
    private volatile HybridConfig config;

    public DirectStrikeService(MessageService messageService, SanctionService sanctionService, HybridConfig config) {
        this.messageService = messageService;
        this.sanctionService = sanctionService;
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
        this.sanctionService.reconfigure(config);
    }

    public void clear(UUID playerUuid) {
        lastAlertMillis.remove(playerUuid);
        sanctionService.clear(playerUuid);
    }

    public void processChecks(Player player, List<CheckResult> results) {
    }

    private void sendAlert(Player player, String signalId, DetectionSignalProgress progress, String detail) {
        if (config.alerts().silentMode()) {
            return;
        }
        DetectionPunishmentSettings settings = config.punishment().detection(signalId);
        if (!settings.enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastAlertMillis.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < ALERT_COOLDOWN_MILLIS) {
            return;
        }
        lastAlertMillis.put(player.getUniqueId(), now);

        int displayThreshold = progress.maxThresholdInt() > 0 ? progress.maxThresholdInt() : settings.maxVlThreshold();
        Map<String, String> placeholders = Map.of(
                "player", player.getName(),
                "check", signalId,
                "progress", progress.progressInt() + "/" + Math.max(1, displayThreshold),
                "detail", detail
        );
        String stream = formatOrFallback(
                "alerts.direct-stream",
                placeholders,
                "&#ffb44cHybridAC &#d9d9d9%player% &#d9d9d9%check% &#ff7878%progress% &#d9d9d9%detail%"
        );

        Bukkit.getOnlinePlayers().stream()
                .filter(online -> online.hasPermission("hybridac.alerts") || online.hasPermission("hybridac.admin"))
                .forEach(online -> online.sendMessage(stream));
    }

    private String formatOrFallback(String path, Map<String, String> placeholders, String fallback) {
        String formatted = messageService.format(path, placeholders);
        if (formatted.equals(path)) {
            return ColorUtil.colorize(fallback, placeholders);
        }
        return formatted;
    }
}
