package me.xginko.serverrestarts;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.xginko.serverrestarts.commands.VelocityRestartsCmd;
import me.xginko.serverrestarts.common.CommonUtil;
import me.xginko.serverrestarts.config.VelocityConfigImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

public final class ServerRestartsVelocity {

    private static VelocityConfigImpl config;
    private static ComponentLogger logger;
    private final ProxyServer server;
    private final Path pluginDir;

    @Inject
    public ServerRestartsVelocity(ProxyServer server, @DataDirectory Path pluginDir) {
        this.pluginDir = pluginDir;
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger = ComponentLogger.logger(getServer().getPluginManager().ensurePluginContainer(this).getDescription()
                .getName().orElse("ServerRestarts"));

        logger.info(Component.text("                                                          ").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("  ___                      ___        _            _      ").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" / __| ___ _ ___ _____ _ _| _ \\___ __| |_ __ _ _ _| |_ ___").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" \\__ \\/ -_) '_\\ V / -_) '_|   / -_|_-<  _/ _` | '_|  _(_-<").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" |___/\\___|_|  \\_/\\___|_| |_|_\\___/__/\\__\\__,_|_|  \\__/__/").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("                                                 by xGinko").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("                                                          ").style(CommonUtil.BOLD_COLOR));

        reloadPlugin();
        new VelocityRestartsCmd(this, server.getCommandManager()).register();
    }

    public static ComponentLogger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public void disablePlugin() {
        server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
    }

    public void reloadPlugin() {
        disablePlugin();
        reloadConfig();
        config.restart_times.forEach(restart_time -> server.getScheduler()
                .buildTask(this, shutdown -> server.shutdown(config.server_restarting))
                .delay(Duration.between(ZonedDateTime.now(config.time_zone_id), restart_time).getSeconds(), TimeUnit.SECONDS)
                .repeat(Duration.ofDays(1))
                .schedule());
    }

    private void reloadConfig() {
        try {
            config = new VelocityConfigImpl(pluginDir.toFile());
            config.saveConfig();
        } catch (Exception e) {
            logger.error("Error loading config!", e);
        }
    }
}
