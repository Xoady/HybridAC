package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class StopSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "stop";
    }

    @Override
    public String permission() {
        return "hybridac.record";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (args.length < 2) {
            context.messageService().send(sender, "commands.stop.usage");
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            context.messageService().send(sender, "commands.common.player-not-found");
            return true;
        }
        var stopped = context.recordingService().stop(player.getUniqueId());
        context.messageService().send(
                sender,
                stopped.isPresent() ? "commands.stop.stopped" : "commands.stop.not-active",
                Map.of(
                        "player", player.getName(),
                        "hits", stopped.map(session -> Integer.toString(session.hitCount())).orElse("0")
                )
        );
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        return args.length == 2 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}
