package me.jarton.chat.commands;

import me.jarton.chat.ChatService;
import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;
import me.jarton.chat.listeners.SpyListener;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final JartonChat plugin;
    private final ChatService service;

    public ReplyCommand(JartonChat plugin, ChatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player s)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("no_one_to_reply_to", "&cYou have no one to reply to!")));
            return true;
        }

        CommandExecutor msgExecutor = plugin.getCommand("msg").getExecutor();
        if (!(msgExecutor instanceof MsgCommand msgCmd)) {
            s.sendMessage(ColorUtil.color("&cReply is currently unavailable."));
            return true;
        }

        UUID last = msgCmd.getLast(s.getUniqueId());
        if (last == null) {
            s.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("no_one_to_reply_to", "&cYou have no one to reply to!")));
            return true;
        }

        Player t = Bukkit.getPlayer(last);
        if (t == null) {
            s.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("player_not_found", "&cPlayer not found.")));
            return true;
        }

        String message = String.join(" ", args);
        if (!service.canMessage(s, t)) {
            s.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("pm_blocked", "&cThat player is not accepting private messages.")));
            return true;
        }

        String outS = MsgCommand.applyPmFormat(plugin.pmFormatSender, s, t, message, true);
        s.sendMessage(ColorUtil.color(outS));
        try {
            s.playSound(s.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 1.5f);
            s.playSound(s.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 2.0f);
        } catch (Throwable ignored) {
        }

        String outR = MsgCommand.applyPmFormat(plugin.pmFormatRecipient, s, t, message, false);
        t.sendMessage(ColorUtil.color(outR));
        try {
            t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 1.5f);
            t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 2.0f);
        } catch (Throwable ignored) {
        }

        try {
            SpyListener sL = SpyListener.get();
            if (sL != null) sL.handlePrivateMessage(s, t, message, label);
        } catch (Throwable ignored) {
        }

        msgCmd.setLast(s.getUniqueId(), t.getUniqueId());
        msgCmd.setLast(t.getUniqueId(), s.getUniqueId());
        return true;
    }
}
