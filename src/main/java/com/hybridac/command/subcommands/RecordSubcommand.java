package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.model.RecordingLabel;
import com.hybridac.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class RecordSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "record";
    }

    @Override
    public String permission() {
        return "hybridac.record";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (args.length < 3) {
            context.messageService().send(sender, "commands.record.usage");
            return true;
        }
        Player player = Bukkit.getPlayerExact(args[2]);
        if (player == null) {
            context.messageService().send(sender, "commands.common.player-not-found");
            return true;
        }

        RecordingLabel label = RecordingLabel.fromArgument(args[1]);
        boolean started = context.recordingService().start(player, label, sender);
        if (started) {
            PlayerData playerData = context.playerDataService().getOrCreate(player);
            playerData.combatSession().reset();
            playerData.suspicionState().reset();
            playerData.lastSignals().clear();
            playerData.resetSprintTracking();
        }
        context.messageService().send(sender,
                started ? "commands.record.started" : "commands.record.already-active",
                Map.of("player", player.getName(), "label", label.apiValue()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        if (args.length == 2) {
            return List.of("legit", "cheats");
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
