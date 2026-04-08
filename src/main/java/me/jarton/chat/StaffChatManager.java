package me.jarton.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StaffChatManager {

    private final JartonChat plugin;
    private final Map<UUID, Boolean> staffToggle = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> soundToggle = new ConcurrentHashMap<>();

    private File soundFile;
    private YamlConfiguration soundConfig;

    private boolean togglesPersist;
    private boolean allowLeave;
    private boolean notifyOnJoin;

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

    public StaffChatManager(JartonChat plugin) {
        this.plugin = plugin;
        loadConfig();
        loadSoundToggles();
    }

    public void loadConfig() {
        plugin.saveResource("staffchat.yml", false);
        File file = new File(plugin.getDataFolder(), "staffchat.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        togglesPersist = cfg.getBoolean("staff-chat.toggles.chat-toggles-persist-after-restart", true);
        allowLeave = cfg.getBoolean("staff-chat.toggles.let-staff-members-leave-staffchat", true);
        notifyOnJoin = cfg.getBoolean("staff-chat.toggles.notify-toggle-status-on-join", true);

        prefixEnabled = cfg.getBoolean("staff-chat.prefix.enabled", true);
        prefixSymbol = cfg.getString("staff-chat.prefix.symbol", "#");

        soundMessagesEnabled = cfg.getBoolean("sounds.messages.enabled", true);
        soundMessagesName = cfg.getString("sounds.messages.name", "ENTITY.ITEM.PICKUP");
        soundMessagesVolume = (float) cfg.getDouble("sounds.messages.volume", 1.0);
        soundMessagesPitch = (float) cfg.getDouble("sounds.messages.pitch", 0.5);

        soundNotifyEnabled = cfg.getBoolean("sounds.notifications.enabled", true);
        soundNotifyName = cfg.getString("sounds.notifications.name", "ENTITY.ITEM.PICKUP");
        soundNotifyVolume = (float) cfg.getDouble("sounds.notifications.volume", 1.0);
        soundNotifyPitch = (float) cfg.getDouble("sounds.notifications.pitch", 0.75);

        formatIngame = cfg.getString("formatting.ingame", "&8[&bStaffChat&8] &7%player%: &f%message%");
    }

    private void loadSoundToggles() {
        soundFile = new File(plugin.getDataFolder(), "staffchat-toggles.yml");
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

    public void toggleStaffChat(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !staffToggle.getOrDefault(uuid, false);
        setStaffChatEnabled(player, newState);
        if (newState) {
            if (plugin.getAdminChatManager() != null) plugin.getAdminChatManager().setAdminChatEnabled(player, false);
            if (plugin.getManagerChatManager() != null) plugin.getManagerChatManager().setManagerChatEnabled(player, false);
        }
        player.sendMessage(newState ? "§aYou are now talking in StaffChat." : "§cYou have left StaffChat.");
    }

    public boolean isInStaffChat(Player player) {
        return staffToggle.getOrDefault(player.getUniqueId(), false);
    }

    public void setStaffChatEnabled(Player player, boolean enabled) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (enabled) staffToggle.put(uuid, true);
        else staffToggle.remove(uuid);
    }

    public void toggleSound(Player player) {
        UUID uuid = player.getUniqueId();
        boolean newState = !soundToggle.getOrDefault(uuid, true);
        soundToggle.put(uuid, newState);
        saveSoundToggles();
        player.sendMessage(newState ? "§aStaffChat sounds enabled." : "§cStaffChat sounds disabled.");
    }

    public boolean isSoundEnabled(Player player) {
        return soundToggle.getOrDefault(player.getUniqueId(), true);
    }

    public boolean isHushed(Player player) {
        HushManager hm = plugin.getHushManager();
        if (hm == null) return false;
        return hm.isHushed(player, HushManager.ChatType.STAFF);
    }

    public boolean isPrefixEnabled() {
        return prefixEnabled;
    }

    public String getPrefixSymbol() {
        return prefixSymbol;
    }

    public void sendStaffMessage(Player sender, String message) {
        if (sender == null || message == null) return;
        if (!sender.hasPermission("jartonchat.staffchat.use")) {
            setStaffChatEnabled(sender, false);
            sender.sendMessage(ChatColor.RED + "You don't have permission to use staffchat.");
            return;
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) return;

        String displayName = sender.getDisplayName();
        String formatted = formatIngame
                .replace("%player%", displayName)
                .replace("%message%", trimmedMessage);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("jartonchat.staffchat.use")) continue;
            if (isHushed(p)) continue;

            p.sendMessage(ColorUtil.color(formatted));
            if (soundMessagesEnabled && isSoundEnabled(p)) {
                try {
                    p.playSound(p.getLocation(), Sound.valueOf(soundMessagesName.toUpperCase()), soundMessagesVolume, soundMessagesPitch);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        String plainName = ChatColor.stripColor(ColorUtil.color(displayName));
        String plainMessage = ChatColor.stripColor(ColorUtil.color(trimmedMessage));
        if (plugin.getDiscordBridge() != null) {
            plugin.getDiscordBridge().sendStaffWebhook(plainName, plainMessage, sender);
        }
    }

    public void sendStaffMessageFromDiscord(String formatted, String rawMessage) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("jartonchat.staffchat.use")) continue;
            if (isHushed(p)) continue;

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
