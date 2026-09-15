package com.hybridac.player;

import com.hybridac.config.HybridConfig;
import com.hybridac.violation.PlayerSuspicionState;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerDataService {

    private final ConcurrentMap<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();
    private volatile HybridConfig config;

    public PlayerDataService(HybridConfig config) {
        this.config = config;
    }

    public PlayerData getOrCreate(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(uuid, createSuspicionState()));
    }

    public PlayerData get(UUID playerUuid) {
        return dataMap.get(playerUuid);
    }

    public void remove(UUID playerUuid) {
        dataMap.remove(playerUuid);
    }

    public Collection<PlayerData> all() {
        return dataMap.values();
    }

    public void reconfigure(HybridConfig config) {
        this.config = config;
    }

    private PlayerSuspicionState createSuspicionState() {
        return new PlayerSuspicionState(
                config.buffers().defaultDecayPerSecond(),
                config.buffers().codeMax(),
                config.buffers().botMax(),
                config.buffers().hybridDecayPerSecond()
        );
    }
}
