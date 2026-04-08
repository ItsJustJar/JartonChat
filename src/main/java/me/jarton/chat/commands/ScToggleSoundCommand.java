package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.jarton.chat.StaffChatManager;

public class ScToggleSoundCommand implements CommandExecutor {

    private final StaffChatManager manager;

    public ScToggleSoundCommand(StaffChatManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        manager.toggleSound(player);
        return true;
    }
}
