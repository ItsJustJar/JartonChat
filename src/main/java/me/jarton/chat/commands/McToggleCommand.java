package me.jarton.chat.commands;

import me.jarton.chat.JartonChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class McToggleCommand implements CommandExecutor {

    private final JartonChat plugin;

    public McToggleCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!p.hasPermission("jartonchat.managerchat.use")) {
            p.sendMessage("§cYou don’t have permission to toggle managerchat.");
            return true;
        }

        plugin.getManagerChatManager().toggleManagerChat(p);
        return true;
    }
}
