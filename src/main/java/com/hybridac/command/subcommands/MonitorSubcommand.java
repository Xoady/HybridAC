package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.monitor.ActionbarMonitorService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MonitorSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "monitor";
    }

    @Override
    public String permission() {
        return "hybridac.monitor";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        ActionbarMonitorService monitorService = context.plugin().bootstrapContext().monitorService();
        if (monitorService == null) {
            context.messageService().send(sender, "commands.monitor.unavailable");
            return true;
        }

        if (args.length < 2) {
            context.messageService().send(sender, "commands.monitor.usage");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    context.messageService().send(sender, "commands.monitor.add-usage");
                    return true;
                }
                Player viewer = Bukkit.getPlayerExact(args[2]);
                Player target = Bukkit.getPlayerExact(args[3]);

                if (viewer == null || target == null) {
                    context.messageService().send(sender, "commands.common.player-not-found");
                    return true;
                }

                monitorService.addMonitor(viewer, target);
                if (!sender.equals(viewer)) {
                    context.messageService().send(sender, "commands.monitor.assigned", Map.of(
                            "viewer", viewer.getName(),
                            "target", target.getName()
                    ));
                }
            }
            case "remove" -> {
                if (args.length < 3) {
                    if (sender instanceof Player p) {
                        monitorService.removeMonitor(p);
                        return true;
                    }
                    context.messageService().send(sender, "commands.monitor.remove-usage");
                    return true;
                }
                Player viewer = Bukkit.getPlayerExact(args[2]);
                if (viewer == null) {
                    context.messageService().send(sender, "commands.common.player-not-found");
                    return true;
                }
                boolean removed = monitorService.removeMonitor(viewer);
                if (removed) {
                    context.messageService().send(sender, "commands.monitor.removed-success", Map.of("viewer", viewer.getName()));
                } else {
                    context.messageService().send(sender, "commands.monitor.not-active", Map.of("viewer", viewer.getName()));
                }
            }
            case "clear" -> {
                monitorService.clearAll();
                context.messageService().send(sender, "commands.monitor.cleared");
            }
            case "list" -> {
                Map<UUID, UUID> active = monitorService.activeMonitors();
                if (active.isEmpty()) {
                    context.messageService().send(sender, "commands.monitor.list-empty");
                    return true;
                }
                context.messageService().send(sender, "commands.monitor.list-header", Map.of("count", String.valueOf(active.size())));
                for (Map.Entry<UUID, UUID> e : active.entrySet()) {
                    Player v = Bukkit.getPlayer(e.getKey());
                    Player t = Bukkit.getPlayer(e.getValue());
                    String vName = v != null ? v.getName() : e.getKey().toString();
                    String tName = t != null ? t.getName() : e.getValue().toString();
                    context.messageService().send(sender, "commands.monitor.list-item", Map.of(
                            "viewer", vName,
                            "target", tName
                    ));
                }
            }
            default -> context.messageService().send(sender, "commands.monitor.usage");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        if (args.length == 2) {
            return List.of("add", "remove", "list", "clear");
        }
        if (args.length == 3 || args.length == 4) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
