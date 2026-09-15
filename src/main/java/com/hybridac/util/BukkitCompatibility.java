package com.hybridac.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public final class BukkitCompatibility {

    private BukkitCompatibility() {
    }

    public static int getPing(Player player) {
        try {
            Method method = player.getClass().getMethod("getPing");
            Object value = method.invoke(player);
            if (value instanceof Integer ping) {
                return ping;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            return player.spigot().getPing();
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static List<Player> getNearbyPlayers(Location location, double radius) {
        Collection<Entity> nearby = location.getWorld() == null
                ? List.of()
                : location.getWorld().getNearbyEntities(location, radius, radius, radius);
        return nearby.stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .toList();
    }
}
