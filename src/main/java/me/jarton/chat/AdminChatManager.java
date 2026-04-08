package me.jarton.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminChatManager {

    private final JartonChat plugin;
    private final DiscordBridge discordBridge;

    private final Map<UUID, Boolean> adminToggle = new ConcurrentHashMap<>();
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

    public AdminChatManager(JartonChat plugin, DiscordBridge discordBridge) {
        this.plugin = plugin;
        this.discordBridge = discordBridge;
        loadConfig();
        loadSoundToggles();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "adminchat.yml");
        if (!file.exists()) plugin.saveResource("adminchat.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        prefixEnabled = cfg.getBoolean("admin-chat.prefix.enabled", true);
        prefixSymbol = cfg.getString("admin-chat.prefix.symbol", "!");

        soundMessagesEnabled = cfg.getBoolean("sounds.messages.enabled", true);
        soundMessagesName = cfg.getString("sounds.messages.name", "ENTITY.ITEM.PICKUP");
        soundMessagesVolume = (float) cfg.getDouble("sounds.messages.volume", 1.0);
        soundMessagesPitch = (float) cfg.getDouble("sounds.messages.pitch", 0.6);

        soundNotifyEnabled = cfg.getBoolean("sounds.notifications.enabled", true);
        soundNotifyName = cfg.getString("sounds.notifications.name", "ENTITY.ITEM.PICKUP");
        soundNotifyVolume = (float) cfg.getDouble("sounds.notifications.volume", 1.0);
        soundNotifyPitch = (float) cfg.getDouble("sounds.notifications.pitch", 0.8);

        formatIngame = cfg.getString("formatting.ingame", "&8[&6AdminChat&8] &7%player%: &f%message%");
    }

    private void loadSoundToggles() {
        soundFile = new File(plugin.getDataFolder(), "adminchat-toggles.yml");
        if (!soundFile.exists()) {
            try {
                soundFile.createNewFile();
            } catch (Exception ignored) {
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
        } catch (Exception ignored) {
        }
    }

    public boolean isPrefixEnabled() {
        return prefixEnabled;
    }

    public String getPrefixSymbol() {
        return prefixSymbol;
    }

    public void toggleAdminChat(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !adminToggle.getOrDefault(uuid, false);
        setAdminChatEnabled(player, newState);
        if (newState) {
            if (plugin.getStaffChatManager() != null) plugin.getStaffChatManager().setStaffChatEnabled(player, false);
            if (plugin.getManagerChatManager() != null) plugin.getManagerChatManager().setManagerChatEnabled(player, false);
        }
        player.sendMessage(newState ? "§aYou are now talking in AdminChat." : "§cYou have left AdminChat.");
    }

    public boolean isInAdminChat(Player player) {
        return adminToggle.getOrDefault(player.getUniqueId(), false);
    }

    public void setAdminChatEnabled(Player player, boolean enabled) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (enabled) adminToggle.put(uuid, true);
        else adminToggle.remove(uuid);
    }

    public void toggleSound(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !soundToggle.getOrDefault(uuid, true);
        soundToggle.put(uuid, newState);
        saveSoundToggles();
        player.sendMessage(newState ? "§aAdminChat sounds enabled." : "§cAdminChat sounds disabled.");
    }

    public boolean isSoundEnabled(Player player) {
        return soundToggle.getOrDefault(player.getUniqueId(), true);
    }

    public void sendAdminMessage(Player sender, String message) {
        if (sender == null || message == null) return;
        if (!sender.hasPermission("jartonchat.adminchat.use")) {
            setAdminChatEnabled(sender, false);
            sender.sendMessage(ChatColor.RED + "You don't have permission to use adminchat.");
            return;
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) return;

        String displayName = sender.getDisplayName();
        String formatted = formatIngame
                .replace("%player%", displayName)
                .replace("%message%", trimmedMessage);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("jartonchat.adminchat.use")) continue;
            if (plugin.getHushManager() != null && plugin.getHushManager().isHushed(p, HushManager.ChatType.ADMIN)) continue;

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
            if (discordBridge != null) discordBridge.sendAdminChatWebhook(plainName, plainMessage, sender);
        } catch (Throwable ignored) {
        }
    }

    public void sendAdminMessageFromDiscord(String formatted, String rawMessage) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("jartonchat.adminchat.use")) continue;
            if (plugin.getHushManager() != null && plugin.getHushManager().isHushed(p, HushManager.ChatType.ADMIN)) continue;

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
