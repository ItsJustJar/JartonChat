package me.jarton.chat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class RanksManager {

    private final JartonChat plugin;
    private LuckPerms luckPerms;

    private final Map<String, List<String>> trackGroups = new HashMap<>();
    private final Map<String, Map<String, String>> displayMap = new HashMap<>();

    private final LinkedHashMap<String, Map<String, String>> rankColors = new LinkedHashMap<>();

    public RanksManager(JartonChat plugin) {
        this.plugin = plugin;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            plugin.getLogger().warning("LuckPerms not found! Rank lookups will not work.");
        }
        loadRanks();
    }

    private void loadRanks() {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection tracksSec = yaml.getConfigurationSection("tracks");
        if (tracksSec != null) {
            for (String track : tracksSec.getKeys(false)) {
                trackGroups.put(track.toLowerCase(Locale.ROOT), yaml.getStringList("tracks." + track));
            }
        }

        ConfigurationSection displaySec = yaml.getConfigurationSection("display");
        if (displaySec != null) {
            for (String track : displaySec.getKeys(false)) {
                ConfigurationSection sub = displaySec.getConfigurationSection(track);
                if (sub == null) continue;
                Map<String, String> map = new HashMap<>();
                for (String rank : sub.getKeys(false)) {
                    map.put(rank.toLowerCase(Locale.ROOT), sub.getString(rank));
                }
                displayMap.put(track.toLowerCase(Locale.ROOT), map);
            }
        }

        ConfigurationSection colorsSec = yaml.getConfigurationSection("rank-colors");
        if (colorsSec != null) {
            for (String rank : colorsSec.getKeys(false)) {
                Map<String, String> map = new HashMap<>();
                map.put("name", colorsSec.getString(rank + ".name", "&7"));
                map.put("message", colorsSec.getString(rank + ".message", "&7"));
                rankColors.put(rank.toLowerCase(Locale.ROOT), map);
            }
        }
    }

    public String getCombinedDisplay(Player p) {
        if (luckPerms == null) return "";
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(p);
        if (user == null) return "";

        Collection<Group> groups = user.getInheritedGroups(QueryOptions.defaultContextualOptions());

        String paid = null, rankup = null, staff = null;

        for (Group g : groups) {
            String name = g.getName().toLowerCase(Locale.ROOT);
            if (trackGroups.getOrDefault("staff", Collections.emptyList()).contains(name)) {
                staff = displayMap.getOrDefault("staff", Collections.emptyMap()).get(name);
            } else if (trackGroups.getOrDefault("paid", Collections.emptyList()).contains(name)) {
                paid = displayMap.getOrDefault("paid", Collections.emptyMap()).get(name);
            } else if (trackGroups.getOrDefault("rankup", Collections.emptyList()).contains(name)) {
                rankup = displayMap.getOrDefault("rankup", Collections.emptyMap()).get(name);
            }
        }

        if (staff != null) return staff;
        StringBuilder sb = new StringBuilder();
        if (paid != null) sb.append(paid);
        if (rankup != null) sb.append(rankup);
        return sb.toString();
    }

    public Set<String> getUserGroups(Player player) {
        if (luckPerms == null) return Collections.emptySet();
        User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if (user == null) return Collections.emptySet();

        Collection<Group> groups = user.getInheritedGroups(QueryOptions.defaultContextualOptions());
        Set<String> names = new HashSet<>();
        for (Group g : groups) {
            names.add(g.getName().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    public List<String> getTrack(String trackName) {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection tracksSec = yaml.getConfigurationSection("tracks");
        if (tracksSec == null) return Collections.emptyList();

        return yaml.getStringList("tracks." + trackName.toLowerCase(Locale.ROOT));
    }

    public String getDisplayForTrack(Player p, String track) {
        if (p == null) return "";
        List<String> trackList = getTrack(track);
        if (trackList == null || trackList.isEmpty()) return "";
        Set<String> groups = getUserGroups(p);

        for (int i = trackList.size() - 1; i >= 0; i--) {
            String g = trackList.get(i).toLowerCase(Locale.ROOT);
            if (groups.contains(g)) {
                Map<String, String> map = displayMap.getOrDefault(track.toLowerCase(Locale.ROOT), Collections.emptyMap());
                return map.getOrDefault(g, g);
            }
        }
        return "";
    }

    public String getRankNameColor(Player p) {
        if (p == null) return "&7";
        Set<String> groups = getUserGroups(p);
        for (String g : groups) {
            Map<String, String> colors = rankColors.get(g);
            if (colors != null) {
                return colors.getOrDefault("name", "&7");
            }
        }
        return "&7";
    }

    public String getRankMessageColor(Player p) {
        if (p == null) return "&7";
        Set<String> groups = getUserGroups(p);
        for (String g : groups) {
            Map<String, String> colors = rankColors.get(g);
            if (colors != null) {
                return colors.getOrDefault("message", "&7");
            }
        }
        return "&7";
    }

    public void reload() {
        trackGroups.clear();
        displayMap.clear();
        rankColors.clear();
        loadRanks();
    }
}
