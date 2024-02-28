package me.xginko.serverrestarts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.serverrestarts.common.CachedTickReport;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class PaperTickReportImpl implements CachedTickReport {

    private final Server server;
    private final Cache<Boolean, Double> reportCache;

    public PaperTickReportImpl(JavaPlugin plugin, Duration cacheTime) {
        this.server = plugin.getServer();
        this.reportCache = Caffeine.newBuilder().expireAfterWrite(cacheTime).build();
    }

    @Override
    public double getTPS() {
        Double tps = this.reportCache.getIfPresent(true);
        if (tps == null) {
            tps = this.server.getTPS()[0];
            this.reportCache.put(true, tps);
        }
        return tps;
    }

    @Override
    public double getMSPT() {
        Double avgTickTime = this.reportCache.getIfPresent(false);
        if (avgTickTime == null) {
            avgTickTime = this.server.getAverageTickTime();
            this.reportCache.put(false, avgTickTime);
        }
        return avgTickTime;
    }
}
