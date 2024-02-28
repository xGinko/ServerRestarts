package me.xginko.serverrestarts.listener;

import me.xginko.serverrestarts.ServerRestartsPaper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinToggle implements Listener {

    public JoinToggle() {}

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!ServerRestartsPaper.joiningAllowed || ServerRestartsPaper.isRestarting) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ServerRestartsPaper.getLang(ServerRestartsPaper.getConfiguration().default_lang).server_restarting
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (!ServerRestartsPaper.joiningAllowed || ServerRestartsPaper.isRestarting) {
            event.getPlayer().kick(ServerRestartsPaper.getLang(event.getPlayer().locale()).server_restarting);
        }
    }
}
