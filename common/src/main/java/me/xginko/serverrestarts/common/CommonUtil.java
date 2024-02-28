package me.xginko.serverrestarts.common;

import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class CommonUtil {
    public static final TextColor COLOR = TextColor.color(2,121,107);
    public static final Style BOLD_COLOR = Style.style(COLOR, TextDecoration.BOLD);

    public static @NotNull String formatDuration(@NotNull Duration duration) {
        if (duration.isNegative()) duration = duration.negated();

        final int seconds = duration.toSecondsPart();
        final int minutes = duration.toMinutesPart();
        final int hours = duration.toHoursPart();

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }
}
