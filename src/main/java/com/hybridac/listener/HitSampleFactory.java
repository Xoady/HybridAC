package com.hybridac.listener;

import com.hybridac.model.HitSample;
import com.hybridac.model.HitboxPart;
import com.hybridac.model.VectorSnapshot;
import com.hybridac.player.PlayerData;
import com.hybridac.player.session.RotationFrame;
import com.hybridac.util.BukkitCompatibility;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;

public final class HitSampleFactory {

    public HitSampleFactory() {
    }

    public HitSample create(Player attacker, Entity target, PlayerData playerData) {
        RotationFrame latest = playerData.combatSession().latestRotation();
        RotationFrame previous = playerData.combatSession().previousRotation();
        Location eye = attacker.getEyeLocation();
        Location targetLocation = target instanceof LivingEntity livingEntity
                ? livingEntity.getEyeLocation().clone().subtract(0.0D, livingEntity.getHeight() * 0.35D, 0.0D)
                : target.getLocation().clone();

        Vector look = eye.getDirection().normalize();
        Vector toTarget = targetLocation.toVector().subtract(eye.toVector()).normalize();
        double dot = look.dot(toTarget);
        double aimError = Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))) / Math.PI;
        Vector velocity = attacker.getVelocity();
        Vector flatLook = look.clone().setY(0.0D).normalize();
        Vector flatVelocity = velocity.clone().setY(0.0D);
        double forward = flatVelocity.dot(flatLook);
        double strafe = flatVelocity.dot(new Vector(-flatLook.getZ(), 0.0D, flatLook.getX()));

        return new HitSample(
                attacker.getUniqueId(),
                target.getUniqueId(),
                target.getType().name(),
                attacker.getWorld().getName(),
                System.currentTimeMillis(),
                BukkitCompatibility.getPing(attacker),
                playerData.combatSession().cpsContext(),
                latest != null ? latest.yaw() : attacker.getLocation().getYaw(),
                latest != null ? latest.pitch() : attacker.getLocation().getPitch(),
                latest != null ? latest.yawDelta() : 0.0D,
                latest != null ? latest.pitchDelta() : 0.0D,
                previous != null ? previous.yawDelta() : 0.0D,
                previous != null ? previous.pitchDelta() : 0.0D,
                latest != null ? latest.angularAcceleration() : 0.0D,
                latest != null ? latest.angularJerk() : 0.0D,
                eye.distance(targetLocation),
                snapshot(look),
                snapshot(toTarget),
                aimError,
                approximateHitbox(targetLocation, target),
                attacker.isSprinting(),
                attacker.isOnGround(),
                snapshot(velocity),
                strafe,
                forward,
                !attacker.isOnGround() && velocity.getY() > 0.08D,
                !attacker.isOnGround() && velocity.getY() < -0.08D,
                target instanceof LivingEntity livingTarget ? livingTarget.getNoDamageTicks() : 0,
                attacker.getAttackCooldown(),
                attacker.hasLineOfSight(target),
                dot,
                false,
                false,
                new HashMap<>(playerData.lastSignals())
        );
    }

    private VectorSnapshot snapshot(Vector vector) {
        return new VectorSnapshot(vector.getX(), vector.getY(), vector.getZ());
    }

    private HitboxPart approximateHitbox(Location targetLocation, Entity target) {
        if (!(target instanceof LivingEntity livingEntity)) {
            return HitboxPart.UNKNOWN;
        }

        double relative = (targetLocation.getY() - livingEntity.getLocation().getY()) / livingEntity.getHeight();
        if (relative > 0.85D) {
            return HitboxPart.HEAD;
        }
        if (relative > 0.55D) {
            return HitboxPart.CHEST;
        }
        if (relative > 0.30D) {
            return HitboxPart.STOMACH;
        }
        return HitboxPart.LEGS;
    }
}
