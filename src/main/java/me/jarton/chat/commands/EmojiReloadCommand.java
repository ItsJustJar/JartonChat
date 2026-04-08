package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class EmojiReloadCommand implements CommandExecutor {

    private final JartonChat plugin;

    public EmojiReloadCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("jartonchat.emojis.reload")) {
            sender.sendMessage(ColorUtil.color("&cYou don't have permission to do that."));
            return true;
        }

        YamlConfiguration emojisCfg = plugin.loadOrSaveDefault("emojis.yml", false);
        plugin.emojiMap.clear();

        int count = 0;
        if (emojisCfg.isConfigurationSection("emojis")) {
            for (String key : emojisCfg.getConfigurationSection("emojis").getKeys(false)) {
                String val = emojisCfg.getString("emojis." + key, "");
                if (val != null) {
                    plugin.emojiMap.put(key, val);
                    count++;
                }
            }
        }

        sender.sendMessage(ColorUtil.color("&aReloaded &eemojis.yml&a. Loaded &e" + count + "&a emoji entries."));
        return true;
    }
}
