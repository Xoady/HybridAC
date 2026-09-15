package com.hybridac.integration;

import com.hybridac.config.HybridConfig;
import com.hybridac.player.PlayerData;
import com.hybridac.player.PlayerDataService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class HybridACExpansion extends PlaceholderExpansion {

    private final PlayerDataService playerDataService;
    private final HybridConfig config;

    public HybridACExpansion(PlayerDataService playerDataService, HybridConfig config) {
        this.playerDataService = playerDataService;
        this.config = config;
    }

    @Override
    public @NotNull String getAuthor() {
        return "HybridAC";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hybridac";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        if (params.equalsIgnoreCase("alerts")) {
            PlayerData data = playerDataService.get(player.getUniqueId());
            if (data == null) {
                return "";
            }
            List<Double> history = data.suspicionState().getMlHistory();
            if (history.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            int yellow = config.alerts().yellowThreshold();
            int red = config.alerts().redThreshold();

            for (int i = 0; i < history.size(); i++) {
                double val = history.get(i);
                int pct = (int) Math.round(val * 100.0D);
                String color = "§a";
                if (pct >= red) {
                    color = "§c";
                } else if (pct >= yellow) {
                    color = "§e";
                }
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(color).append(pct).append("%");
            }
            return sb.toString();
        }
        return null;
    }
}
