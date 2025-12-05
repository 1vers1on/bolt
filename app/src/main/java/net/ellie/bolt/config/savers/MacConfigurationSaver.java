package net.ellie.bolt.config.savers;

import java.nio.file.Path;

import net.ellie.bolt.config.ConfigurationSaver;

public class MacConfigurationSaver extends ConfigurationSaver {
    @Override
    protected void saveConfiguration(String configData) {
        String home = System.getProperty("user.home");
        Path configPath = Path.of(home, "Library", "Application Support", "Bolt", "config.json");

        try {
            java.nio.file.Files.createDirectories(configPath.getParent());
            java.nio.file.Files.writeString(configPath, configData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String loadConfiguration() {
        String home = System.getProperty("user.home");
        Path configPath = Path.of(home, "Library", "Application Support", "Bolt", "config.json");

        try {
            return java.nio.file.Files.readString(configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
