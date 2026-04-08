package me.jarton.chat.listeners;

import me.jarton.chat.JartonChat;
import me.jarton.chat.players.SpyLogService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SpyListener implements Listener {

    private static SpyListener instance;
    public static SpyListener get() { return instance; }

    private final JartonChat plugin;
    private final SpyLogService logService;

    private final Set<UUID> socialSpyEnabled = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> commandSpyEnabled = Collections.synchronizedSet(new HashSet<>());

    private final Map<UUID, UUID> socialSpyTargets = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, UUID> commandSpyTargets = Collections.synchronizedMap(new HashMap<>());

    private final String permSocialSpy;
    private final String permSocialSpyExempt;

    private final String permCommandSpy;
    private final String permCommandSpyExempt;

    private final boolean includeConsoleInCommandSpy;

    public SpyListener(JartonChat plugin) {
        this.plugin = plugin;
        instance = this;

        this.permSocialSpy = plugin.getConfig().getString("spy.permissions.socialspy", "jartonchat.socialspy");
        this.permSocialSpyExempt = plugin.getConfig().getString("spy.permissions.socialspy_exempt", "jartonchat.socialspy.exempt");

        this.permCommandSpy = plugin.getConfig().getString("spy.permissions.commandspy", "jartonchat.commandspy");
        this.permCommandSpyExempt = plugin.getConfig().getString("spy.permissions.commandspy_exempt", "jartonchat.commandspy.exempt");

        this.includeConsoleInCommandSpy = plugin.getConfig().getBoolean("spy.commandspy.include-console", true);

        this.logService = new SpyLogService(plugin);
    }

    public boolean toggleSocialSpy(Player viewer) {
        if (!viewer.hasPermission(permSocialSpy)) return false;

        UUID id = viewer.getUniqueId();
        if (socialSpyEnabled.contains(id)) {
            socialSpyEnabled.remove(id);
            socialSpyTargets.remove(id);
            viewer.sendMessage("§6[SocialSpy] §7Disabled.");
        } else {
            socialSpyEnabled.add(id);
            socialSpyTargets.remove(id); // global
            viewer.sendMessage("§6[SocialSpy] §aEnabled§7. (Global PM spy)");
        }
        return true;
    }

    public boolean toggleSocialSpyTarget(Player viewer, UUID targetUuid, String targetName) {
        if (!viewer.hasPermission(permSocialSpy)) return false;

        UUID viewerId = viewer.getUniqueId();
        socialSpyEnabled.add(viewerId);

        UUID current = socialSpyTargets.get(viewerId);
        if (current != null && current.equals(targetUuid)) {
            socialSpyTargets.remove(viewerId);
            viewer.sendMessage("§6[SocialSpy] §aEnabled§7. Now watching: §eEVERYONE§7.");
        } else {
            socialSpyTargets.put(viewerId, targetUuid);
            viewer.sendMessage("§6[SocialSpy] §aEnabled§7. Now watching PMs for: §e" + targetName + "§7.");
        }
        return true;
    }

    public void handlePrivateMessage(Player sender, Player recipient, String message, String viaLabel) {
        if (sender == null || recipient == null) return;

        if (sender.hasPermission(permSocialSpyExempt) || recipient.hasPermission(permSocialSpyExempt)) {
            return;
        }

        String line = "§6[SocialSpy] §e" + sender.getName() + " §7-> §e" + recipient.getName() + "§7: §f" + message;

        synchronized (socialSpyEnabled) {
            for (UUID viewerId : socialSpyEnabled) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) continue;
                if (!viewer.hasPermission(permSocialSpy)) continue;
                if (viewerId.equals(sender.getUniqueId()) || viewerId.equals(recipient.getUniqueId())) continue;

                UUID target = socialSpyTargets.get(viewerId);
                if (target != null) {
                    if (!target.equals(sender.getUniqueId()) && !target.equals(recipient.getUniqueId())) continue;
                }

                viewer.sendMessage(line);
            }
        }

        logService.logPrivateMessage(sender, recipient, message, viaLabel);
    }

    public boolean toggleCommandSpy(Player viewer) {
        if (!viewer.hasPermission(permCommandSpy)) return false;

        UUID id = viewer.getUniqueId();
        if (commandSpyEnabled.contains(id)) {
            commandSpyEnabled.remove(id);
            commandSpyTargets.remove(id);
            viewer.sendMessage("§6[CommandSpy] §7Disabled.");
        } else {
            commandSpyEnabled.add(id);
            commandSpyTargets.remove(id); // global
            viewer.sendMessage("§6[CommandSpy] §aEnabled§7. (Global command spy)");
        }
        return true;
    }

    public boolean toggleCommandSpyTarget(Player viewer, UUID targetUuid, String targetName) {
        if (!viewer.hasPermission(permCommandSpy)) return false;

        UUID viewerId = viewer.getUniqueId();
        commandSpyEnabled.add(viewerId);

        UUID current = commandSpyTargets.get(viewerId);
        if (current != null && current.equals(targetUuid)) {
            commandSpyTargets.remove(viewerId);
            viewer.sendMessage("§6[CommandSpy] §aEnabled§7. Now watching: §eEVERYONE§7.");
        } else {
            commandSpyTargets.put(viewerId, targetUuid);
            viewer.sendMessage("§6[CommandSpy] §aEnabled§7. Now watching: §e" + targetName + "§7.");
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        Player runner = e.getPlayer();
        String raw = e.getMessage(); // includes leading "/"

        if (runner.hasPermission(permCommandSpyExempt)) return;
        if (isPrivateChannelCommand(raw)) return;

        String safeRaw = isPrivateMessageCommand(raw) ? redactPmCommand(raw) : raw;

        String msg = "§6[CommandSpy] §e" + runner.getName() + "§7: §f" + safeRaw;

        synchronized (commandSpyEnabled) {
            for (UUID viewerId : commandSpyEnabled) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null || !viewer.isOnline()) continue;
                if (!viewer.hasPermission(permCommandSpy)) continue;
                if (viewerId.equals(runner.getUniqueId())) continue;

                UUID target = commandSpyTargets.get(viewerId);
                if (target != null && !target.equals(runner.getUniqueId())) continue;

                viewer.sendMessage(msg);
            }
        }

        logService.logPlayerCommand(runner, safeRaw);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent e) {
        if (!includeConsoleInCommandSpy) return;

        CommandSender sender = e.getSender();
        String raw = "/" + e.getCommand();
        if (isPrivateChannelCommand(raw)) return;

        String safeRaw = isPrivateMessageCommand(raw) ? "/msg <redacted>" : raw;

        String msg = "§6[CommandSpy] §cCONSOLE§7: §f" + safeRaw;
        synchronized (commandSpyEnabled) {
            for (UUID viewerId : commandSpyEnabled) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline() && viewer.hasPermission(permCommandSpy)) {
                    viewer.sendMessage(msg);
                }
            }
        }

        logService.logConsoleCommand(sender == null ? "CONSOLE" : sender.getName(), safeRaw);
    }

    private static boolean isPrivateMessageCommand(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toLowerCase();
        if (s.startsWith("/")) s = s.substring(1);
        return s.startsWith("msg ") || s.startsWith("tell ") || s.startsWith("whisper ") || s.startsWith("pm ")
                || s.equals("msg") || s.equals("tell") || s.equals("whisper") || s.equals("pm")
                || s.startsWith("reply ") || s.equals("reply")
                || s.startsWith("r ") || s.equals("r");
    }

    private static boolean isPrivateChannelCommand(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toLowerCase();
        if (s.startsWith("/")) s = s.substring(1);
        int namespaceIdx = s.indexOf(':');
        if (namespaceIdx >= 0 && namespaceIdx < s.length() - 1) {
            s = s.substring(namespaceIdx + 1);
        }
        return s.equals("ac") || s.startsWith("ac ") || s.equals("mc") || s.startsWith("mc ");
    }

    private static String redactPmCommand(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return "/msg <redacted>";
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("/reply") || lower.startsWith("/r")) return "/reply <redacted>";
        return "/msg <redacted>";
    }
}
