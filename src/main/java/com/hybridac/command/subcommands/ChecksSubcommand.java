package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import org.bukkit.command.CommandSender;

public final class ChecksSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "checks";
    }

    @Override
    public String permission() {
        return "hybridac.admin";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        context.checkRegistry().describeChecks().forEach(line ->
                context.messageService().send(sender, "commands.checks.line", java.util.Map.of("line", line))
        );
        return true;
    }
}
