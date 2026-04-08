package me.jarton.chat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteStorage {
    private final JartonChat plugin;
    private final File file;
    private final Map<UUID, Long> muted = new HashMap<>();

    public MuteStorage(JartonChat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mutes.yml");
    }

    public void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.isConfigurationSection("mutes")) {
            for (String k : cfg.getConfigurationSection("mutes").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(k);
                    long until = cfg.getLong("mutes." + k, 0L);
                    muted.put(id, until);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : muted.entrySet()) {
            cfg.set("mutes." + e.getKey(), e.getValue());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mutes.yml: " + e.getMessage());
        }
    }

    public long remainingMillis(UUID id) {
        Long until = muted.get(id);
        if (until == null) return 0L;
        if (until <= 0L) return Long.MAX_VALUE; // permanent
        long left = until - System.currentTimeMillis();
        if (left <= 0L) {
            muted.remove(id);
            return 0L;
        }
        return left;
    }

    public void mute(UUID id, long durationMillis) {
        long until = System.currentTimeMillis() + durationMillis;
        muted.put(id, until);
        save();
    }

    public void unmute(UUID id) {
        muted.remove(id);
        save();
    }
}
