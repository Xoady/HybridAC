package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.model.RecordingLabel;
import com.hybridac.player.PlayerData;
import com.hybridac.recording.RecordingSession;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DataCollectorSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "datacollector";
    }

    @Override
    public String permission() {
        return "hybridac.datacollector";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (args.length < 2) {
            context.messageService().send(sender, "commands.datacollector.usage");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "record" -> {
                if (args.length < 4) {
                    context.messageService().send(sender, "commands.datacollector.record-usage");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[2]);
                if (player == null) {
                    context.messageService().send(sender, "commands.common.player-not-found");
                    return true;
                }

                String labelArg = args[3].toLowerCase();
                if (!labelArg.equals("legit") && !labelArg.equals("cheater") && !labelArg.equals("cheat")) {
                    context.messageService().send(sender, "commands.datacollector.invalid-label");
                    return true;
                }

                if (args.length < 5) {
                    context.messageService().send(sender, "commands.datacollector.comment-required");
                    return true;
                }

                String comment = String.join(" ", Arrays.copyOfRange(args, 4, args.length)).trim();
                RecordingLabel label = RecordingLabel.fromArgument(labelArg);

                boolean started = context.recordingService().start(player, label, sender, comment);
                if (started) {
                    PlayerData playerData = context.playerDataService().getOrCreate(player);
                    playerData.combatSession().reset();
                    playerData.suspicionState().reset();
                    playerData.lastSignals().clear();

                    context.messageService().sendList(sender, "commands.datacollector.started-lines", Map.of(
                            "player", player.getName(),
                            "label", label == RecordingLabel.LEGIT ? "LEGIT" : "CHEATER",
                            "comment", comment
                    ));
                } else {
                    context.messageService().send(sender, "commands.datacollector.already-recording", Map.of("player", player.getName()));
                }
            }
            case "stop" -> {
                if (args.length < 3) {
                    context.messageService().send(sender, "commands.datacollector.stop-usage");
                    return true;
                }

                Player player = Bukkit.getPlayerExact(args[2]);
                if (player == null) {
                    context.messageService().send(sender, "commands.common.player-not-found");
                    return true;
                }

                Optional<RecordingSession> stopped = context.recordingService().stop(player.getUniqueId());
                if (stopped.isPresent()) {
                    RecordingSession s = stopped.get();
                    context.messageService().sendList(sender, "commands.datacollector.stopped-lines", Map.of(
                            "player", s.playerName(),
                            "hits", String.valueOf(s.hitCount()),
                            "label", s.label() == RecordingLabel.LEGIT ? "LEGIT" : "CHEATER",
                            "comment", s.comment()
                    ));
                } else {
                    context.messageService().send(sender, "commands.datacollector.not-recording", Map.of("player", player.getName()));
                }
            }
            case "status" -> {
                if (args.length >= 3) {
                    Player p = Bukkit.getPlayerExact(args[2]);
                    if (p != null) {
                        Optional<RecordingSession> s = context.recordingService().getSession(p.getUniqueId());
                        if (s.isPresent()) {
                            RecordingSession sess = s.get();
                            context.messageService().sendList(sender, "commands.datacollector.status-lines", Map.of(
                                    "player", sess.playerName(),
                                    "hits", String.valueOf(sess.hitCount()),
                                    "label", sess.label().apiValue(),
                                    "comment", sess.comment()
                            ));
                            return true;
                        }
                    }
                }
                context.messageService().send(sender, "commands.datacollector.status-usage");
            }
            default -> context.messageService().send(sender, "commands.datacollector.usage");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        if (args.length == 2) {
            return List.of("record", "stop", "status");
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("record")) {
            return List.of("legit", "cheater");
        }
        return List.of();
    }
}
