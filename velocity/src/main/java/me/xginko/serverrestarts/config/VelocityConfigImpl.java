package me.xginko.serverrestarts.config;


import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.serverrestarts.ServerRestartsVelocity;
import me.xginko.serverrestarts.common.IPluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VelocityConfigImpl implements IPluginConfig {

    private final @NotNull ConfigFile configFile;
    public final @NotNull List<ZonedDateTime> restart_times;
    public final @NotNull ZoneId time_zone_id;
    public final @NotNull Component server_restarting;

    public VelocityConfigImpl(File parentDirectory) throws Exception {
        // Create plugin folder first if it does not exist yet
        if (!parentDirectory.exists() && !parentDirectory.mkdir())
            ServerRestartsVelocity.getLogger().error("Failed to create plugin folder.");
        // Load config.yml with ConfigMaster
        this.configFile = ConfigFile.loadConfig(new File(parentDirectory, "config.yml"));

        // General Settings
        this.createTitledSection("General Settings", "general");
        ZoneId zoneId = ZoneId.systemDefault();
        try {
            zoneId = ZoneId.of(getString("general.timezone", zoneId.getId(),
                    "The TimeZone (ZoneId) to use for scheduling restart times."));
        } catch (ZoneRulesException e) {
            ServerRestartsVelocity.getLogger().warn("Configured timezone could not be found. Using host zone '"+zoneId+"' (System Default)");
        } catch (DateTimeException e) {
            ServerRestartsVelocity.getLogger().warn("Configured timezone has an invalid format. Using '"+zoneId+"' (System Default)");
        }
        this.time_zone_id = zoneId;
        this.server_restarting = MiniMessage.miniMessage().deserialize(getString("general.restart-message", "<gold>Proxy restarting."));

        // Restart times
        this.createTitledSection("Restart Times", "restart-times");
        this.restart_times = getList("restart-times", List.of("02:00:00", "03:00:00", "17:30:00"), """
                Make sure you are enclosing the time values within single quotes (Ex. - '17:30:00').\s
                Without the quotes, the YAML parser might interpret the time values as numeric literals, leading to errors.\s
                The YAML parser will remove enclosing quotes where it does not deem necessary. Those can be safely ignored.""")
                .stream()
                .distinct()
                .map(timeString -> {
                    try {
                        final int hour = Integer.parseInt(timeString.substring(0, 2), 10);
                        final int minute = Integer.parseInt(timeString.substring(3, 5), 10);
                        final int second = Integer.parseInt(timeString.substring(6, 8), 10);
                        return this.getRestartTime(hour, minute, second);
                    } catch (Throwable t) {
                        ServerRestartsVelocity.getLogger().warn("Restart time '"+timeString+"' is not formatted properly. " +
                                "Format: 23:59:59 -> hour:minute:second");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(restartTime -> Duration.between(ZonedDateTime.now(time_zone_id), restartTime).toNanos()))
                .collect(Collectors.toList());
        if (this.restart_times.isEmpty()) {
            final ZonedDateTime systime_02_30_AM = this.getRestartTime(2,30,0);
            this.restart_times.add(systime_02_30_AM);
            ServerRestartsVelocity.getLogger().warn("Queued 1 restart for " + systime_02_30_AM + " due to restart times being invalid or empty.");
        }
    }

    @Override
    public @NotNull ZonedDateTime getRestartTime(int hours, int minutes, int seconds) throws DateTimeException {
        ZonedDateTime now = ZonedDateTime.now(time_zone_id);
        ZonedDateTime nextRestart = now.withHour(hours).withMinute(minutes).withSecond(seconds);
        return nextRestart.isAfter(now) ? nextRestart : nextRestart.plusDays(1);
    }

    @Override
    public void saveConfig() {
        try {
            this.configFile.save();
        } catch (Exception e) {
            ServerRestartsVelocity.getLogger().error("Failed to save config file! - " + e.getLocalizedMessage());
        }
    }

    @Override
    public @NotNull ConfigFile master() {
        return configFile;
    }

    @Override
    public void createTitledSection(String title, @NotNull String path) {
        this.configFile.addSection(title);
        this.configFile.addDefault(path, null);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getBoolean(path, def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getBoolean(path, def);
    }

    @Override
    public int getInt(@NotNull String path, int def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getInteger(path, def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getInteger(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getDouble(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getDouble(path, def);
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getString(path, def);
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getString(path, def);
    }

    @Override
    public @NotNull List<String> getList(@NotNull String path, List<String> def, @NotNull String comment) {
        this.configFile.addDefault(path, def, comment);
        return this.configFile.getStringList(path);
    }

    @Override
    public @NotNull List<String> getList(@NotNull String path, List<String> def) {
        this.configFile.addDefault(path, def);
        return this.configFile.getStringList(path);
    }
}
