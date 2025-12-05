package net.ellie.bolt.config.savers;

import java.nio.file.Path;

import net.ellie.bolt.config.ConfigurationSaver;

public class WindowsConfigurationSaver extends ConfigurationSaver {
    @Override
    protected void saveConfiguration(String configData) {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        Path configPath = Path.of(appData, "Bolt", "config.json");

        try {
            java.nio.file.Files.createDirectories(configPath.getParent());
            java.nio.file.Files.writeString(configPath, configData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String loadConfiguration() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        Path configPath = Path.of(appData, "Bolt", "config.json");

        try {
            return java.nio.file.Files.readString(configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
