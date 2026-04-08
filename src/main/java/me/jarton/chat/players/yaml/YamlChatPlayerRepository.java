package me.jarton.chat.players.yaml;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;

import com.jellyrekt.storage.fileconfiguration.FileConfigurationProvider;
import com.jellyrekt.storage.fileconfiguration.FileConfigurationStorage;

import me.jarton.chat.players.ChatPlayer;
import me.jarton.chat.players.ChatPlayerRepository;

public class YamlChatPlayerRepository extends FileConfigurationStorage implements ChatPlayerRepository {
    private final Map<UUID, ChatPlayer> chatPlayers = new ConcurrentHashMap<>();

    public YamlChatPlayerRepository(FileConfigurationProvider configurationProvider) {
        super(configurationProvider);
    }

    @Override
    public Optional<ChatPlayer> getByPlayerId(UUID playerId) {
        return Optional.ofNullable(chatPlayers.get(playerId));
    }

    @Override
    public void save(ChatPlayer chatPlayer) {
        chatPlayers.put(chatPlayer.getPlayerId(), chatPlayer);
        notifyChange();
    }
    
    public void loadFromYaml() {
        chatPlayers.clear();
        var config = getFileConfiguration();
        var keys = config.getKeys(false);
        for (var key : keys) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            var section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            var player = deserialize(playerId, section);
            chatPlayers.put(playerId, player);
        }
    }

    private ChatPlayer deserialize(UUID playerId, ConfigurationSection section) {
        var player = new ChatPlayer(playerId);
        player.setColorCode(section.getString("color-code"));
        return player;
    }

    public void flush() {
        clearRecords();
        var config = getFileConfiguration();
        for (var entry : chatPlayers.entrySet()) {
            var playerId = entry.getKey();
            var player = entry.getValue();
            config.set(playerId.toString() + ".color-code", player.getColorCode());
        }
    }

    private void clearRecords() {
        var config = getFileConfiguration();
        for (var key : config.getKeys(false)) {
            config.set(key, null);
        }
    }
}
