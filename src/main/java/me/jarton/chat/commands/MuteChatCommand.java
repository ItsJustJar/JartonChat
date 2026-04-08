package me.jarton.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

public class MuteChatCommand implements CommandExecutor {
    private final JartonChat plugin;

    public MuteChatCommand(JartonChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jartonchat.admin")) {
            sender.sendMessage(ColorUtil.color("&cYou do not have permission."));
            return true;
        }
        plugin.getChatService().toggleGlobalChat();
        boolean muted = plugin.getChatService().isGlobalChatMuted();
        String key = muted ? "you_have_muted_global_chat" : "you_have_unmuted_global_chat";
        String msg = plugin.messageMap.getOrDefault(key, muted ? "&cGlobal chat muted." : "&aGlobal chat unmuted.");
        sender.sendMessage(ColorUtil.color(msg));
        return true;
    }
}
