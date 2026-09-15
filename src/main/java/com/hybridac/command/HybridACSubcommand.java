package com.hybridac.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface HybridACSubcommand {

    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args, CommandContext context);

    default List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        return List.of();
    }
}
