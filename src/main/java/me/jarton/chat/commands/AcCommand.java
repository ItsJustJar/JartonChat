package me.jarton.chat.commands;

import me.jarton.chat.JartonChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcCommand implements CommandExecutor {

    private final JartonChat plugin;

    public AcCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!p.hasPermission("jartonchat.adminchat.use")) {
            p.sendMessage("§cYou don’t have permission to use adminchat.");
            return true;
        }

        if (args.length == 0) {
            plugin.getAdminChatManager().toggleAdminChat(p);
            return true;
        }

        String msg = String.join(" ", args).trim();
        if (msg.isEmpty()) return true;

        plugin.getAdminChatManager().sendAdminMessage(p, msg);
        return true;
    }
}
