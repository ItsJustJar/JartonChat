package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.jarton.chat.StaffChatManager;

public class ScCommand implements CommandExecutor {

    private final StaffChatManager staffChatManager;

    public ScCommand(StaffChatManager staffChatManager) {
        this.staffChatManager = staffChatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("jartonchat.staffchat.use")) {
            player.sendMessage("§cYou don’t have permission to use staffchat.");
            return true;
        }

        if (args.length == 0) {
            staffChatManager.toggleStaffChat(player);
            return true;
        }

        String message = String.join(" ", args);
        staffChatManager.sendStaffMessage(player, message);
        return true;
    }
}
