package net.ellie.bolt.config;

import net.ellie.bolt.gui.colormap.Colormaps.Colormap;

public class Configuration {
    private static Colormap colormap = Colormap.VIRIDIS;
    private static String inputDevice = "Audio";
    private static int fftSize = 4096;
    private static int msaaSamples = 4;
    private static RTLSDRConfig rtlSdrConfig = new RTLSDRConfig();
    private static double waterfallMinDb = -60.0;
    private static double waterfallMaxDb = 6.0;

    public static Colormap getColormap() {
        return colormap;
    }

    public static void setColormap(Colormap newColormap) {
        colormap = newColormap;
    }

    public static String getInputDevice() {
        return inputDevice;
    }

    public static void setInputDevice(String newInputDevice) {
        inputDevice = newInputDevice;
    }

    public static int getFftSize() {
        return fftSize;
    }

    public static void setFftSize(int newFftSize) {
        fftSize = newFftSize;
    }

    public static int getMsaaSamples() {
        return msaaSamples;
    }

    public static void setMsaaSamples(int newMsaaSamples) {
        msaaSamples = newMsaaSamples;
    }

    public static RTLSDRConfig getRtlSdrConfig() {
        return rtlSdrConfig;
    }

    public static void setRtlSdrConfig(RTLSDRConfig newRtlSdrConfig) {
        rtlSdrConfig = newRtlSdrConfig;
    }

    public static double getWaterfallMinDb() {
        return waterfallMinDb;
    }

    public static void setWaterfallMinDb(double newWaterfallMinDb) {
        waterfallMinDb = newWaterfallMinDb;
    }

    public static double getWaterfallMaxDb() {
        return waterfallMaxDb;
    }

    public static void setWaterfallMaxDb(double newWaterfallMaxDb) {
        waterfallMaxDb = newWaterfallMaxDb;
    }
}
