package me.jarton.chat.playerresolution;

import java.util.Comparator;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class PartialNickPlayerResolver implements PlayerResolver {
    private final Comparator<Player> playerComparator;

    public PartialNickPlayerResolver(Comparator<Player> playerComparator) {
        this.playerComparator = playerComparator;
    }

    @Override
    public Optional<Player> getPlayer(String name) {
        var player = Bukkit.getPlayerExact(name);
        if (player != null) {
            return Optional.of(player);
        }

        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> effectiveNameStartsWith(p, name))
            .map(p -> (Player) p)
            .sorted(playerComparator)
            .findFirst();
    }

    private static boolean effectiveNameStartsWith(Player player, String prefix) {
        var rawDisplayName = ChatColor.stripColor(player.getDisplayName());
        
        return startsWithIgnoreCase(rawDisplayName, prefix)
                || startsWithIgnoreCase(player.getName(), prefix);
    }

    private static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
