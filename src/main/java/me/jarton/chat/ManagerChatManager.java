package me.jarton.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ManagerChatManager {

    private static final String ACCESS_DENIED_MESSAGE = ChatColor.RED + "You don't have permission to use ManagerChat.";
    private static final Set<String> ALLOWED_MANAGER_USERNAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "simplyjar",
            "vaise",
            "jahwicked",
            "spikedyoge",
            "chxnce_xx"
    )));

    private final JartonChat plugin;
    private final Map<UUID, Boolean> managerToggle = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> soundToggle = new ConcurrentHashMap<>();

    private File soundFile;
    private YamlConfiguration soundConfig;

    private boolean prefixEnabled;
    private String prefixSymbol;

    private boolean soundMessagesEnabled;
    private String soundMessagesName;
    private float soundMessagesVolume;
    private float soundMessagesPitch;

    private boolean soundNotifyEnabled;
    private String soundNotifyName;
    private float soundNotifyVolume;
    private float soundNotifyPitch;

    private String formatIngame;

    public ManagerChatManager(JartonChat plugin) {
        this.plugin = plugin;
        loadConfig();
        loadSoundToggles();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "managerchat.yml");
        if (!file.exists()) {
            plugin.saveResource("managerchat.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        prefixEnabled = cfg.getBoolean("manager-chat.prefix.enabled", true);
        prefixSymbol = cfg.getString("manager-chat.prefix.symbol", "@");

        soundMessagesEnabled = cfg.getBoolean("sounds.messages.enabled", true);
        soundMessagesName = cfg.getString("sounds.messages.name", "ENTITY.ITEM.PICKUP");
        soundMessagesVolume = (float) cfg.getDouble("sounds.messages.volume", 1.0);
        soundMessagesPitch = (float) cfg.getDouble("sounds.messages.pitch", 0.6);

        soundNotifyEnabled = cfg.getBoolean("sounds.notifications.enabled", true);
        soundNotifyName = cfg.getString("sounds.notifications.name", "ENTITY.ITEM.PICKUP");
        soundNotifyVolume = (float) cfg.getDouble("sounds.notifications.volume", 1.0);
        soundNotifyPitch = (float) cfg.getDouble("sounds.notifications.pitch", 0.8);

        formatIngame = cfg.getString("formatting.ingame", "&8[&eManagerChat&8] &7%player%: &f%message%");
        if (formatIngame == null || formatIngame.isBlank()) {
            formatIngame = "&8[&eManagerChat&8] &7%player%: &f%message%";
        }
        if (!formatIngame.contains("%player%")) {
            formatIngame = "%player%: " + formatIngame;
        }
        if (!formatIngame.contains("%message%")) {
            formatIngame = formatIngame + " &f%message%";
        }
    }

    private void loadSoundToggles() {
        soundFile = new File(plugin.getDataFolder(), "managerchat-toggles.yml");
        if (!soundFile.exists()) {
            try {
                soundFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        soundConfig = YamlConfiguration.loadConfiguration(soundFile);
        for (String key : soundConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                boolean enabled = soundConfig.getBoolean(key, true);
                soundToggle.put(uuid, enabled);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveSoundToggles() {
        for (Map.Entry<UUID, Boolean> entry : soundToggle.entrySet()) {
            soundConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            soundConfig.save(soundFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isPrefixEnabled() {
        return prefixEnabled;
    }

    public String getPrefixSymbol() {
        return prefixSymbol;
    }

    public boolean canUseManagerChat(Player player) {
        return player != null && canUseManagerChat(player.getName());
    }

    public boolean canUseManagerChat(String username) {
        if (username == null) return false;
        return ALLOWED_MANAGER_USERNAMES.contains(username.toLowerCase(Locale.ROOT));
    }

    public void setManagerChatEnabled(Player player, boolean enabled) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (enabled) managerToggle.put(uuid, true);
        else managerToggle.remove(uuid);
    }

    public void toggleManagerChat(Player player) {
        if (!canUseManagerChat(player)) {
            if (player != null) player.sendMessage(ACCESS_DENIED_MESSAGE);
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean newState = !managerToggle.getOrDefault(uuid, false);
        setManagerChatEnabled(player, newState);
        if (newState) {
            if (plugin.getStaffChatManager() != null) plugin.getStaffChatManager().setStaffChatEnabled(player, false);
            if (plugin.getAdminChatManager() != null) plugin.getAdminChatManager().setAdminChatEnabled(player, false);
        }
        player.sendMessage(newState ? "§aYou are now talking in ManagerChat." : "§cYou have left ManagerChat.");
    }

    public boolean isInManagerChat(Player player) {
        return managerToggle.getOrDefault(player.getUniqueId(), false);
    }

    public void toggleSound(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !soundToggle.getOrDefault(uuid, true);
        soundToggle.put(uuid, newState);
        saveSoundToggles();
        player.sendMessage(newState ? "§aManagerChat sounds enabled." : "§cManagerChat sounds disabled.");
    }

    public boolean isSoundEnabled(Player player) {
        return soundToggle.getOrDefault(player.getUniqueId(), true);
    }

    public void sendManagerMessage(Player sender, String message) {
        if (sender == null || message == null) return;
        if (!canUseManagerChat(sender)) {
            sender.sendMessage(ACCESS_DENIED_MESSAGE);
            setManagerChatEnabled(sender, false);
            return;
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) return;

        String displayName = sender.getDisplayName();
        String formatted = formatIngame
                .replace("%player%", displayName)
                .replace("%message%", trimmedMessage);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canUseManagerChat(p)) continue;
            if (plugin.getHushManager() != null && plugin.getHushManager().isHushed(p, HushManager.ChatType.MANAGER)) continue;

            p.sendMessage(ColorUtil.color(formatted));
            if (soundMessagesEnabled && isSoundEnabled(p)) {
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(soundMessagesName.toUpperCase()), soundMessagesVolume, soundMessagesPitch);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        try {
            String plainName = ChatColor.stripColor(ColorUtil.color(displayName));
            String plainMessage = ChatColor.stripColor(ColorUtil.color(trimmedMessage)).trim();
            if (plugin.getDiscordBridge() != null) {
                plugin.getDiscordBridge().sendManagerChatWebhook(plainName, plainMessage, sender);
            }
        } catch (Throwable ignored) {
        }
    }

    public void sendManagerMessageFromDiscord(String formatted, String rawMessage) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!canUseManagerChat(p)) continue;
            if (plugin.getHushManager() != null && plugin.getHushManager().isHushed(p, HushManager.ChatType.MANAGER)) continue;

            p.sendMessage(ColorUtil.color(formatted));
            if (soundNotifyEnabled && isSoundEnabled(p)) {
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(soundNotifyName.toUpperCase()), soundNotifyVolume, soundNotifyPitch);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
