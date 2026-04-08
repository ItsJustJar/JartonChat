package me.jarton.chat.players;

import java.util.Optional;
import java.util.UUID;

public interface ChatPlayerRepository {
    Optional<ChatPlayer> getByPlayerId(UUID playerId);

    void save(ChatPlayer chatPlayer);
}
