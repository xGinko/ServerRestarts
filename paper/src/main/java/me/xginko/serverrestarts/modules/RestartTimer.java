package me.xginko.serverrestarts.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.config.PaperConfigImpl;
import me.xginko.serverrestarts.events.PreRestartEvent;
import me.xginko.serverrestarts.events.RestartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RestartTimer implements ServerRestartModule {
    private record RestartTask(ScheduledTask countdownTask, RestartEvent.RestartType restartType) {}

    private final @NotNull ServerRestartsPaper plugin;
    private final @NotNull Set<ScheduledTask> pendingRestarts;
    private final @NotNull PaperConfigImpl config;
    private @Nullable RestartTask activeRestart;
    private final boolean do_safe_restart;

    public RestartTimer() {
        shouldEnable();
        this.plugin = ServerRestartsPaper.getInstance();
        this.config = ServerRestartsPaper.getConfiguration();
        this.pendingRestarts = new HashSet<>(config.restart_times.size());
        this.do_safe_restart = config.getBoolean("general.restart-gracefully", true, """
                Will disable joining and kick all players before restarting.\s
                If set to false, will immediately shutdown/restart (not advised).""");
    }

    @Override
    public boolean shouldEnable() {
        return !ServerRestartsPaper.getConfiguration().restart_times.isEmpty();
    }

    @Override
    public void enable() {
        for (ZonedDateTime restart_time : config.restart_times) {
            final Duration time_left_until_restart = getAdjustedDelay(restart_time);
            pendingRestarts.add(plugin.getServer().getAsyncScheduler().runDelayed(
                    plugin,
                    initRestart -> tryInitRestart(time_left_until_restart),
                    time_left_until_restart.toMillis(),
                    TimeUnit.MILLISECONDS
            ));
        }
    }

    /*
    * This method helps getting an accurate delay by taking configured notify times into consideration.
    *
    * For example:
    *   The next restart should happen in 5 minutes.
    *   We have configured to notify players when there is only 30mins left, then 15mins, then
    *   5mins, then 1min, etc.
    *   We cant take away 30mins from the delay until countdown because that would result in a negative duration.
    *   Therefore we filter out any notification time that would result in a zero or negative delay and use the biggest one
    *   of those results.
    * */
    private Duration getAdjustedDelay(ZonedDateTime restart_time) {
        final Duration between_now_and_restart_time = Duration.between(ZonedDateTime.now(config.time_zone_id), restart_time);
        if (between_now_and_restart_time.toSeconds() < 1) return Duration.ofSeconds(1);
        return config.notification_times.stream()
                .filter(duration_left_notification -> between_now_and_restart_time.compareTo(duration_left_notification) > 0)
                .max(Comparator.comparingLong(Duration::toNanos))
                .orElse(between_now_and_restart_time);
    }

    @Override
    public void disable() {
        this.pendingRestarts.forEach(ScheduledTask::cancel);
        if (this.activeRestart != null) activeRestart.countdownTask().cancel();
    }

    private void tryInitRestart(Duration init) {
        if (ServerRestartsPaper.isRestarting) {
            disable();
            return;
        }

        PreRestartEvent preRestartEvent = new PreRestartEvent(true);
        if (!preRestartEvent.callEvent()) return;

        final RestartEvent.RestartType restartType = preRestartEvent.getDelayTicks() <= 1L ? RestartEvent.RestartType.SCHEDULED : RestartEvent.RestartType.DELAYED;
        final AtomicReference<Duration> timeLeft = new AtomicReference<>(init);

        this.activeRestart = new RestartTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, countdown -> {
            Duration remaining = timeLeft.get();

            if (remaining.isZero() || remaining.isNegative()) {
                RestartEvent restartEvent = new RestartEvent(
                        false,
                        restartType,
                        ServerRestartsPaper.getConfiguration().restart_method,
                        do_safe_restart,
                        do_safe_restart
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

            if (config.notification_times.stream().anyMatch(notifyTimeLeft -> notifyTimeLeft.equals(remaining))) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    switch (ServerRestartsPaper.getConfiguration().message_mode) {
                        case ACTIONBAR -> player.sendActionBar(ServerRestartsPaper.getLang(player.locale()).time_until_restart(remaining));
                        case BROADCAST -> player.sendMessage(ServerRestartsPaper.getLang(player.locale()).time_until_restart(remaining));
                    }
                }
            }

            timeLeft.set(remaining.minusMillis(500));
        }, preRestartEvent.getDelayTicks(), 10), restartType);
    }
}
