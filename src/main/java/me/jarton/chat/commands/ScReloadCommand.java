package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.StaffChatManager;

public class ScReloadCommand implements CommandExecutor {

    private final StaffChatManager staffChatManager;

    public ScReloadCommand(StaffChatManager staffChatManager) {
        this.staffChatManager = staffChatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.staffchat.admin")) {
            sender.sendMessage("§cYou don’t have permission to reload staffchat.");
            return true;
        }

        staffChatManager.loadConfig();
        sender.sendMessage("§aStaffChat configuration reloaded.");
        return true;
    }
}
