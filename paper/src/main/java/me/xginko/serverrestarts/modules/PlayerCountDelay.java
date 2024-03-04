package me.xginko.serverrestarts.modules;

import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.config.PaperConfigImpl;
import me.xginko.serverrestarts.events.PreRestartEvent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.time.Duration;

public class PlayerCountDelay implements ServerRestartModule, Listener {

    private final Server server;
    private final long delay_ticks;
    private final int min_players_for_delay;
    private final boolean should_log, notify_players;

    public PlayerCountDelay() {
        shouldEnable();
        this.server = ServerRestartsPaper.getInstance().getServer();
        PaperConfigImpl config = ServerRestartsPaper.getConfiguration();
        config.master().addComment("player-count-delay.enable",
                "If enabled, will only restart once playercount is below the configured number.");
        this.should_log = config.getBoolean("player-count-delay.log", true);
        this.notify_players = config.getBoolean("player-count-delay.notify-players", false);
        this.min_players_for_delay = Math.max(1, config.getInt("player-count-delay.min-players-for-delay", 20,
                "If the player count is this value or bigger, restart logic will be delayed."));
        this.delay_ticks = Math.max(1, config.getInt("player-count-delay.delay-seconds", 10800,
                "Time in seconds until plugin will check again if it can restart.")) * 20L;
    }

    @Override
    public boolean shouldEnable() {
        return ServerRestartsPaper.getConfiguration().getBoolean("player-count-delay.enable", false);
    }

    @Override
    public void enable() {
        server.getPluginManager().registerEvents(this, ServerRestartsPaper.getInstance());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPreRestart(PreRestartEvent event) {
        if (ServerRestartsPaper.isRestarting) {
            disable();
            return;
        }

        if (server.getOnlinePlayers().size() < min_players_for_delay) {
            return;
        }

        event.setDelayTicks(delay_ticks);

        if (notify_players) {
            for (final Player player : server.getOnlinePlayers()) {
                switch (ServerRestartsPaper.getConfiguration().message_mode) {
                    case ACTIONBAR -> player.sendActionBar(ServerRestartsPaper.getLang(player.locale())
                            .restart_delayed_playercount(Duration.ofMillis(delay_ticks * 50L)));
                    case BROADCAST -> player.sendMessage(ServerRestartsPaper.getLang(player.locale())
                            .time_until_restart(Duration.ofMillis(delay_ticks * 50L)));
                }
            }
        }

        if (should_log) {
            ServerRestartsPaper.getLog().info(ServerRestartsPaper.getLang(ServerRestartsPaper.getConfiguration().default_lang)
                    .restart_delayed_playercount(Duration.ofMillis(delay_ticks * 50L)));
        }
    }
}
