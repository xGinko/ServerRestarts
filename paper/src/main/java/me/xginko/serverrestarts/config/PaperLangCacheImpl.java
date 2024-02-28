package me.xginko.serverrestarts.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.serverrestarts.ServerRestartsPaper;
import me.xginko.serverrestarts.common.CommonUtil;
import me.xginko.serverrestarts.common.ILangCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;
import java.util.List;

public class PaperLangCacheImpl implements ILangCache {

    private final @NotNull ConfigFile langFile;
    public final @NotNull Component no_permission, server_restarting, server_restarting_on_fire,
            restart_delayed_playercount, countdown_now, restart_in;

    public PaperLangCacheImpl(String locale) throws Exception {
        ServerRestartsPaper plugin = ServerRestartsPaper.getInstance();
        File langYML = new File(plugin.getDataFolder() + File.separator + "lang", locale + ".yml");
        // Check if the lang folder has already been created
        File parent = langYML.getParentFile();
        if (!parent.exists() && !parent.mkdir())
            ServerRestartsPaper.getLog().error("Unable to create lang directory.");
        // Check if the file already exists and save the one from the plugin resources folder if it does not
        if (!langYML.exists())
            plugin.saveResource("lang/" + locale + ".yml", false);
        // Finally, load the lang file with configmaster
        this.langFile = ConfigFile.loadConfig(langYML);

        this.no_permission = getTranslation("messages.no-permission",
                "<red>You don't have permission to use this command.");
        this.server_restarting = getTranslation("messages.server-restarting",
                "<gold>Server is restarting and will be back in a few minutes.");
        this.server_restarting_on_fire = getTranslation("messages.server-restarting",
                "<gold>Server is restarting and will be back in a few minutes.");
        this.restart_delayed_playercount = getTranslation("messages.restart-delayed-high-playercount",
                "<gray>Delaying restart for %time% due to high playercount.");

        this.restart_in = getTranslation("countdown.timed", "<gold>Restarting in %time% ...");
        this.countdown_now = getTranslation("countdown.now", "<bold><red>Restarting now");

        try {
            this.langFile.save();
        } catch (Exception e) {
            ServerRestartsPaper.getLog().error("Failed to save language file: "+ langYML.getName() +" - " + e.getLocalizedMessage());
        }
    }

    public @NotNull Component time_until_restart(final Duration remainingTime) {
        return this.restart_in.replaceText(TextReplacementConfig.builder()
                .match("%time%")
                .replacement(CommonUtil.formatDuration(remainingTime))
                .build());
    }

    @Override
    public @NotNull Component getTranslation(String path, String defaultTranslation) {
        this.langFile.addDefault(path, defaultTranslation);
        return MiniMessage.miniMessage().deserialize(this.langFile.getString(path, defaultTranslation));
    }

    @Override
    public @NotNull Component getTranslation(String path, String defaultTranslation, String comment) {
        this.langFile.addDefault(path, defaultTranslation, comment);
        return MiniMessage.miniMessage().deserialize(this.langFile.getString(path, defaultTranslation));
    }

    @Override
    public @NotNull List<Component> getListTranslation(String path, List<String> defaultTranslation) {
        this.langFile.addDefault(path, defaultTranslation);
        return this.langFile.getStringList(path).stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }

    @Override
    public @NotNull List<Component> getListTranslation(String path, List<String> defaultTranslation, String comment) {
        this.langFile.addDefault(path, defaultTranslation, comment);
        return this.langFile.getStringList(path).stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }
}