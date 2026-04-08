package me.jarton.chat.players.yaml;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedTest;

import com.jellyrekt.storage.fileconfiguration.FileConfigurationProvider;

import me.jarton.chat.players.ChatPlayer;
import me.jarton.chat.players.yaml.YamlChatPlayerRepository;

public class YamlChatPlayerRepositoryTests {

    static ChatPlayer createChatPlayer(UUID playerId, String colorCode) {
        var player = new ChatPlayer(playerId);
        player.setColorCode(colorCode);
        return player;
    }

    @Test
    void testLoadFromYaml() throws Exception {
        var config = new YamlConfiguration();
        var playerId = UUID.randomUUID();
        var colorCode = "#19a2e1";
        config.loadFromString(String.format("%s:\n  color-code: '%s'", playerId, colorCode));
        var configProvider = new TestConfigurationProvider(config);
        var repository = new YamlChatPlayerRepository(configProvider);

        repository.loadFromYaml();

        var player = repository.getByPlayerId(playerId).orElseThrow();

        Assertions.assertEquals(playerId, player.getPlayerId());
        Assertions.assertEquals(colorCode, player.getColorCode());
    }

    @Test
    void testFlush() throws Exception {
        var player = new ChatPlayer(UUID.randomUUID());
        player.setColorCode("#19a2e1");
        var config = new YamlConfiguration();
        var configProvider = new TestConfigurationProvider(config);
        var repository = new YamlChatPlayerRepository(configProvider);

        repository.save(player);
        repository.flush();

        Assertions.assertTrue(config.contains(player.getPlayerId().toString()));
        Assertions.assertEquals(player.getColorCode(),
                config.getString(player.getPlayerId().toString() + ".color-code"));
    }
    
    class TestConfigurationProvider implements FileConfigurationProvider {
        private final FileConfiguration config;

        public TestConfigurationProvider(FileConfiguration config) {
            this.config = config;
        }

        @Override
        public FileConfiguration getFileConfiguration() {
            return config;
        }

        @Override
        public void reload() throws IOException { }

        @Override
        public void save() throws IOException { }

    }
}
