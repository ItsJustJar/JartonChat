package me.jarton.chat.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.jarton.chat.ChatColorManager;
import me.jarton.chat.ColorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ChatColorCommand implements CommandExecutor {

    private final ChatColorManager colorManager;

    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private static final Map<String, String> NAMED_COLORS = new HashMap<>();

    static {
        NAMED_COLORS.put("black", "&0");
        NAMED_COLORS.put("dark_blue", "&1");
        NAMED_COLORS.put("dark_green", "&2");
        NAMED_COLORS.put("dark_aqua", "&3");
        NAMED_COLORS.put("dark_red", "&4");
        NAMED_COLORS.put("dark_purple", "&5");
        NAMED_COLORS.put("gold", "&6");
        NAMED_COLORS.put("gray", "&7");
        NAMED_COLORS.put("grey", "&7");
        NAMED_COLORS.put("dark_gray", "&8");
        NAMED_COLORS.put("dark_grey", "&8");
        NAMED_COLORS.put("blue", "&9");
        NAMED_COLORS.put("green", "&a");
        NAMED_COLORS.put("aqua", "&b");
        NAMED_COLORS.put("red", "&c");
        NAMED_COLORS.put("light_purple", "&d");
        NAMED_COLORS.put("purple", "&5");
        NAMED_COLORS.put("pink", "&d");
        NAMED_COLORS.put("yellow", "&e");
        NAMED_COLORS.put("white", "&f");

        NAMED_COLORS.put("orange", "&6");
        NAMED_COLORS.put("cyan", "&b");
        NAMED_COLORS.put("lime", "&a");
        NAMED_COLORS.put("magenta", "&d");
    }

    public ChatColorCommand(ChatColorManager colorManager) {
        this.colorManager = colorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("jartonchat.chatcolor")) {
            player.sendMessage("§cYou don't have permission to change your chat color.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage: /chatcolor <code|#hex|name|reset>");
            player.sendMessage("§7Examples: §f/chatcolor &a§7, §f/chatcolor #ff8800§7, §f/chatcolor red§7, §f/chatcolor reset");
            return true;
        }

        String raw = args[0].trim();

        String lower = raw.toLowerCase();
        if (lower.equals("reset") || lower.equals("default")) {
            colorManager.clearColor(player);
            player.sendMessage("§aYour chat color has been reset to your rank's default.");
            return true;
        }

        String colorCode = null;

        if (NAMED_COLORS.containsKey(lower)) {
            colorCode = NAMED_COLORS.get(lower);
        } else {
            String arg = raw;

            if (!arg.startsWith("&") && !arg.startsWith("#")) {
                arg = "&" + arg;
            }

            colorCode = arg;
        }

        if (colorCode.startsWith("#")) {
            if (!HEX_PATTERN.matcher(colorCode).matches()) {
                player.sendMessage("§cInvalid hex color. Use format §f#RRGGBB§c, e.g. §f#ff8800§c.");
                return true;
            }
        } else if (colorCode.startsWith("&")) {
            if (colorCode.length() < 2 || !isValidLegacyCode(colorCode.charAt(1))) {
                player.sendMessage("§cInvalid color code. Use something like §f&7, &a, &b, &e§c.");
                return true;
            }
        } else {
            player.sendMessage("§cInvalid color. Use a name (e.g. red), &code, or #hex.");
            return true;
        }

        try {
            String test = ColorUtil.color(colorCode + "test");
            if (ChatColor.stripColor(test).isEmpty()) {
                player.sendMessage("§cThat color code didn't work. Try another one.");
                return true;
            }
        } catch (Exception ex) {
            player.sendMessage("§cThat color code didn't work. Try another one.");
            return true;
        }

        colorManager.setColor(player, colorCode);
        player.sendMessage("§aYour chat color has been updated.");

        return true;
    }

    private boolean isValidLegacyCode(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'k' && c <= 'o') ||
               c == 'r';
    }
}
