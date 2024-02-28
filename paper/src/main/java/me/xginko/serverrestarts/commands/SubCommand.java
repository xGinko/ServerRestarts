package me.xginko.serverrestarts.commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;

public abstract class SubCommand {
    public abstract String label();
    public abstract TextComponent description();
    public abstract TextComponent syntax();
    public abstract void perform(CommandSender sender, String[] args);
}
