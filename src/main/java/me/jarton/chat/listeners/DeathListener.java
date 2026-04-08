package me.jarton.chat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.jarton.chat.JartonChat;

public class DeathListener implements Listener {

    private final JartonChat plugin;

    public DeathListener(JartonChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        String player = event.getEntity().getName();
        String deathMessage = event.getDeathMessage();

        if (deathMessage != null && plugin.getDiscordBridge() != null) {
            plugin.getDiscordBridge().sendDeathMessage(player, deathMessage);
        }
    }
}
