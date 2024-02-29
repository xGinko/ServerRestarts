package me.xginko.serverrestarts.commands;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.common.CommonUtil;
import me.xginko.serverrestarts.events.RestartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RestartGracefullyCmd implements TabCompleter, CommandExecutor {

    private static final List<String> suggestions = List.of("cancel", "30", "60", "120", "240");
    private ScheduledTask activeRestart;

    public RestartGracefullyCmd() {}

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return args.length == 1 ? suggestions : Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            return false;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (activeRestart != null && !activeRestart.isCancelled()) {
                activeRestart.cancel();
                sender.sendMessage(Component.text("Cancelled graceful restart.").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("There is no pending restart scheduled by this command."));
            }
            return true;
        }

        try {
            final AtomicReference<Duration> timeLeft = new AtomicReference<>(Duration.ofSeconds(Math.abs(Integer.parseInt(args[0]))));
            sender.sendMessage(Component.text("Scheduled graceful restart in " + CommonUtil.formatDuration(timeLeft.get())).color(CommonUtil.COLOR));

            if (activeRestart != null) activeRestart.cancel();
            final ServerRestartsPaper plugin = ServerRestartsPaper.getInstance();

            this.activeRestart = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, countdown -> {
                Duration remaining = timeLeft.get();

                if (remaining.isZero() || remaining.isNegative()) {
                    RestartEvent restartEvent = new RestartEvent(
                            false,
                            RestartEvent.RestartType.COMMAND,
                            ServerRestartsPaper.getConfiguration().restart_method,
                            true,
                            true
                    );

                    if (!restartEvent.callEvent()) return;
                    ServerRestartsPaper.joiningAllowed = false;

                    ServerRestartsPaper.getLog().info(Component.text("Restarting server!")
                            .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

                    ServerRestartsPaper.restart(
                            restartEvent.getType(),
                            restartEvent.getMethod(),
                            restartEvent.getDisableJoin(),
                            restartEvent.getKickAll()
                    );

                    return;
                }

                if (ServerRestartsPaper.getConfiguration().notification_times.stream().anyMatch(notifyTimeLeft -> notifyTimeLeft.equals(remaining))) {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        switch (ServerRestartsPaper.getConfiguration().message_mode) {
                            case ACTIONBAR -> player.sendActionBar(ServerRestartsPaper.getLang(player.locale()).time_until_restart(remaining));
                            case BROADCAST -> player.sendMessage(ServerRestartsPaper.getLang(player.locale()).time_until_restart(remaining));
                        }
                    }
                }

                timeLeft.set(remaining.minusMillis(500));
            }, 1L, 10L);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text(args[0] + " is not a valid integer."));
        }

        return true;
    }
}