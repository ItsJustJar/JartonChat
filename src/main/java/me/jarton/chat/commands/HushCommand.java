package me.jarton.chat.commands;

import me.jarton.chat.HushManager;
import me.jarton.chat.JartonChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HushCommand implements CommandExecutor {

    private final JartonChat plugin;

    public HushCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!p.hasPermission("jartonchat.hush")) {
            p.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length < 1) {
            p.sendMessage("§cUsage: /hush <staff|admin|manager|all>");
            return true;
        }

        String mode = args[0].toLowerCase();
        HushManager hm = plugin.getHushManager();

        switch (mode) {
            case "staff" -> {
                boolean newState = !hm.isHushed(p, HushManager.ChatType.STAFF);
                hm.setHushed(p, HushManager.ChatType.STAFF, newState);
                p.sendMessage(newState ? "§aStaffChat hushed." : "§cStaffChat unhushed.");
            }
            case "admin" -> {
                boolean newState = !hm.isHushed(p, HushManager.ChatType.ADMIN);
                hm.setHushed(p, HushManager.ChatType.ADMIN, newState);
                p.sendMessage(newState ? "§aAdminChat hushed." : "§cAdminChat unhushed.");
            }
            case "manager" -> {
                boolean newState = !hm.isHushed(p, HushManager.ChatType.MANAGER);
                hm.setHushed(p, HushManager.ChatType.MANAGER, newState);
                p.sendMessage(newState ? "§aManagerChat hushed." : "§cManagerChat unhushed.");
            }
            case "all" -> {
                boolean isAll = hm.getHushed(p).containsAll(java.util.EnumSet.allOf(HushManager.ChatType.class));
                hm.setAll(p, !isAll);
                p.sendMessage(!isAll ? "§aAll staff chats hushed." : "§cAll staff chats unhushed.");
            }
            default -> p.sendMessage("§cUsage: /hush <staff|admin|manager|all>");
        }

        return true;
    }
}
