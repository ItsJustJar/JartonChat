package me.jarton.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class ClearChatCommand implements CommandExecutor {
    private final JartonChat plugin;

    public ClearChatCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }
        for (int i = 0; i < 100; i++) {
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(" "));
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(ColorUtil.color("&eChat has been cleared by &a" + sender.getName())));
        try {
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_COMPOSTER_READY, 1f, 2f));
        } catch (Throwable ignored) {}
        return true;
    }
}
