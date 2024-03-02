package me.xginko.serverrestarts.commands.subcommands;

import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.commands.SubCommand;
import me.xginko.serverrestarts.common.SRPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class DisableSubCmd extends SubCommand {

    @Override
    public String label() {
        return "disable";
    }

    @Override
    public TextComponent description() {
        return Component.text("Disables all plugin tasks and listeners.").color(NamedTextColor.GRAY);
    }

    @Override
    public TextComponent syntax() {
        return Component.text("/restarts disable").color(NamedTextColor.GOLD);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission(SRPermission.DISABLE.permission())) {
            sender.sendMessage(ServerRestartsPaper.getLang(sender).no_permission);
            return;
        }

        sender.sendMessage(Component.text("Disabling SimpleRestarts...").color(NamedTextColor.RED));
        ServerRestartsPaper.getInstance().disableModules();
        sender.sendMessage(Component.text("Disabled all plugin listeners and tasks.").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("You can enable the plugin again using the reload command.").color(NamedTextColor.YELLOW));
    }
}