package net.ellie.bolt.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ellie.bolt.config.savers.LinuxConfigurationSaver;
import net.ellie.bolt.config.savers.MacConfigurationSaver;
import net.ellie.bolt.config.savers.WindowsConfigurationSaver;
import net.ellie.bolt.util.SystemInfo;


public abstract class ConfigurationSaver {
    protected abstract void saveConfiguration(String configData);
    protected abstract String loadConfiguration();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static ConfigurationSaver create() {
        SystemInfo.OS os = SystemInfo.getOperatingSystem();
        switch (os) {
            case LINUX:
                return new LinuxConfigurationSaver();
            case WINDOWS:
                return new WindowsConfigurationSaver();
            case MAC:
                return new MacConfigurationSaver();
            case UNKNOWN:
                break;
        }

        throw new UnsupportedOperationException("Unsupported operating system: " + os);
    }

    public void save(Configuration config) {
        String json = gson.toJson(config);
        saveConfiguration(json);
    }

    public Configuration load() {
        String json = loadConfiguration();
        if (json != null) {
            return gson.fromJson(json, Configuration.class);
        }
        return new Configuration();
    }
}
