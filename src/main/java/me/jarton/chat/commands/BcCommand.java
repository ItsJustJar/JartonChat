package me.jarton.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BcCommand implements CommandExecutor, TabCompleter {

    private final JartonChat plugin;
    private YamlConfiguration broadcastConfig;

    public BcCommand(JartonChat plugin) {
        this.plugin = plugin;
        loadBroadcastConfig();
    }

    public void loadBroadcastConfig() {
        File broadcastFile = new File(plugin.getDataFolder(), "broadcasts.yml");
        if (!broadcastFile.exists()) {
            plugin.saveResource("broadcasts.yml", false);
        }
        this.broadcastConfig = YamlConfiguration.loadConfiguration(broadcastFile);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("jcbc.broadcast")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String broadcastType = args[0].toLowerCase();

        if (broadcastType.equals("reload")) {
            if (!sender.hasPermission("jcbc.reload")) {
                sender.sendMessage(ColorUtil.color("&cYou do not have permission to reload the config."));
                return true;
            }
            loadBroadcastConfig();
            sender.sendMessage(ColorUtil.color("&aBroadcasts configuration reloaded."));
            return true;
        }

        ConfigurationSection section = broadcastConfig.getConfigurationSection(broadcastType);
        if (section == null || !section.getBoolean("enabled", false)) {
            sender.sendMessage(ColorUtil.color("&cUnknown or disabled broadcast type: " + broadcastType));
            return true;
        }

        String permission = section.getString("permission");
        if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission to use this broadcast type."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color("&cUsage: /jcbc " + broadcastType + " <arg1> [arg2] ..."));
            return true;
        }

        String[] messageArgs = Arrays.copyOfRange(args, 1, args.length);

        ConfigurationSection defaults = section.getConfigurationSection("defaults");
        String mainColor = defaults != null ? defaults.getString("main_color", "#FFFFFF") : "#FFFFFF";
        String secondaryColor = defaults != null ? defaults.getString("secondary_color", "#FFFFFF") : "#FFFFFF";
        String textColor = defaults != null ? defaults.getString("text_color", "&f") : "&f";
        boolean bold = defaults != null && defaults.getBoolean("bold", false);

        List<String> messages = section.getStringList("messages");

        for (String line : messages) {
            String processedLine = line
                    .replace("{main_color}", mainColor)
                    .replace("{secondary_color}", secondaryColor)
                    .replace("{text_color}", textColor)
                    .replace("{bold}", bold ? "&l" : "")
                    .replace("{store_text}", section.getString("store_text", ""));

            for (int i = 0; i < messageArgs.length; i++) {
                processedLine = processedLine.replace("%arg" + (i + 1) + "%", messageArgs[i].replace('_', ' '));
            }

            String finalMessage = ColorUtil.color(processedLine);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(finalMessage);
            }
        }

        List<Map<?, ?>> sounds = section.getMapList("sounds");
        for (Map<?, ?> soundMap : sounds) {
            try {
                String soundName = ((String) soundMap.get("name")).toUpperCase();
                float volume = (float) ((Number) soundMap.get("volume")).doubleValue();
                float pitch = (float) ((Number) soundMap.get("pitch")).doubleValue();
                Sound sound = Sound.valueOf(soundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound in broadcasts.yml for type '" + broadcastType + "': " + e.getMessage());
            }
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&eJartonChat Broadcast Usage:"));
        sender.sendMessage(ColorUtil.color("&a/jcbc <type> [args...] &7- Send a broadcast."));
        sender.sendMessage(ColorUtil.color("&a/jcbc reload &7- Reload the broadcasts.yml config."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("jcbc.reload")) {
                completions.add("reload");
            }
            completions.addAll(broadcastConfig.getKeys(false).stream()
                    .filter(key -> broadcastConfig.getBoolean(key + ".enabled", false))
                    .filter(key -> sender.hasPermission(broadcastConfig.getString(key + ".permission", "jcbc.broadcast")))
                    .collect(Collectors.toList()));
            return completions;
        }
        return null;
    }
}