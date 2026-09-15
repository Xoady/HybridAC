package com.hybridac.monitor;

import com.hybridac.message.MessageService;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import com.hybridac.util.TaskUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionbarMonitorService {

    private final Map<UUID, UUID> viewerToTarget = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final PlayerDataService playerDataService;
    private final MessageService messageService;
    private TaskUtil.CancellableTask tickerTask;

    public ActionbarMonitorService(Plugin plugin, PlayerDataService playerDataService, MessageService messageService) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.messageService = messageService;
        startTicker();
    }

    public void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = TaskUtil.runTimer(plugin, this::tick, 5L, 5L);
    }

    public void addMonitor(Player viewer, Player target) {
        viewerToTarget.put(viewer.getUniqueId(), target.getUniqueId());
        if (messageService != null) {
            messageService.send(viewer, "commands.monitor.started", Map.of("target", target.getName()));
        }
        renderSingle(viewer, target);
    }

    public boolean removeMonitor(Player viewer) {
        UUID removed = viewerToTarget.remove(viewer.getUniqueId());
        if (removed != null) {
            if (messageService != null) {
                messageService.send(viewer, "commands.monitor.stopped");
            }
            viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            return true;
        }
        return false;
    }

    public void clearAll() {
        for (UUID viewerId : viewerToTarget.keySet()) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }
        }
        viewerToTarget.clear();
    }

    public boolean isMonitoring(UUID viewerUuid) {
        return viewerToTarget.containsKey(viewerUuid);
    }

    public UUID getTarget(UUID viewerUuid) {
        return viewerToTarget.get(viewerUuid);
    }

    public Map<UUID, UUID> activeMonitors() {
        return Collections.unmodifiableMap(viewerToTarget);
    }

    public void notifyInference(UUID targetUuid, String targetName, double cheatProbability, double cps, String modelName) {
        if (viewerToTarget.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, UUID> entry : viewerToTarget.entrySet()) {
            if (entry.getValue().equals(targetUuid)) {
                Player viewer = Bukkit.getPlayer(entry.getKey());
                if (viewer != null && viewer.isOnline()) {
                    sendFormattedActionBar(viewer, targetName, cheatProbability, cps, modelName);
                }
            }
        }
    }

    private void tick() {
        if (viewerToTarget.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, UUID> entry : viewerToTarget.entrySet()) {
            UUID viewerId = entry.getKey();
            UUID targetId = entry.getValue();

            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                viewerToTarget.remove(viewerId);
                continue;
            }

            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cOffline"));
                continue;
            }

            renderSingle(viewer, target);
        }
    }

    private void renderSingle(Player viewer, Player target) {
        PlayerData targetData = playerDataService.getOrCreate(target);
        double rawProb = targetData.suspicionState().lastMlProbabilityRaw();
        double smoothed = targetData.suspicionState().smoothedConfidence();
        double cheatProb = Math.max(rawProb, smoothed);
        double cps = targetData.combatSession().estimatedCps();
        String model = targetData.suspicionState().lastModelVersion();
        if (model == null || model.isBlank() || model.equals("unavailable")) {
            model = "neural";
        }

        sendFormattedActionBar(viewer, target.getName(), cheatProb, cps, model);
    }

    private void sendFormattedActionBar(Player viewer, String targetName, double cheatProbability, double cps, String modelName) {
        double pct = Math.min(100.0, Math.max(0.0, cheatProbability * 100.0));
        String color;
        if (pct < 30.0) {
            color = "§a";
        } else if (pct < 70.0) {
            color = "§e";
        } else {
            color = "§c§l";
        }

        String formattedPct = String.format(Locale.US, "%.1f%%", pct);
        String message = "§f" + targetName + " §8| " + color + formattedPct;
        viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void shutdown() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        clearAll();
    }
}
