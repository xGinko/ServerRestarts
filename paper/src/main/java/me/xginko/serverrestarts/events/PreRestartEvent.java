package me.xginko.serverrestarts.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreRestartEvent extends Event implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;

    private long delayTicks = 1L;

    public PreRestartEvent(boolean isAsync) {
        super(isAsync);
    }

    public long getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(long delayMillis) {
        this.delayTicks = Math.max(delayMillis, 1L);
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
