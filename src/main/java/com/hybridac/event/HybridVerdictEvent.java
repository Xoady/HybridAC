package com.hybridac.event;

import com.hybridac.violation.HybridVerdict;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HybridVerdictEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final HybridVerdict verdict;

    public HybridVerdictEvent(Player player, HybridVerdict verdict) {
        this.player = player;
        this.verdict = verdict;
    }

    public Player player() {
        return player;
    }

    public HybridVerdict verdict() {
        return verdict;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
