package net.ellie.bolt.util;

public class SystemInfo {
    public static OS getOperatingSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("mac")) {
            return OS.MAC;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OS.LINUX;
        } else {
            return OS.UNKNOWN;
        }
    }

    public static enum OS {
        WINDOWS,
        MAC,
        LINUX,
        UNKNOWN
    }
}
