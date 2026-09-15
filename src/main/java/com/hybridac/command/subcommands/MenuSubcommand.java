package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.menu.SuspectedMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MenuSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "menu";
    }

    @Override
    public String permission() {
        return "hybridac.menu";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (!(sender instanceof Player player)) {
            context.messageService().send(sender, "commands.common.player-only");
            return true;
        }

        SuspectedMenu menu = new SuspectedMenu(context.config().menu());
        menu.populate(context.playerDataService(), context.config().menu());
        player.openInventory(menu.getInventory());
        return true;
    }
}
