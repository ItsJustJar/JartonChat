package me.jarton.chat;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private ColorUtil() {}

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    public static String color(String s) {
        if (s == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(s);
        while (matcher.find()) {
            String hex = matcher.group();
            try {
                s = s.replace(hex, ChatColor.of(hex).toString());
            } catch (IllegalArgumentException ignored) {}
        }

        return ChatColor.translateAlternateColorCodes('&', s);
    }
}