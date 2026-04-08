package me.jarton.chat.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.jarton.chat.ChatService;
import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;
import me.jarton.chat.playerresolution.PlayerResolver;
import me.jarton.chat.listeners.SpyListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MsgCommand implements CommandExecutor {

    private static final String DEFAULT_SENDER_FORMAT = "&6&lME &e-> &e{recipient}&f: {message}";
    private static final String DEFAULT_RECIPIENT_FORMAT = "&e{sender} &e-> &6&lME&f: {message}";

    private final Map<UUID, UUID> lastRecipient = new ConcurrentHashMap<>();

    private final JartonChat plugin;
    private final ChatService service;
    private final PlayerResolver playerResolver;

    public MsgCommand(JartonChat plugin, ChatService service, PlayerResolver playerResolver) {
        this.plugin = plugin;
        this.service = service;
        this.playerResolver = playerResolver;
    }

    public UUID getLast(UUID player) {
        return lastRecipient.get(player);
    }

    public void setLast(UUID player, UUID target) {
        lastRecipient.put(player, target);
    }

    public static String applyPmFormat(String template, Player sender, Player recipient, String message, boolean senderView) {
        String fallback = senderView ? DEFAULT_SENDER_FORMAT : DEFAULT_RECIPIENT_FORMAT;
        String resolved = (template == null || template.isBlank()) ? fallback : template;

        return resolved
                .replace("{sender}", sender.getName())
                .replace("{recipient}", recipient.getName())
                .replace("{message}", message)
                .replace("%sender%", sender.getName())
                .replace("%recipient%", recipient.getName())
                .replace("%message%", message)
                .replace("SENDER", sender.getName())
                .replace("RECIPENT", recipient.getName())
                .replace("RECIPIENT", recipient.getName())
                .replace("MESSAGE", message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player s)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.color("&cUsage: /msg <player> <message>"));
            return true;
        }
        Player t = playerResolver.getPlayer(args[0]).orElse(null);
        if (t == null) {
            s.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("player_not_found", "&cPlayer not found.")));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        if (!service.canMessage(s, t)) {
            s.sendMessage(ColorUtil.color(plugin.messageMap.getOrDefault("pm_blocked", "&cThat player is not accepting private messages.")));
            return true;
        }

        String outS = applyPmFormat(plugin.pmFormatSender, s, t, message, true);
        s.sendMessage(ColorUtil.color(outS));
        try {
            s.playSound(s.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 1.5f);
            s.playSound(s.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 2.0f);
        } catch (Throwable ignored) {}

        String outR = applyPmFormat(plugin.pmFormatRecipient, s, t, message, false);
        t.sendMessage(ColorUtil.color(outR));
        try {
            t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 1.5f);
            t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.4f, 2.0f);
        } catch (Throwable ignored) {}

        try {
            SpyListener sL = SpyListener.get();
            if (sL != null) sL.handlePrivateMessage(s, t, message, label);
        } catch (Throwable ignored) {}

        lastRecipient.put(s.getUniqueId(), t.getUniqueId());
        lastRecipient.put(t.getUniqueId(), s.getUniqueId());
        return true;
    }
}
