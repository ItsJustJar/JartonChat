package me.jarton.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class UnmuteCommand implements CommandExecutor {
    private final JartonChat plugin;

    public UnmuteCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color("&cUsage: /unmute <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(ColorUtil.color("&cPlayer not found."));
            return true;
        }
        plugin.getChatService().unmute(target.getUniqueId());
        sender.sendMessage(ColorUtil.color("&f &fYou have unmuted &a" + target.getName()));
        return true;
    }
}
