package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import org.bukkit.command.CommandSender;

public final class ReloadSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return "hybridac.reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        context.plugin().reloadRuntime();
        context.messageService().send(sender, "commands.reload.success");
        return true;
    }
}
