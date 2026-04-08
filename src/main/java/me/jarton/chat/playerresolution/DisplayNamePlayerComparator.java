package me.jarton.chat.playerresolution;

import java.util.Comparator;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class DisplayNamePlayerComparator implements Comparator<Player> {
    private static final Comparator<Player> playerComparator = Comparator
            .comparingInt((Player p) -> ChatColor.stripColor(p.getDisplayName()).length())
            .thenComparing(p -> !p.getDisplayName().equals(p.getName()) ? 1 : 0)
            .thenComparing(p -> p.getName())
            .thenComparing(p -> ChatColor.stripColor(p.getDisplayName()));

    @Override
    public int compare(Player o1, Player o2) {
        return playerComparator.compare(o1, o2);
    }
    
}
