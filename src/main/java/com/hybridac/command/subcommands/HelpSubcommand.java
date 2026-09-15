package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import org.bukkit.command.CommandSender;

public final class HelpSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        context.messageService().sendList(sender, "commands.help.lines", java.util.Map.of());
        return true;
    }
}
