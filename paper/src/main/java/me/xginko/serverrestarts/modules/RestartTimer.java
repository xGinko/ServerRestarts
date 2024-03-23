package me.xginko.serverrestarts.modules;

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RestartTimer implements ServerRestartModule {
    private record RestartTask(ScheduledFuture<?> countdownTask, RestartEvent.RestartType restartType) {}
    private final @NotNull ServerRestartsPaper plugin;
    private final @NotNull ScheduledExecutorService executorService;
    private final @NotNull Set<ScheduledFuture<?>> pendingRestarts;
    private final @NotNull PaperConfigImpl config;
    private @Nullable RestartTask activeRestart;
    private final boolean do_safe_restart;

    public RestartTimer() {
        shouldEnable();
        this.plugin = ServerRestartsPaper.getInstance();
        this.config = ServerRestartsPaper.getConfiguration();
        this.pendingRestarts = new HashSet<>(config.restart_times.size());
        this.executorService = Executors.newScheduledThreadPool(config.restart_times.size());
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
            final Duration time_left_until_restart = getDelay(restart_time);
            this.pendingRestarts.add(executorService.schedule(
                    () -> new Thread(() -> tryInitRestart(time_left_until_restart)).start(),
                    time_left_until_restart.toMillis(),
                    TimeUnit.MILLISECONDS
            ));
        }
    }

    /*
    * Returns a sensible delay for when to initiate the restart countdown logic,
    * so we restart precisely at the configured time.
    * */
    private Duration getDelay(ZonedDateTime restart_time) {
        final Duration between_now_and_restart_time = Duration.between(ZonedDateTime.now(config.time_zone_id), restart_time);
        if (between_now_and_restart_time.toSeconds() < 1) {
            return Duration.ofSeconds(1);
        }
        // If there are no countdown durations or duration is greater than the largest configured countdown duration, no need to adjust.
        if (config.notification_times.isEmpty() || between_now_and_restart_time.compareTo(config.notification_times.get(0)) > 0) {
            return between_now_and_restart_time;
        }
        // If not, return closest restart duration compared to the time left
        return config.notification_times.stream()
                .filter(duration_left_notification -> between_now_and_restart_time.compareTo(duration_left_notification) > 0)
                .max(Comparator.comparingLong(Duration::toNanos))
                .orElse(between_now_and_restart_time);
    }

    @Override
    public void disable() {
        this.pendingRestarts.forEach(restartTask -> restartTask.cancel(true));
        if (this.activeRestart != null)
            activeRestart.countdownTask().cancel(true);
        this.executorService.shutdownNow();
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

        this.activeRestart = new RestartTask(executorService.scheduleAtFixedRate(() -> {
            assert activeRestart != null;
            if (activeRestart.countdownTask.isCancelled()) {
                return;
            }

            Duration remaining = timeLeft.get();

            if (remaining.isZero() || remaining.isNegative()) {
                RestartEvent restartEvent = new RestartEvent(
                        true,
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

                activeRestart.countdownTask.cancel(true);

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
        }, preRestartEvent.getDelayTicks() * 50L, 500L, TimeUnit.MILLISECONDS), restartType);
    }
}
