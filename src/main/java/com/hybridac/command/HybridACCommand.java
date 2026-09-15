package com.hybridac.command;

import com.hybridac.command.subcommands.AlertsSubcommand;
import com.hybridac.command.subcommands.AuthSubcommand;
import com.hybridac.command.subcommands.ChecksSubcommand;
import com.hybridac.command.subcommands.DataCollectorSubcommand;
import com.hybridac.command.subcommands.HelpSubcommand;
import com.hybridac.command.subcommands.MenuSubcommand;
import com.hybridac.command.subcommands.MonitorSubcommand;
import com.hybridac.command.subcommands.ReloadSubcommand;
import com.hybridac.command.subcommands.SuspiciousSubcommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HybridACCommand implements CommandExecutor, TabCompleter {

    private final Map<String, HybridACSubcommand> subcommands = new LinkedHashMap<>();
    private final CommandContext context;

    public HybridACCommand(CommandContext context) {
        this.context = context;
        register(new HelpSubcommand());
        register(new AuthSubcommand());
        register(new MonitorSubcommand());
        register(new DataCollectorSubcommand());
        register(new ReloadSubcommand());
        register(new SuspiciousSubcommand());
        register(new ChecksSubcommand());
        register(new AlertsSubcommand());
        register(new MenuSubcommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return subcommands.get("help").execute(sender, args, context);
        }

        HybridACSubcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) {
            context.messageService().send(sender, "commands.common.unknown-subcommand");
            return true;
        }
        if (!subcommand.permission().isBlank() && !sender.hasPermission(subcommand.permission())) {
            context.messageService().send(sender, "commands.common.no-permission");
            return true;
        }
        return subcommand.execute(sender, args, context);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return subcommands.keySet().stream()
                    .filter(name -> name.startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        HybridACSubcommand subcommand = subcommands.get(args[0].toLowerCase());
        return subcommand == null ? List.of() : subcommand.tabComplete(sender, args, context);
    }

    private void register(HybridACSubcommand subcommand) {
        subcommands.put(subcommand.name(), subcommand);
    }
}
