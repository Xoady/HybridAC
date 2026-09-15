package com.hybridac.violation;

import com.hybridac.config.DetectionPunishmentSettings;
import com.hybridac.config.HybridConfig;
import com.hybridac.config.PunishmentActionSettings;
import com.hybridac.config.PunishmentThresholdSettings;
import com.hybridac.stats.StatsService;
import com.hybridac.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SanctionService {

    private final JavaPlugin plugin;
    private final StorageService storageService;
    private final StatsService statsService;
    private final Map<UUID, Map<String, DetectionSessionState>> detectionStates = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveSanctions> activeSanctions = new ConcurrentHashMap<>();
    private volatile HybridConfig config;

    public SanctionService(JavaPlugin plugin, StorageService storageService, StatsService statsService, HybridConfig config) {
        this.plugin = plugin;
        this.storageService = storageService;
        this.statsService = statsService;
        this.config = config;
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
    }

    public void clear(UUID playerUuid) {
        detectionStates.remove(playerUuid);
        activeSanctions.remove(playerUuid);
    }

    public DetectionSignalProgress registerVl(Player player, String detectionId, String detail) {
        DetectionPunishmentSettings settings = config.punishment().detection(detectionId);
        if (!settings.enabled()) {
            return new DetectionSignalProgress(0.0D, 0.0D, false);
        }
        long now = System.currentTimeMillis();
        DetectionSessionState state = state(player.getUniqueId(), detectionId);
        double progress = state.recordVl(now, settings.sessionWindowSeconds() * 1_000L);
        boolean triggered = applyThresholds(player, detectionId, detail, progress, settings, state, now);
        return new DetectionSignalProgress(progress, settings.maxVlThreshold(), triggered);
    }

    public DetectionSignalProgress registerPercent(Player player, String detectionId, double percent, String detail) {
        DetectionPunishmentSettings settings = config.punishment().detection(detectionId);
        if (!settings.enabled()) {
            return new DetectionSignalProgress(0.0D, 0.0D, false);
        }
        long now = System.currentTimeMillis();
        DetectionSessionState state = state(player.getUniqueId(), detectionId);
        double progress = state.recordPercent(percent, now, settings.sessionWindowSeconds() * 1_000L);
        boolean triggered = applyThresholds(player, detectionId, detail, progress, settings, state, now);
        double maxThreshold = settings.thresholds().stream().mapToDouble(PunishmentThresholdSettings::threshold).max().orElse(100.0D);
        return new DetectionSignalProgress(progress, maxThreshold, triggered);
    }

    public double applyOutgoingDamage(Player player, double damage) {
        ActiveSanctions sanctions = activeSanctions.get(player.getUniqueId());
        if (sanctions == null) {
            return damage;
        }
        return damage * sanctions.outgoingDamageMultiplier(System.currentTimeMillis());
    }

    public double applyIncomingDamage(Player player, double damage) {
        ActiveSanctions sanctions = activeSanctions.get(player.getUniqueId());
        if (sanctions == null) {
            return damage;
        }
        return damage * sanctions.incomingDamageMultiplier(System.currentTimeMillis());
    }

    public void applyArmorDamage(Player player) {
        ActiveSanctions sanctions = activeSanctions.get(player.getUniqueId());
        if (sanctions == null) {
            return;
        }
        double multiplier = sanctions.armorDamageMultiplier(System.currentTimeMillis());
        if (multiplier <= 1.0D) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        boolean updated = false;
        int extraDamage = Math.max(1, (int) Math.round(multiplier - 1.0D));
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item == null || item.getType() == Material.AIR || item.getType().getMaxDurability() <= 0) {
                continue;
            }
            short nextDamage = (short) (item.getDurability() + extraDamage);
            if (nextDamage >= item.getType().getMaxDurability()) {
                armor[i] = null;
            } else {
                item.setDurability(nextDamage);
                armor[i] = item;
            }
            updated = true;
        }
        if (updated) {
            inventory.setArmorContents(armor);
        }
    }

    public Location freezeAnchor(Player player) {
        ActiveSanctions sanctions = activeSanctions.get(player.getUniqueId());
        if (sanctions == null) {
            return null;
        }
        Location anchor = sanctions.freezeAnchor(System.currentTimeMillis());
        if (anchor == null || !sanctions.hasActiveEffects(System.currentTimeMillis())) {
            if (!sanctions.hasActiveEffects(System.currentTimeMillis())) {
                activeSanctions.remove(player.getUniqueId());
            }
            return null;
        }
        return anchor;
    }

    private DetectionSessionState state(UUID playerUuid, String detectionId) {
        return detectionStates
                .computeIfAbsent(playerUuid, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(detectionId, ignored -> new DetectionSessionState());
    }

    private boolean applyThresholds(Player player, String detectionId, String detail, double progress, DetectionPunishmentSettings settings, DetectionSessionState state, long now) {
        boolean triggered = false;
        for (PunishmentThresholdSettings threshold : settings.thresholds()) {
            if (progress < threshold.threshold() || !state.markThresholdTriggered(threshold.threshold())) {
                continue;
            }
            triggered = true;
            if (config.punishment().alertOnly()) {
                continue;
            }
            executeThreshold(player, detectionId, progress, threshold, detail, now);
        }
        return triggered;
    }

    private void executeThreshold(Player player, String detectionId, double progress, PunishmentThresholdSettings threshold, String detail, long now) {
        for (PunishmentActionSettings action : threshold.actions()) {
            executeAction(player, detectionId, progress, threshold.threshold(), detail, action, now);
        }
        storageService.savePunishment(
                player.getUniqueId(),
                player.getName(),
                progress,
                List.of(detectionId, "progress=" + format(progress), "threshold=" + format(threshold.threshold()), detail)
        );
        statsService.incrementPunishments();
    }

    private void executeAction(Player player, String detectionId, double progress, double threshold, String detail, PunishmentActionSettings action, long now) {
        long untilMillis = now + Math.max(1L, action.durationSeconds()) * 1_000L;
        switch (action.normalizedType()) {
            case "COMMAND" -> dispatchCommands(player, detectionId, progress, threshold, detail, action.commands());
            case "REDUCE_DAMAGE" -> sanctions(player.getUniqueId()).applyOutgoingDamage(action.multiplier(), untilMillis);
            case "INCREASE_DAMAGE" -> sanctions(player.getUniqueId()).applyIncomingDamage(action.multiplier(), untilMillis);
            case "ARMOR_BREAK" -> sanctions(player.getUniqueId()).applyArmorDamage(action.multiplier(), untilMillis);
            case "FREEZE" -> sanctions(player.getUniqueId()).freeze(player.getLocation(), untilMillis);
            default -> plugin.getLogger().warning("Unknown punishment action type: " + action.type());
        }
    }

    private void dispatchCommands(Player player, String detectionId, double progress, double threshold, String detail, List<String> commands) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String command : commands) {
            String resolved = command
                    .replace("%player%", player.getName())
                    .replace("%check%", detectionId)
                    .replace("%progress%", format(progress))
                    .replace("%threshold%", format(threshold))
                    .replace("%detail%", detail);
            Bukkit.dispatchCommand(console, resolved);
        }
    }

    private ActiveSanctions sanctions(UUID playerUuid) {
        return activeSanctions.computeIfAbsent(playerUuid, ignored -> new ActiveSanctions());
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
