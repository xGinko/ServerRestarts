package me.xginko.serverrestarts.commands;

import me.xginko.serverrestarts.commands.subcommands.VersionSubCmd;
import me.xginko.serverrestarts.common.SRPermission;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.commands.subcommands.DisableSubCmd;
import me.xginko.serverrestarts.commands.subcommands.ReloadSubCmd;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RestartsCmd implements TabCompleter, CommandExecutor {

    private final List<SubCommand> subCommands;
    private final List<String> tabCompleter;

    public RestartsCmd() {
        subCommands = List.of(new ReloadSubCmd(), new VersionSubCmd(), new DisableSubCmd());
        tabCompleter = subCommands.stream().map(SubCommand::label).toList();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return args.length == 1 ? tabCompleter : Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            showCmdHelp(sender);
            return true;
        }

        for (final SubCommand subCommand : subCommands) {
            if (args[0].equalsIgnoreCase(subCommand.label())) {
                subCommand.perform(sender, args);
                return true;
            }
        }

        showCmdHelp(sender);
        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void showCmdHelp(CommandSender sender) {
        if (Arrays.stream(SRPermission.values()).noneMatch(perm -> sender.hasPermission(perm.permission()))) return;

        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text(ServerRestartsPaper.getInstance().getPluginMeta().getName() + " Commands").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        subCommands.forEach(subCommand -> sender.sendMessage(
                subCommand.syntax().append(Component.text(" - ").color(NamedTextColor.DARK_GRAY)).append(subCommand.description())));
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
    }
}