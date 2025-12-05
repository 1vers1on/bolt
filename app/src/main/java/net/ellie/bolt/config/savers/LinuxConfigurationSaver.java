package net.ellie.bolt.config.savers;

import java.nio.file.Path;

import net.ellie.bolt.config.ConfigurationSaver;

public class LinuxConfigurationSaver extends ConfigurationSaver {
    @Override
    protected void saveConfiguration(String configData) {
        Path configPath;
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isEmpty()) {
            configPath = Path.of(xdg, "bolt", "config.json");
        } else {
            String home = System.getProperty("user.home");
            configPath = Path.of(home, ".config", "bolt", "config.json");
        }

        try {
            java.nio.file.Files.createDirectories(configPath.getParent());
            java.nio.file.Files.writeString(configPath, configData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String loadConfiguration() {
        Path configPath;
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isEmpty()) {
            configPath = Path.of(xdg, "bolt", "config.json");
        } else {
            String home = System.getProperty("user.home");
            configPath = Path.of(home, ".config", "bolt", "config.json");
        }

        try {
            if (java.nio.file.Files.exists(configPath)) {
                return java.nio.file.Files.readString(configPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
