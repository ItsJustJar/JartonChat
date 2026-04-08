package me.jarton.chat;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoBroadcastTask extends BukkitRunnable {

    private final JartonChat plugin;
    private final List<Map.Entry<String, java.util.List<String>>> active;
    private int index = 0;

    public AutoBroadcastTask(JartonChat plugin, int seconds) {
        this.plugin = plugin;
        this.active = new ArrayList<>(plugin.broadcasts.entrySet());
    }

    @Override
    public void run() {
        if (active.isEmpty()) return;
        Map.Entry<String, java.util.List<String>> entry = active.get(index);
        index = (index + 1) % active.size();

        for (String line : entry.getValue()) {
            if (line == null || line.isEmpty()) {
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(" "));
            } else {
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(ColorUtil.color(line)));
            }
        }
    }
}
