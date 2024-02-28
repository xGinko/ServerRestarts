package me.xginko.serverrestarts.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import me.xginko.serverrestarts.ServerRestartsVelocity;
import me.xginko.serverrestarts.common.SRPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class VelocityRestartsCmd {

    private final @NotNull ServerRestartsVelocity plugin;
    private final @NotNull CommandManager commandManager;

    public VelocityRestartsCmd(@NotNull ServerRestartsVelocity plugin, @NotNull CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public void register() {
        LiteralCommandNode<CommandSource> commandNode = LiteralArgumentBuilder
                .<CommandSource>literal("vrestarts")
                .requires(sender -> Arrays.stream(SRPermission.values()).anyMatch(perm -> sender.hasPermission(perm.permission())))
                .executes(context -> {
                    showCmdHelp(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .requires(sender -> Arrays.stream(SRPermission.values()).anyMatch(perm -> sender.hasPermission(perm.permission())))
                        .executes(context -> {
                            showCmdHelp(context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission(SRPermission.RELOAD.permission()))
                        .executes(context -> {
                            plugin.reloadPlugin();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("disable")
                        .requires(source -> source.hasPermission(SRPermission.DISABLE.permission()))
                        .executes(context -> {
                            plugin.disablePlugin();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSource>literal("version")
                        .requires(source -> source.hasPermission(SRPermission.VERSION.permission()))
                        .executes(context -> {
                            PluginDescription pluginJSON = plugin.getServer().getPluginManager().ensurePluginContainer(plugin).getDescription();
                            context.getSource().sendMessage(Component.newline()
                                    .append(Component.text(pluginJSON.getName()+" "+pluginJSON.getVersion())
                                            .color(NamedTextColor.GOLD)
                                            .clickEvent(ClickEvent.openUrl(pluginJSON.getUrl().isPresent() ? pluginJSON.getUrl().get() : "https://github.com/xGinko/ServerRestart")))
                                    .append(Component.text(" by ").color(NamedTextColor.GRAY))
                                    .append(Component.text(pluginJSON.getAuthors().get(0))
                                            .color(NamedTextColor.DARK_AQUA)
                                            .clickEvent(ClickEvent.openUrl("https://github.com/xGinko")))
                                    .append(Component.newline()));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
        final BrigadierCommand command = new BrigadierCommand(commandNode);
        commandManager.register(commandManager.metaBuilder(command).plugin(plugin).build(), command);
    }

    private void showCmdHelp(CommandSource sender) {
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("ServerRestart Velocity Commands").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/restarts reload - Reload the plugin configuration").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/restarts version - Show the plugin version").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/restarts disable - Disable the plugin").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
    }
}