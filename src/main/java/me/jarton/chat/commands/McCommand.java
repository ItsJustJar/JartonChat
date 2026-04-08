package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class McCommand implements CommandExecutor {
    private final JartonChat plugin;

    public McCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.mc")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }

        List<String> list = plugin.mildConcern;
        if (list == null || list.isEmpty()) {
            sender.sendMessage(ColorUtil.color("&7[Tip] Let’s keep it friendly."));
            return true;
        }

        int idx = ThreadLocalRandom.current().nextInt(list.size());
        sender.sendMessage(ColorUtil.color(list.get(idx)));
        return true;
    }
}
