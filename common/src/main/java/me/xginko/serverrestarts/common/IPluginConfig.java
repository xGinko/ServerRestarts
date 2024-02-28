package me.xginko.serverrestarts.common;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.util.List;

public interface IPluginConfig {

    @NotNull ZonedDateTime getRestartTime(int hours, int minutes, int seconds) throws DateTimeException;
    void saveConfig();
    @NotNull ConfigFile master();
    void createTitledSection(String title, @NotNull String path);
    boolean getBoolean(@NotNull String path, boolean def, @NotNull String comment);
    boolean getBoolean(@NotNull String path, boolean def);
    int getInt(@NotNull String path, int def, @NotNull String comment);
    int getInt(@NotNull String path, int def);
    double getDouble(@NotNull String path, double def, @NotNull String comment);
    double getDouble(@NotNull String path, double def);
    @NotNull String getString(@NotNull String path, @NotNull String def, @NotNull String comment);
    @NotNull String getString(@NotNull String path, @NotNull String def);
    @NotNull List<String> getList(@NotNull String path, List<String> def, @NotNull String comment);
    @NotNull List<String> getList(@NotNull String path, List<String> def);

}
