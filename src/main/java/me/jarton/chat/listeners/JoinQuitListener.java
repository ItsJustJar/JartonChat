package me.jarton.chat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.jarton.chat.ChatService;
import me.jarton.chat.ColorUtil;
import me.jarton.chat.JartonChat;

import java.util.List;
import java.util.Random;

public class JoinQuitListener implements Listener {

    private final JartonChat plugin;
    private final ChatService service;
    private final Random rnd = new Random();

    public JoinQuitListener(JartonChat plugin, ChatService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (plugin.getConfig().getBoolean("hush.reset-on-relog", true) && plugin.getHushManager() != null) {
            plugin.getHushManager().clear(p);
        }

        String msg = null;
        List<String> list = plugin.joinMessages;
        if (!list.isEmpty()) {
            msg = list.get(rnd.nextInt(list.size()));
        }
        if (msg != null) {
            msg = msg.replace("USERNAME", p.getName());
            msg = msg.replace("PREFIX", service.buildPrefix(p));
            e.setJoinMessage(ColorUtil.color(msg));
        }

        if (plugin.getDiscordBridge() != null) {
            plugin.getDiscordBridge().sendJoinLeaveTitle(p.getName(), true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        String msg = null;
        List<String> list = plugin.quitMessages;
        if (!list.isEmpty()) {
            msg = list.get(rnd.nextInt(list.size()));
        }
        if (msg != null) {
            msg = msg.replace("USERNAME", p.getName());
            msg = msg.replace("PREFIX", service.buildPrefix(p));
            e.setQuitMessage(ColorUtil.color(msg));
        }

        if (plugin.getDiscordBridge() != null) {
            plugin.getDiscordBridge().sendJoinLeaveTitle(p.getName(), false);
        }
    }
}
