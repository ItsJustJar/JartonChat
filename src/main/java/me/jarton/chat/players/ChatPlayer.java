package me.jarton.chat.players;

import java.util.UUID;

public class ChatPlayer {
    private final UUID playerId;
    private String colorCode;

    public ChatPlayer(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }
}
