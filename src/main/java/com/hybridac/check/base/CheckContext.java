package com.hybridac.check.base;

import com.hybridac.model.HitSample;
import com.hybridac.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public record CheckContext(Player player, Entity target, HitSample hitSample, PlayerData playerData) {
}
