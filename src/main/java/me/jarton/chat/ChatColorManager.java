package me.jarton.chat;

import org.bukkit.entity.Player;

import me.jarton.chat.players.ChatPlayer;
import me.jarton.chat.players.ChatPlayerRepository;

public class ChatColorManager {

    private final ChatPlayerRepository chatPlayerRepository;

    public ChatColorManager(ChatPlayerRepository chatPlayerRepository) {
        this.chatPlayerRepository = chatPlayerRepository;
    }

    public void setColor(Player player, String colorCode) {
        if (player == null) {
            return;
        }
        var chatPlayer = chatPlayerRepository.getByPlayerId(player.getUniqueId())
            .orElse(new ChatPlayer(player.getUniqueId()));
        chatPlayer.setColorCode(colorCode);
        chatPlayerRepository.save(chatPlayer);
    }

    public void clearColor(Player player) {
        setColor(player, null);
    }

    public String getOverride(Player player) {
        if (player == null) {
            return null;
        }

        return chatPlayerRepository.getByPlayerId(player.getUniqueId())
            .map(chatPlayer -> chatPlayer.getColorCode())
            .orElse(null);
    }

    public String getEffectiveMessageColor(Player player, String baseColor) {
        if (baseColor == null || baseColor.isEmpty()) {
            baseColor = "&7";
        }

        if (player == null) {
            return baseColor;
        }

        if (!player.hasPermission("jartonchat.chatcolor")) {
            return baseColor;
        }

        String override = getOverride(player);
        if (override != null && !override.isEmpty()) {
            return override;
        }

        return baseColor;
    }
}
