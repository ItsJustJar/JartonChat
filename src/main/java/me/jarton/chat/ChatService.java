package me.jarton.chat;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {

    private final JartonChat plugin;

    private final Map<UUID, Deque<Long>> spam5 = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spam15 = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> spam60 = new ConcurrentHashMap<>();
    private volatile boolean globalChatMuted = false;

    public ChatService(JartonChat plugin) {
        this.plugin = plugin;
    }

    public boolean isGlobalChatMuted() {
        return globalChatMuted;
    }

    public void toggleGlobalChat() {
        globalChatMuted = !globalChatMuted;
    }

    public String buildPrefix(Player p) {
        return plugin.getRanksManager().getCombinedDisplay(p);
    }

    public String papi(Player p, String placeholder) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return PlaceholderAPI.setPlaceholders(p, placeholder);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public String getRankNameColor(Player player) {
        return getRankColor(player, "name");
    }

    public String getRankMessageColor(Player player) {
        return getRankColor(player, "message");
    }

    private String getRankColor(Player player, String type) {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection colorsSec = yaml.getConfigurationSection("rank-colors");
        if (colorsSec == null) return "&7";

        Set<String> userGroups = plugin.getRanksManager().getUserGroups(player);

        List<String> orderedKeys = new ArrayList<>(colorsSec.getKeys(false));

        for (int i = orderedKeys.size() - 1; i >= 0; i--) {
            String group = orderedKeys.get(i);
            if (userGroups.contains(group.toLowerCase(Locale.ROOT))) {
                String val = colorsSec.getString(group + "." + type, null);
                if (val != null) {
                    return ColorUtil.color(val);
                }
            }
        }

        return ColorUtil.color(colorsSec.getString("default." + type, "&7"));
    }

    public Player findMentioned(String messageColored) {
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (messageColored.toLowerCase(Locale.ROOT).contains(t.getName().toLowerCase(Locale.ROOT))) {
                return t;
            }
        }
        return null;
    }

    public String highlightMention(String colored, Player target, String continuationColor) {
        String name = target.getName();
        return colored.replaceAll(
                "(?i)" + java.util.regex.Pattern.quote(name),
                "&e" + name + "&r" + continuationColor
        );
    }

    public boolean isSpam(UUID id) {
        long now = System.currentTimeMillis();
        return overLimit(spam5, id, now, 5000, 4)
                || overLimit(spam15, id, now, 15000, 10)
                || overLimit(spam60, id, now, 60000, 15);
    }

    private boolean overLimit(Map<UUID, Deque<Long>> map, UUID id, long now, long windowMs, int max) {
        Deque<Long> q = map.computeIfAbsent(id, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() >= windowMs) q.pollFirst();
        q.addLast(now);
        return q.size() >= max;
    }

    public long getMuteRemaining(UUID id) {
        return plugin.getMuteStorage().remainingMillis(id);
    }

    public void mute(UUID id, long durationMillis) {
        plugin.getMuteStorage().mute(id, durationMillis);
    }

    public void unmute(UUID id) {
        plugin.getMuteStorage().unmute(id);
    }

    public String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long m = s / 60; s %= 60;
        long h = m / 60; m %= 60;
        long d = h / 24; h %= 24;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public String censor(Player p, String message) {
        if (p.hasPermission("chat.bypass_censor")) return message;

        int swears = 0;
        String lower = message.toLowerCase(Locale.ROOT);

        for (String banned : plugin.bannedWords) {
            if (banned.isBlank()) continue;
            if (lower.contains(banned)) {
                swears++;
                message = message.replaceAll("(?i)" + java.util.regex.Pattern.quote(banned), "***");
            }
        }

        if (swears >= 1) {
            if (!plugin.mildConcern.isEmpty() && !p.hasPermission("chat.bypass_censor_punishment")) {
                String concern = plugin.mildConcern.get(new Random().nextInt(plugin.mildConcern.size()));
                p.sendMessage(ColorUtil.color("&c " + concern));
            }

            String respectMsg = plugin.messageMap.get("respect");
            if (respectMsg != null && !respectMsg.isBlank()) {
                p.sendMessage(ColorUtil.color("&c " + respectMsg));
            }
        }
        return message;
    }

    public String replaceEmojis(Player p, String message, String messageColor) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        if (plugin.emojiMap.isEmpty()) {
            return message;
        }

        if (messageColor == null) {
            messageColor = "";
        }

        String result = message;
        for (Map.Entry<String, String> entry : plugin.emojiMap.entrySet()) {
            String trigger = entry.getKey();
            String replacement = entry.getValue();
            if (trigger == null || trigger.isEmpty()) continue;
            if (replacement == null) replacement = "";

            replacement = replacement.replaceAll("(?i)&[0-9A-FK-OR]", "");

            String wrapped = "&r" + replacement + messageColor;
            result = result.replace(trigger, wrapped);
        }
        return result;
    }

    public void broadcastDiscordMessage(String formatted) {
        String colored = ColorUtil.color(formatted);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(colored);
        }
    }

    public boolean canMessage(Player s, Player t) {
        // Jar you better figure out what you want here because you called this method and never even wrote it
        return true;
    }
}