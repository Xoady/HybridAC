package com.hybridac.command.subcommands;

import com.hybridac.command.CommandContext;
import com.hybridac.command.HybridACSubcommand;
import com.hybridac.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class SuspiciousSubcommand implements HybridACSubcommand {

    @Override
    public String name() {
        return "suspicious";
    }

    @Override
    public String permission() {
        return "hybridac.suspicious";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args, CommandContext context) {
        if (args.length < 2) {
            context.messageService().send(sender, "commands.suspicious.usage");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "alerts" -> handleAlerts(sender, context);
            case "list" -> handleList(sender, context);
            case "top" -> handleTop(sender, context);
            case "flagged" -> handleFlagged(sender, context);
            default -> context.messageService().send(sender, "commands.suspicious.usage");
        }
        return true;
    }

    private void handleAlerts(CommandSender sender, CommandContext context) {
        if (!sender.hasPermission("hybridac.suspicious.alerts") && !sender.hasPermission("hybridac.admin") && !sender.hasPermission("hybridac.alerts")) {
            context.messageService().send(sender, "commands.common.no-permission");
            return;
        }
        if (!(sender instanceof Player player)) {
            context.messageService().send(sender, "commands.common.player-only");
            return;
        }

        boolean active = context.alertStreamService().toggle(player);
        if (active) {
            context.messageService().send(player, "commands.suspicious.alerts-enabled");
        } else {
            context.messageService().send(player, "commands.suspicious.alerts-disabled");
        }
    }

    private void handleList(CommandSender sender, CommandContext context) {
        if (!sender.hasPermission("hybridac.suspicious.list") && !sender.hasPermission("hybridac.admin")) {
            context.messageService().send(sender, "commands.common.no-permission");
            return;
        }

        List<SuspectEntry> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = context.playerDataService().get(p.getUniqueId());
            if (data == null) continue;
            double conf = Math.max(data.suspicionState().smoothedConfidence(), data.suspicionState().lastMlProbabilityRaw());
            int hits = data.combatSession().hitCount();
            if (conf >= 0.05 || hits > 0) {
                list.add(new SuspectEntry(p.getName(), (int) Math.round(conf * 100.0), hits, data.suspicionState().evidenceCount(), (int) Math.round(data.combatSession().estimatedCps()), "-"));
            }
        }

        if (list.isEmpty()) {
            context.messageService().send(sender, "commands.suspicious.list-empty");
            return;
        }

        context.messageService().send(sender, "commands.suspicious.list-header");
        for (SuspectEntry entry : list) {
            context.messageService().send(sender, "commands.suspicious.list-item", Map.of(
                    "player", entry.name,
                    "confidence", String.valueOf(entry.confidence),
                    "hits", String.valueOf(entry.hits),
                    "cps", String.valueOf(entry.cps)
            ));
        }
    }

    private void handleTop(CommandSender sender, CommandContext context) {
        if (!sender.hasPermission("hybridac.suspicious.top") && !sender.hasPermission("hybridac.admin")) {
            context.messageService().send(sender, "commands.common.no-permission");
            return;
        }

        List<SuspectEntry> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = context.playerDataService().get(p.getUniqueId());
            if (data == null) continue;
            double conf = Math.max(data.suspicionState().smoothedConfidence(), data.suspicionState().lastMlProbabilityRaw());
            int flags = data.suspicionState().evidenceCount();
            int hits = data.combatSession().hitCount();
            if (conf > 0.01 || flags > 0) {
                list.add(new SuspectEntry(p.getName(), (int) Math.round(conf * 100.0), hits, flags, (int) Math.round(data.combatSession().estimatedCps()), "-"));
            }
        }

        list.sort((a, b) -> {
            int cmp = Integer.compare(b.confidence, a.confidence);
            if (cmp != 0) return cmp;
            return Integer.compare(b.flags, a.flags);
        });

        if (list.isEmpty()) {
            context.messageService().send(sender, "commands.suspicious.top-empty");
            return;
        }

        context.messageService().send(sender, "commands.suspicious.top-header");
        int rank = 1;
        for (SuspectEntry entry : list) {
            if (rank > 10) break;
            context.messageService().send(sender, "commands.suspicious.top-item", Map.of(
                    "rank", String.valueOf(rank++),
                    "player", entry.name,
                    "confidence", String.valueOf(entry.confidence),
                    "flags", String.valueOf(entry.flags),
                    "cps", String.valueOf(entry.cps)
            ));
        }
    }

    private void handleFlagged(CommandSender sender, CommandContext context) {
        if (!sender.hasPermission("hybridac.suspicious.flagged") && !sender.hasPermission("hybridac.admin")) {
            context.messageService().send(sender, "commands.common.no-permission");
            return;
        }

        List<SuspectEntry> list = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData data = context.playerDataService().get(p.getUniqueId());
            if (data == null) continue;
            int flags = data.suspicionState().evidenceCount();
            double conf = Math.max(data.suspicionState().smoothedConfidence(), data.suspicionState().lastMlProbabilityRaw());
            String lastCheck = data.lastSignals().isEmpty() ? "None" : data.lastSignals().keySet().iterator().next();
            if (flags > 0 || conf >= 0.20) {
                list.add(new SuspectEntry(p.getName(), (int) Math.round(conf * 100.0), data.combatSession().hitCount(), flags, (int) Math.round(data.combatSession().estimatedCps()), lastCheck));
            }
        }

        if (list.isEmpty()) {
            context.messageService().send(sender, "commands.suspicious.flagged-empty");
            return;
        }

        context.messageService().send(sender, "commands.suspicious.flagged-header");
        for (SuspectEntry entry : list) {
            context.messageService().send(sender, "commands.suspicious.flagged-item", Map.of(
                    "player", entry.name,
                    "signals", String.valueOf(entry.flags),
                    "last_check", entry.lastCheck,
                    "confidence", String.valueOf(entry.confidence)
            ));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args, CommandContext context) {
        if (args.length == 2) {
            List<String> options = List.of("alerts", "list", "top", "flagged");
            String prefix = args[1].toLowerCase();
            return options.stream().filter(o -> o.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private record SuspectEntry(String name, int confidence, int hits, int flags, int cps, String lastCheck) {}
}
