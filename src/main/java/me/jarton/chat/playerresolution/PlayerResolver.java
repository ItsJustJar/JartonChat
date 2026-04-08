package me.jarton.chat.playerresolution;

import java.util.Optional;

import org.bukkit.entity.Player;

public interface PlayerResolver {
    Optional<Player> getPlayer(String name);
}
