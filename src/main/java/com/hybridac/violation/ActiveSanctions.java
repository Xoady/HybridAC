package com.hybridac.violation;

import org.bukkit.Location;

public final class ActiveSanctions {

    private double outgoingDamageMultiplier = 1.0D;
    private long outgoingDamageUntilMillis;
    private double incomingDamageMultiplier = 1.0D;
    private long incomingDamageUntilMillis;
    private double armorDamageMultiplier = 1.0D;
    private long armorDamageUntilMillis;
    private Location freezeAnchor;
    private long freezeUntilMillis;

    public synchronized void applyOutgoingDamage(double multiplier, long untilMillis) {
        cleanup(System.currentTimeMillis());
        outgoingDamageMultiplier = Math.min(outgoingDamageMultiplier, Math.max(0.0D, multiplier));
        outgoingDamageUntilMillis = Math.max(outgoingDamageUntilMillis, untilMillis);
    }

    public synchronized void applyIncomingDamage(double multiplier, long untilMillis) {
        cleanup(System.currentTimeMillis());
        incomingDamageMultiplier = Math.max(incomingDamageMultiplier, Math.max(1.0D, multiplier));
        incomingDamageUntilMillis = Math.max(incomingDamageUntilMillis, untilMillis);
    }

    public synchronized void applyArmorDamage(double multiplier, long untilMillis) {
        cleanup(System.currentTimeMillis());
        armorDamageMultiplier = Math.max(armorDamageMultiplier, Math.max(1.0D, multiplier));
        armorDamageUntilMillis = Math.max(armorDamageUntilMillis, untilMillis);
    }

    public synchronized void freeze(Location anchor, long untilMillis) {
        cleanup(System.currentTimeMillis());
        freezeAnchor = anchor == null ? null : anchor.clone();
        freezeUntilMillis = Math.max(freezeUntilMillis, untilMillis);
    }

    public synchronized double outgoingDamageMultiplier(long now) {
        cleanup(now);
        return outgoingDamageMultiplier;
    }

    public synchronized double incomingDamageMultiplier(long now) {
        cleanup(now);
        return incomingDamageMultiplier;
    }

    public synchronized double armorDamageMultiplier(long now) {
        cleanup(now);
        return armorDamageMultiplier;
    }

    public synchronized Location freezeAnchor(long now) {
        cleanup(now);
        return freezeAnchor == null ? null : freezeAnchor.clone();
    }

    public synchronized boolean hasActiveEffects(long now) {
        cleanup(now);
        return outgoingDamageUntilMillis > now
                || incomingDamageUntilMillis > now
                || armorDamageUntilMillis > now
                || freezeUntilMillis > now;
    }

    private void cleanup(long now) {
        if (outgoingDamageUntilMillis <= now) {
            outgoingDamageMultiplier = 1.0D;
            outgoingDamageUntilMillis = 0L;
        }
        if (incomingDamageUntilMillis <= now) {
            incomingDamageMultiplier = 1.0D;
            incomingDamageUntilMillis = 0L;
        }
        if (armorDamageUntilMillis <= now) {
            armorDamageMultiplier = 1.0D;
            armorDamageUntilMillis = 0L;
        }
        if (freezeUntilMillis <= now) {
            freezeAnchor = null;
            freezeUntilMillis = 0L;
        }
    }
}
