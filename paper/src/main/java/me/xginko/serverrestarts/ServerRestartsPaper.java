package me.xginko.serverrestarts;

import me.xginko.serverrestarts.commands.RestartGracefullyCmd;
import me.xginko.serverrestarts.commands.RestartsCmd;
import me.xginko.serverrestarts.common.CachedTickReport;
import me.xginko.serverrestarts.common.CommonUtil;
import me.xginko.serverrestarts.config.PaperConfigImpl;
import me.xginko.serverrestarts.config.PaperLangCacheImpl;
import me.xginko.serverrestarts.enums.RestartMethod;
import me.xginko.serverrestarts.events.RestartEvent;
import me.xginko.serverrestarts.folia.FoliaTickReportImpl;
import me.xginko.serverrestarts.listener.JoinToggle;
import me.xginko.serverrestarts.modules.ServerRestartModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class ServerRestartsPaper extends JavaPlugin {

    private static ServerRestartsPaper instance;
    private static PaperConfigImpl config;
    private static Map<String, PaperLangCacheImpl> languageCacheMap;
    private static Server server;
    private static CachedTickReport cachedTickReport;
    private static ComponentLogger logger;
    private static Metrics metrics;
    public static boolean isFolia, isRestarting, joiningAllowed;

    @Override
    public void onEnable() {
        isFolia = isRestarting = false;
        joiningAllowed = true;
        instance = this;
        logger = getComponentLogger();
        server = getServer();
        metrics = new Metrics(this, 21162);
        server.getPluginManager().registerEvents(new JoinToggle(), this);
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            logger.info("Detected Folia");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {}

        logger.info(Component.text("                                                          ").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("  ___                      ___        _            _      ").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" / __| ___ _ ___ _____ _ _| _ \\___ __| |_ __ _ _ _| |_ ___").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" \\__ \\/ -_) '_\\ V / -_) '_|   / -_|_-<  _/ _` | '_|  _(_-<").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text(" |___/\\___|_|  \\_/\\___|_| |_|_\\___/__/\\__\\__,_|_|  \\__/__/").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("                                                 by xGinko").style(CommonUtil.BOLD_COLOR));
        logger.info(Component.text("                                                          ").style(CommonUtil.BOLD_COLOR));

        reloadLang();
        reloadConfiguration();

        getCommand("serverrestarts").setExecutor(new RestartsCmd());
        getCommand("restartgracefully").setExecutor(new RestartGracefullyCmd());
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
    }

    public static void restart(RestartEvent.RestartType type, RestartMethod method, boolean disableJoining, boolean kickAll) {
        isRestarting = true;

        if (disableJoining) {
            joiningAllowed = false;
        }

        ServerRestartModule.modules.forEach(ServerRestartModule::disable);

        if (kickAll) {
            for (Player player : server.getOnlinePlayers()) {
                player.getScheduler().execute(instance, () -> player.kick(
                        switch (type) {
                            case ON_FIRE -> ServerRestartsPaper.getLang(player.locale()).server_restarting_on_fire;
                            case SCHEDULED, DELAYED, OTHER, COMMAND -> ServerRestartsPaper.getLang(player.locale()).server_restarting;
                        },
                        PlayerKickEvent.Cause.RESTART_COMMAND
                ), null, 1L);
            }
        }

        server.getGlobalRegionScheduler().runDelayed(instance, shutdown -> {
            switch (method) {
                case SPIGOT_RESTART -> server.spigot().restart();
                case BUKKIT_SHUTDOWN -> server.shutdown();
            }
        }, 10L);
    }

    public static ServerRestartsPaper getInstance() {
        return instance;
    }

    public static PaperConfigImpl getConfiguration() {
        return config;
    }

    public static CachedTickReport getTickReports() {
        return cachedTickReport;
    }

    public static ComponentLogger getLog() {
        return logger;
    }

    public static PaperLangCacheImpl getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }

    public static PaperLangCacheImpl getLang(CommandSender commandSender) {
        return commandSender instanceof Player player ? getLang(player.locale()) : getLang(config.default_lang);
    }

    public static PaperLangCacheImpl getLang(String lang) {
        if (!config.auto_lang) return languageCacheMap.get(config.default_lang.toString().toLowerCase());
        return languageCacheMap.getOrDefault(lang.replace("-", "_"), languageCacheMap.get(config.default_lang.toString().toLowerCase()));
    }

    public void reloadPlugin() {
        reloadLang();
        reloadConfiguration();
    }

    public void disablePlugin() {
        ServerRestartModule.modules.forEach(ServerRestartModule::disable);
        ServerRestartModule.modules.clear();
    }

    private void reloadConfiguration() {
        try {
            config = new PaperConfigImpl(this.getDataFolder());
            if (isFolia) cachedTickReport = new FoliaTickReportImpl(this, config.tick_report_cache_time);
            else cachedTickReport = new PaperTickReportImpl(this, config.tick_report_cache_time);
            ServerRestartModule.reloadModules();
            config.saveConfig();
        } catch (Exception e) {
            logger.error("Error loading config!", e);
        }
    }

    private void reloadLang() {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder() + File.separator + "lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                final String localeString = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
                logger.info("Found language file for " + localeString);
                languageCacheMap.put(localeString, new PaperLangCacheImpl(localeString));
            }
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                final Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        logger.info("Found language file for "+ localeString);
                        languageCacheMap.put(localeString, new PaperLangCacheImpl(localeString));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error loading language files!", e);
        }
    }

    private Set<String> getDefaultLanguageFiles() {
        try (final JarFile pluginJarFile = new JarFile(this.getFile())) {
            return pluginJarFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("lang/") && name.endsWith(".yml"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Failed getting default language files!", e);
            return Collections.emptySet();
        }
    }
}
