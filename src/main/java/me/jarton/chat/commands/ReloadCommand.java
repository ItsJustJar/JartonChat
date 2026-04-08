package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class ReloadCommand implements CommandExecutor {
    private final JartonChat plugin;

    public ReloadCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(ColorUtil.color("&aJartonChat reloaded."));
            return true;
        }
        sender.sendMessage(ColorUtil.color("&eUsage: /jartonchat reload"));
        return true;
    }
}
