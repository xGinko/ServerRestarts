 package me.xginko.serverrestarts.folia;

 import com.github.benmanes.caffeine.cache.Cache;
 import com.github.benmanes.caffeine.cache.Caffeine;
 import io.papermc.paper.threadedregions.ThreadedRegionizer;
 import io.papermc.paper.threadedregions.TickRegionScheduler;
 import io.papermc.paper.threadedregions.TickRegions;
 import me.xginko.serverrestarts.common.CachedTickReport;
 import org.bukkit.Chunk;
 import org.bukkit.Server;
 import org.bukkit.plugin.java.JavaPlugin;

 import java.time.Duration;
 import java.util.*;
 import java.util.concurrent.CompletableFuture;
 import java.util.concurrent.CopyOnWriteArraySet;

 public final class FoliaTickReportImpl implements CachedTickReport {

    private final JavaPlugin plugin;
    private final Server server;
    private final Cache<Boolean, Double> reportCache;

    public FoliaTickReportImpl(JavaPlugin plugin, Duration cacheTime) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.reportCache = Caffeine.newBuilder().expireAfterWrite(cacheTime).build();
    }

    private Set<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>> getAllRegions() {
        try {
            final Iterator<Chunk> iterator = server.getWorlds().stream()
                    .map(world -> Arrays.stream(world.getLoadedChunks()).findAny())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .iterator();

            if (!iterator.hasNext()) {
                return Collections.emptySet();
            }

            final CompletableFuture<Set<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>>>
                    future = new CompletableFuture<>();
            final Set<ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>>
                    regions = new CopyOnWriteArraySet<>();

            while (true) {
                final Chunk chunk = iterator.next();
                final boolean complete = !iterator.hasNext();
                server.getRegionScheduler().execute(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), () -> {
                    try {
                        final ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData>
                                currentRegion = TickRegionScheduler.getCurrentRegion();
                        if (currentRegion != null) currentRegion.regioniser.computeForAllRegionsUnsynchronised(regions::add);
                    } finally {
                        if (complete) future.complete(regions);
                    }
                });
                if (complete) break;
            }

            return future.get();
        } catch (Throwable t) {
            return Collections.emptySet();
        }
    }

    @Override
    public double getTPS() {
        Double lowestTPS = this.reportCache.getIfPresent(true);
        if (lowestTPS == null) {
            lowestTPS = 20.0;
            for (final ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> region : getAllRegions()) {
                lowestTPS = Math.min(lowestTPS, region.getData().getRegionSchedulingHandle()
                        .getTickReport5s(System.nanoTime())
                        .tpsData()
                        .segmentAll()
                        .average());
            }
            this.reportCache.put(true, lowestTPS);
        }
        return lowestTPS;
    }

    @Override
    public double getMSPT() {
        Double highestMSPT = this.reportCache.getIfPresent(false);
        if (highestMSPT == null) {
            highestMSPT = 0.0;
            for (final ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> region : getAllRegions()) {
                highestMSPT = Math.max(highestMSPT, region.getData().getRegionSchedulingHandle()
                        .getTickReport5s(System.nanoTime())
                        .timePerTickData()
                        .segmentAll()
                        .average() / 1000000); // timerPerTickData is in nanoseconds. We want it in millis though.
            }
            this.reportCache.put(false, highestMSPT);
        }
        return highestMSPT;
    }
}
