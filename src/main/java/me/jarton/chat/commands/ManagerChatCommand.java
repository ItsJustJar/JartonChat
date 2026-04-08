package me.jarton.chat.commands;

import me.jarton.chat.JartonChat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManagerChatCommand implements CommandExecutor {

    private final JartonChat plugin;

    public ManagerChatCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!plugin.getManagerChatManager().canUseManagerChat(p)) {
            p.sendMessage(ChatColor.RED + "You don't have permission to use ManagerChat.");
            return true;
        }

        if (args.length == 0) {
            if (!plugin.getManagerChatManager().isInManagerChat(p)) {
                plugin.getStaffChatManager().setStaffChatEnabled(p, false);
                plugin.getAdminChatManager().setAdminChatEnabled(p, false);
            }
            plugin.getManagerChatManager().toggleManagerChat(p);
            return true;
        }

        String msg = String.join(" ", args).trim();
        if (msg.isEmpty()) return true;

        plugin.getManagerChatManager().sendManagerMessage(p, msg);
        return true;
    }
}
