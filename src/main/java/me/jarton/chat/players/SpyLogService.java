package me.jarton.chat.players;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class SpyLogService {

    private final JavaPlugin plugin;

    private final boolean enabled;
    private final boolean logCommands;
    private final boolean logPrivateMessages;
    private final int retentionDays;

    private final File root;
    private final ConcurrentLinkedQueue<QueuedWrite> queue = new ConcurrentLinkedQueue<>();

    public SpyLogService(JavaPlugin plugin) {
        this.plugin = plugin;

        this.enabled = plugin.getConfig().getBoolean("spy.logging.enabled", true);
        this.logCommands = plugin.getConfig().getBoolean("spy.logging.commands", true);
        this.logPrivateMessages = plugin.getConfig().getBoolean("spy.logging.private_messages", true);
        this.retentionDays = plugin.getConfig().getInt("spy.logging.retention-days", 14);

        this.root = new File(plugin.getDataFolder(), "command-logs");
        if (enabled && !root.exists()) {
            root.mkdirs();
        }

        if (enabled) {
            startFlushTask();
            startPurgeTask();
        }
    }

    public void logPlayerCommand(Player p, String rawCommand) {
        if (!enabled || !logCommands) return;

        Location loc = p.getLocation();
        String world = (loc.getWorld() == null ? "" : loc.getWorld().getName());

        String json =
                "{"
                        + "\"ts\":\"" + esc(Instant.now().toString()) + "\","
                        + "\"type\":\"command\","
                        + "\"uuid\":\"" + esc(p.getUniqueId().toString()) + "\","
                        + "\"name\":\"" + esc(p.getName()) + "\","
                        + "\"cmd\":\"" + esc(rawCommand) + "\","
                        + "\"world\":\"" + esc(world) + "\","
                        + "\"x\":" + loc.getX() + ","
                        + "\"y\":" + loc.getY() + ","
                        + "\"z\":" + loc.getZ()
                        + "}";

        queue.add(new QueuedWrite("commands", p.getUniqueId().toString() + ".jsonl", json));
    }

    public void logConsoleCommand(String senderName, String rawCommand) {
        if (!enabled || !logCommands) return;

        String json =
                "{"
                        + "\"ts\":\"" + esc(Instant.now().toString()) + "\","
                        + "\"type\":\"command\","
                        + "\"uuid\":null,"
                        + "\"name\":\"" + esc(senderName == null ? "CONSOLE" : senderName) + "\","
                        + "\"cmd\":\"" + esc(rawCommand) + "\""
                        + "}";

        queue.add(new QueuedWrite("commands", "console.jsonl", json));
    }

    public void logPrivateMessage(Player sender, Player recipient, String message, String viaLabel) {
        if (!enabled || !logPrivateMessages) return;

        String json =
                "{"
                        + "\"ts\":\"" + esc(Instant.now().toString()) + "\","
                        + "\"type\":\"pm\","
                        + "\"via\":\"" + esc(viaLabel == null ? "" : viaLabel) + "\","
                        + "\"from_uuid\":\"" + esc(sender.getUniqueId().toString()) + "\","
                        + "\"from_name\":\"" + esc(sender.getName()) + "\","
                        + "\"to_uuid\":\"" + esc(recipient.getUniqueId().toString()) + "\","
                        + "\"to_name\":\"" + esc(recipient.getName()) + "\","
                        + "\"message\":\"" + esc(message) + "\""
                        + "}";

        queue.add(new QueuedWrite("pms", sender.getUniqueId().toString() + ".jsonl", json));
        queue.add(new QueuedWrite("pms", recipient.getUniqueId().toString() + ".jsonl", json));
    }

    private void startFlushTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                flush();
            } catch (Throwable t) {
                plugin.getLogger().warning("[SpyLog] flush failed: " + t.getMessage());
            }
        }, 40L, 40L);
    }

    private void flush() throws Exception {
        if (queue.isEmpty()) return;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        File dayFolder = new File(root, today.toString());
        dayFolder.mkdirs();

        while (!queue.isEmpty()) {
            QueuedWrite qw = queue.poll();
            if (qw == null) break;

            File typeFolder = new File(dayFolder, qw.folder);
            typeFolder.mkdirs();

            File out = new File(typeFolder, qw.fileName);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out, StandardCharsets.UTF_8, true))) {
                bw.write(qw.jsonLine);
                bw.newLine();
            }
        }
    }

    private void startPurgeTask() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::purgeOld);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::purgeOld, 20L * 60L * 60L * 12L, 20L * 60L * 60L * 12L);
    }

    private void purgeOld() {
        try {
            if (!enabled) return;
            if (retentionDays <= 0) return;
            if (!root.exists()) return;

            LocalDate cutoff = LocalDate.now(ZoneId.systemDefault()).minusDays(retentionDays);
            File[] folders = root.listFiles();
            if (folders == null) return;

            for (File f : folders) {
                if (!f.isDirectory()) continue;
                try {
                    LocalDate day = LocalDate.parse(f.getName());
                    if (day.isBefore(cutoff)) deleteRecursively(f);
                } catch (Exception ignored) {}
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[SpyLog] purge failed: " + t.getMessage());
        }
    }

    private void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursively(k);
        f.delete();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class QueuedWrite {
        final String folder;
        final String fileName;
        final String jsonLine;

        private QueuedWrite(String folder, String fileName, String jsonLine) {
            this.folder = folder;
            this.fileName = fileName;
            this.jsonLine = jsonLine;
        }
    }
}
