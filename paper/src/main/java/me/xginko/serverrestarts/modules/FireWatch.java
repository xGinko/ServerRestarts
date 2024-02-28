package me.xginko.serverrestarts.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.common.CachedTickReport;
import me.xginko.serverrestarts.common.CommonUtil;
import me.xginko.serverrestarts.config.PaperConfigImpl;
import me.xginko.serverrestarts.events.RestartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FireWatch implements ServerRestartModule, Runnable {

    private @Nullable ScheduledTask HEARTBEAT;
    private final @NotNull CachedTickReport tickReports;
    private final @NotNull AtomicLong millis_spent_lagging;
    private final long max_millis_lagging, initial_delay_millis, interval_millis;
    private final double restart_tps, restart_mspt;
    private final boolean do_safe_restart;

    public FireWatch() {
        shouldEnable();
        this.tickReports = ServerRestartsPaper.getTickReports();
        this.millis_spent_lagging = new AtomicLong();
        PaperConfigImpl config = ServerRestartsPaper.getConfiguration();
        config.master().addComment("fire-watch.enable",
                "Reboot the server when lagging for a configurable amount of time.");
        this.do_safe_restart = config.getBoolean("fire-watch.restart-gracefully", true, """
                Will disable joining and kick all players before restarting.\s
                If set to false, will just immediately shutdown/restart.""");
        this.restart_tps = Math.abs(config.getDouble("fire-watch.restart-TPS", 12.5,
                "The TPS (ticks per seconds) at which to start taking measures."));
        this.restart_mspt = Math.abs(config.getDouble("fire-watch.restart-MSPT", 100.0,
                "The MSPT (milliseconds per tick) at which to start taking measures."));
        this.max_millis_lagging = TimeUnit.SECONDS.toMillis(Math.max(1, config.getInt("fire-watch.min-lag-duration", 10,
                "How long in seconds the server needs to be lower than the configured tps to restart.")));
        this.initial_delay_millis = TimeUnit.SECONDS.toMillis(Math.max(1, config.getInt("fire-watch.check-timer.initial-delay-seconds", 30)));
        this.interval_millis = TimeUnit.SECONDS.toMillis(Math.max(1, config.getInt("fire-watch.check-timer.interval-seconds", 3)));
    }

    @Override
    public boolean shouldEnable() {
        return ServerRestartsPaper.getConfiguration().getBoolean("fire-watch.enable", true);
    }

    @Override
    public void enable() {
        ServerRestartsPaper plugin = ServerRestartsPaper.getInstance();
        this.HEARTBEAT = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                BEAT_TASK -> this.run(),
                initial_delay_millis,
                interval_millis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void disable() {
        if (this.HEARTBEAT != null) this.HEARTBEAT.cancel();
    }

    @Override
    public void run() {
        if (ServerRestartsPaper.isRestarting) {
            disable();
            return;
        }

        final double tps = tickReports.getTPS();
        final double mspt = tickReports.getMSPT();

        if (tps > restart_tps && mspt < restart_mspt) {
            millis_spent_lagging.set(0L); // No lag, reset time
            return;
        }

        if (millis_spent_lagging.addAndGet(interval_millis) <= max_millis_lagging) {
            return; // Not lagging for long enough yet
        }

        RestartEvent restartEvent = new RestartEvent(
                true,
                RestartEvent.RestartType.ON_FIRE,
                ServerRestartsPaper.getConfiguration().restart_method,
                do_safe_restart,
                do_safe_restart
        );

        if (!restartEvent.callEvent()) {
            return;
        }

        ServerRestartsPaper.joiningAllowed = false;

        ServerRestartsPaper.getLog().error(Component.text("Restarting server because on fire! -> " +
                        "Lag Duration: " + CommonUtil.formatDuration(Duration.ofMillis(millis_spent_lagging.get())) + " · " +
                        "TPS: " + String.format("%.2f", tps) + " · " +
                        "MSPT: " + String.format("%.2f", mspt))
                .color(TextColor.color(255, 81, 112)).decorate(TextDecoration.BOLD));

        ServerRestartsPaper.restart(
                restartEvent.getType(),
                restartEvent.getMethod(),
                restartEvent.getDisableJoin(),
                restartEvent.getKickAll()
        );
    }
}
