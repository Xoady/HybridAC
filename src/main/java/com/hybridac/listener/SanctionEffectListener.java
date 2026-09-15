package com.hybridac.listener;

import com.hybridac.violation.SanctionService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class SanctionEffectListener implements Listener {

    private static final double FREEZE_EPSILON_SQUARED = 0.0004D;

    private final SanctionService sanctionService;

    public SanctionEffectListener(SanctionService sanctionService) {
        this.sanctionService = sanctionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        double damage = event.getDamage();
        if (event.getDamager() instanceof Player damager) {
            damage = sanctionService.applyOutgoingDamage(damager, damage);
        }
        if (event.getEntity() instanceof Player victim) {
            damage = sanctionService.applyIncomingDamage(victim, damage);
            sanctionService.applyArmorDamage(victim);
        }
        event.setDamage(damage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Location anchor = sanctionService.freezeAnchor(event.getPlayer());
        if (anchor == null) {
            return;
        }
        if (!anchor.getWorld().equals(event.getTo().getWorld())) {
            event.setTo(anchor);
            return;
        }
        if (event.getTo().toVector().distanceSquared(anchor.toVector()) <= FREEZE_EPSILON_SQUARED) {
            return;
        }
        Location corrected = event.getTo().clone();
        corrected.setX(anchor.getX());
        corrected.setY(anchor.getY());
        corrected.setZ(anchor.getZ());
        event.setTo(corrected);
    }
}
