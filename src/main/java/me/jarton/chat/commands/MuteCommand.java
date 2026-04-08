package me.jarton.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class MuteCommand implements CommandExecutor {
    private final JartonChat plugin;

    public MuteCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color("&cUsage: /mute <player> [time] [unit: seconds|minutes|hours|days|months]"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(ColorUtil.color("&cPlayer not found."));
            return true;
        }
        long duration = 24L * 60 * 60 * 1000; // default 24h
        if (args.length >= 2) {
            try {
                long amount = Long.parseLong(args[1]);
                String unit = (args.length >= 3) ? args[2].toLowerCase() : "hours";
                switch (unit) {
                    case "second":
                    case "seconds":
                    case "s": duration = amount * 1000L; break;
                    case "minute":
                    case "minutes":
                    case "m": duration = amount * 60_000L; break;
                    case "hour":
                    case "hours":
                    case "h": duration = amount * 3_600_000L; break;
                    case "day":
                    case "days":
                    case "d": duration = amount * 86_400_000L; break;
                    case "month":
                    case "months":
                    case "mon": duration = amount * 2_592_000_000L; break;
                    default: duration = amount * 3_600_000L; // hours
                }
            } catch (NumberFormatException ex) {
                sender.sendMessage(ColorUtil.color("&cTime must be a number."));
                return true;
            }
        }
        plugin.getChatService().mute(target.getUniqueId(), duration);
        sender.sendMessage(ColorUtil.color("&f &fYou have muted &a" + target.getName() + " &ffor &e" + plugin.getChatService().formatDuration(duration)));
        return true;
    }
}
