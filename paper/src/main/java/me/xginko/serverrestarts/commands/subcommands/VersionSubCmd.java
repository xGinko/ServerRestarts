package me.xginko.serverrestarts.commands.subcommands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.xginko.serverrestarts.common.SRPermission;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.commands.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class VersionSubCmd extends SubCommand {

    @Override
    public String label() {
        return "version";
    }

    @Override
    public TextComponent description() {
        return Component.text("Shows the plugin version.").color(NamedTextColor.GRAY);
    }

    @Override
    public TextComponent syntax() {
        return Component.text("/restarts version").color(NamedTextColor.GOLD);
    }

    @Override
    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission(SRPermission.VERSION.permission())) {
            sender.sendMessage(ServerRestartsPaper.getLang(sender).no_permission);
            return;
        }

        String name, version, website, author;

        try {
            final PluginMeta pluginMeta = ServerRestartsPaper.getInstance().getPluginMeta();
            name = pluginMeta.getName();
            version = pluginMeta.getVersion();
            website = pluginMeta.getWebsite();
            author = pluginMeta.getAuthors().get(0);
        } catch (Throwable versionIncompatible) {
            final PluginDescriptionFile pluginYML = ServerRestartsPaper.getInstance().getDescription();
            name = pluginYML.getName();
            version = pluginYML.getVersion();
            website = pluginYML.getWebsite();
            author = pluginYML.getAuthors().get(0);
        }

        sender.sendMessage(
                Component.newline()
                .append(Component.text(name + " " + version).color(NamedTextColor.GOLD).clickEvent(ClickEvent.openUrl(website)))
                .append(Component.text(" by ").color(NamedTextColor.GRAY))
                .append(Component.text(author).color(NamedTextColor.DARK_AQUA).clickEvent(ClickEvent.openUrl("https://github.com/xGinko")))
                .append(Component.newline())
        );
    }
}