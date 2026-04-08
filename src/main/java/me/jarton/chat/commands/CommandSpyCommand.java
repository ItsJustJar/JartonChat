package me.jarton.chat.commands;

import me.jarton.chat.listeners.SpyListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandSpyCommand implements CommandExecutor {

    private final SpyListener spy;

    public CommandSpyCommand(SpyListener spy) {
        this.spy = spy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!spy.toggleCommandSpy(p)) {
                p.sendMessage("§cNo permission.");
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            p.sendMessage("§cThat player is not online.");
            return true;
        }

        if (!spy.toggleCommandSpyTarget(p, target.getUniqueId(), target.getName())) {
            p.sendMessage("§cNo permission.");
        }
        return true;
    }
}
