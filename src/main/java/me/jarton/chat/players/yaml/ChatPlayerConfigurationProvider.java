package me.jarton.chat.players.yaml;

import java.io.File;
import java.io.IOException;

import com.jellyrekt.storage.fileconfiguration.yaml.YamlConfigurationProvider;

public class ChatPlayerConfigurationProvider extends YamlConfigurationProvider {

    public ChatPlayerConfigurationProvider(File parent, String fileName) throws IOException {
        super(parent, fileName);
    }
    
}
