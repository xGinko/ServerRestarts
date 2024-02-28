package me.xginko.serverrestarts.config;


import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.enums.MessageMode;
import me.xginko.serverrestarts.enums.RestartMethod;
import me.xginko.serverrestarts.common.IPluginConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Collectors;

public class PaperConfigImpl implements IPluginConfig {

    private final @NotNull ConfigFile configFile;
    public final @NotNull List<ZonedDateTime> restart_times;
    public final @NotNull List<Duration> notification_times;
    public final @NotNull Duration tick_report_cache_time;
    public final @NotNull ZoneId time_zone_id;
    public final @NotNull Locale default_lang;
    public final @NotNull MessageMode message_mode;
    public final @NotNull RestartMethod restart_method;
    public final boolean auto_lang;

    public PaperConfigImpl(File parentDirectory) throws Exception {
        // Create plugin folder first if it does not exist yet
        if (!parentDirectory.exists() && !parentDirectory.mkdir())
            ServerRestartsPaper.getLog().error("Failed to create plugin folder.");
        // Load config.yml with ConfigMaster
        this.configFile = ConfigFile.loadConfig(new File(parentDirectory, "config.yml"));

        // Generate config titles, structuring it
        this.structure();

        // Language Settings
        this.default_lang = Locale.forLanguageTag(getString("language.default-language", "en_us",
                "The default language that will be used if auto-language is false or no matching language file was found.")
                .replace("_", "-"));
        this.auto_lang = getBoolean("language.auto-language", true,
                "If set to true, will display messages based on client language");

        // General Settings
        this.tick_report_cache_time = Duration.ofMillis(Math.max(getInt("general.tps-cache-time-ticks", 40,
                "How long a checked tps is cached to save resources in ticks (1 sec = 20 ticks)"), 20) * 50L);
        RestartMethod method = RestartMethod.BUKKIT_SHUTDOWN;
        String configuredMethod = getString("general.restart-method", RestartMethod.BUKKIT_SHUTDOWN.name(),
                "Available options are: " + String.join(", ", Arrays.stream(RestartMethod.values()).map(Enum::name).toList()));
        try {
            method = RestartMethod.valueOf(configuredMethod);
        } catch (IllegalArgumentException e) {
            ServerRestartsPaper.getLog().warn("RestartMethod '"+configuredMethod+"' is not a valid method. Valid methods are as follows: ");
            ServerRestartsPaper.getLog().warn(String.join(", ", Arrays.stream(RestartMethod.values()).map(Enum::name).toList()));
        }
        this.restart_method = method;
        ZoneId zoneId = ZoneId.systemDefault();
        try {
            zoneId = ZoneId.of(getString("general.timezone", zoneId.getId(),
                    "The TimeZone (ZoneId) to use for scheduling restart times."));
        } catch (ZoneRulesException e) {
            ServerRestartsPaper.getLog().warn("Configured timezone could not be found. Using host zone '"+zoneId+"' (System Default)");
        } catch (DateTimeException e) {
            ServerRestartsPaper.getLog().warn("Configured timezone has an invalid format. Using '"+zoneId+"' (System Default)");
        }
        this.time_zone_id = zoneId;

        // Restart times
        this.restart_times = getList("restart-times", List.of("02:00:00", "03:00:00", "17:30:00"), """
                Make sure you are enclosing the time values within single quotes (Ex. - '17:30:00').\s
                Without the quotes, the YAML parser might interpret the time values as numeric literals, leading to errors.\s
                The YAML parser will remove enclosing quotes where it does not deem necessary. Those can be safely ignored.""")
                .stream()
                .distinct()
                .map(timeString -> {
                    try {
                        final String[] numbers = timeString.split(":");
                        return this.getRestartTime(
                                Integer.parseInt(numbers[0].trim(), 10),
                                Integer.parseInt(numbers[1].trim(), 10),
                                Integer.parseInt(numbers[2].trim(), 10));
                    } catch (Throwable t) {
                        ServerRestartsPaper.getLog().warn("Restart time '"+timeString+"' is not formatted properly. " +
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
            ServerRestartsPaper.getLog().warn("Queued 1 restart for " + systime_02_30_AM + " due to restart times being invalid or empty.");
        }

        // Notifications
        MessageMode messageMode = MessageMode.ACTIONBAR;
        final String configuredMode = getString("notifications.notify-mode", messageMode.name(),
                "Available options are: " + String.join(", ", Arrays.stream(MessageMode.values()).map(Enum::name).toList()));
        try {
            messageMode = MessageMode.valueOf(configuredMode);
        } catch (IllegalArgumentException e) {
            ServerRestartsPaper.getLog().warn("MessageMode '"+configuredMode+"' is not a valid mode. Valid modes are as follows: ");
            ServerRestartsPaper.getLog().warn(String.join(", ", Arrays.stream(MessageMode.values()).map(Enum::name).toList()));
        }
        this.message_mode = messageMode;
        this.notification_times = getList("notifications.notify-times", List.of(
                        "PT15M", "PT10M", "PT5M", "PT4M", "PT3M", "PT2M", "PT1M", "PT30S", "PT15S",
                        "PT10S", "PT9S", "PT8S", "PT7S", "PT6S", "PT5S", "PT4S", "PT3S", "PT2S", "PT1S"))
                .stream()
                .distinct()
                .map(text -> {
                    try {
                        return Duration.parse(text);
                    } catch (DateTimeParseException e) {
                        ServerRestartsPaper.getLog().warn("Unable to parse Duration '"+text+"'. Is it formatted correctly?");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Collections.reverseOrder(Comparator.comparingLong(Duration::toNanos)))
                .toList();
        configFile.addComment("notifications.notify-times", """
                At which time remaining should we send a notification to all online players.\s
                \s
                EXAMPLES:\s
                   "PT20.345S" -- parses as "20.345 seconds"\s
                   "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)\s
                   "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)\s
                   "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)\s
                   "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"\s
                   "PT-6H3M"    -- parses as "-6 hours and +3 minutes"\s
                   "-PT6H3M"    -- parses as "-6 hours and -3 minutes"\s
                   "-PT-6H+3M"  -- parses as "+6 hours and -3 minutes"
                """);
    }

    @Override
    public @NotNull ZonedDateTime getRestartTime(int hours, int minutes, int seconds) throws DateTimeException {
        ZonedDateTime now = ZonedDateTime.now(time_zone_id);
        ZonedDateTime nextRestart = now.withHour(hours).withMinute(minutes).withSecond(seconds);
        if (now.isAfter(nextRestart) || now.isEqual(nextRestart))
            return nextRestart.plusDays(1);
        return nextRestart;
    }

    private void structure() {
        createTitledSection("Language Settings", "language");
        createTitledSection("General Settings", "general");
        createTitledSection("Restart Times", "restart-times");
        createTitledSection("Notifications", "notifications");
        createTitledSection("Fire Watch", "fire-watch");
        createTitledSection("Playercount Delay", "player-count-delay");
    }

    @Override
    public void saveConfig() {
        try {
            this.configFile.save();
        } catch (Exception e) {
            ServerRestartsPaper.getLog().error("Failed to save config file! - " + e.getLocalizedMessage());
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
