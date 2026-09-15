package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AlertsSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "alerts";
    }

    @Override
    public String permission() {
        return "hybridac.alerts";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (!(sender instanceof Player player)) {
            context.messageService().send(sender, "commands.common.player-only");
            return true;
        }
        boolean enabled = context.alertStreamService().toggle(player);
        int threshold = (int) Math.round(context.config().hybrid().alertThreshold() * 100.0D);
        java.util.Map<String, String> placeholders = java.util.Map.of("threshold", threshold + "%");
        context.messageService().send(sender, enabled ? "commands.alerts.enabled" : "commands.alerts.disabled", placeholders);
        return true;
    }
}
