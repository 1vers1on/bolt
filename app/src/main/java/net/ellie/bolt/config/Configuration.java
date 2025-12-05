package net.ellie.bolt.config;

import net.ellie.bolt.gui.colormap.Colormaps.Colormap;

public class Configuration {
    private Colormap colormap = Colormap.VIRIDIS;
    private String inputDevice = "Audio";
    private int fftSize = 4096;
    private int msaaSamples = 4;
    private double waterfallMinDb = -60.0;
    private double waterfallMaxDb = 6.0;
    private double zoomLevel = 1.0; // TODO: implement zooming
    private String audioOutputDevice = "Default";
    private int targetFrequency = 101_500_000; // 101.5 MHz

    private RTLSDRConfig rtlSdrConfig = new RTLSDRConfig();
    private DummyInputConfig dummyConfig = new DummyInputConfig(2048000);

    private int audioBufferSize = 8192;
    private int sampleRate = 48000;

    private static ConfigurationSaver saver = ConfigurationSaver.create();
    public static Configuration INSTANCE = saver.load();

    public static Colormap getColormap() {
        return INSTANCE.colormap;
    }

    public static void setColormap(Colormap newColormap) {
        INSTANCE.colormap = newColormap;
        saver.save(INSTANCE);
    }

    public static String getInputDevice() {
        return INSTANCE.inputDevice;
    }

    public static void setInputDevice(String newInputDevice) {
        INSTANCE.inputDevice = newInputDevice;
        saver.save(INSTANCE);
    }

    public static int getFftSize() {
        return INSTANCE.fftSize;
    }

    public static void setFftSize(int newFftSize) {
        INSTANCE.fftSize = newFftSize;
        saver.save(INSTANCE);
    }

    public static int getMsaaSamples() {
        return INSTANCE.msaaSamples;
    }

    public static void setMsaaSamples(int newMsaaSamples) {
        INSTANCE.msaaSamples = newMsaaSamples;
        saver.save(INSTANCE);
    }

    public static RTLSDRConfig getRtlSdrConfig() {
        return INSTANCE.rtlSdrConfig;
    }

    public static void setRtlSdrConfig(RTLSDRConfig newRtlSdrConfig) {
        INSTANCE.rtlSdrConfig = newRtlSdrConfig;
        saver.save(INSTANCE);
    }

    public static double getWaterfallMinDb() {
        return INSTANCE.waterfallMinDb;
    }

    public static void setWaterfallMinDb(double newWaterfallMinDb) {
        INSTANCE.waterfallMinDb = newWaterfallMinDb;
        saver.save(INSTANCE);
    }

    public static double getWaterfallMaxDb() {
        return INSTANCE.waterfallMaxDb;
    }

    public static void setWaterfallMaxDb(double newWaterfallMaxDb) {
        INSTANCE.waterfallMaxDb = newWaterfallMaxDb;
        saver.save(INSTANCE);
    }

    public static DummyInputConfig getDummyConfig() {
        return INSTANCE.dummyConfig;
    }

    public static void setDummyConfig(DummyInputConfig newDummyConfig) {
        INSTANCE.dummyConfig = newDummyConfig;
        saver.save(INSTANCE);
    }

    public static double getZoomLevel() {
        return INSTANCE.zoomLevel;
    }

    public static void setZoomLevel(double newZoomLevel) {
        INSTANCE.zoomLevel = newZoomLevel;
        saver.save(INSTANCE);
    }

    public static String getAudioOutputDevice() {
        return INSTANCE.audioOutputDevice;
    }

    public static void setAudioOutputDevice(String newAudioOutputDevice) {
        INSTANCE.audioOutputDevice = newAudioOutputDevice;
        saver.save(INSTANCE);
    }

    public static int getTargetFrequency() {
        return INSTANCE.targetFrequency;
    }

    public static void setTargetFrequency(int newTargetFrequency) {
        INSTANCE.targetFrequency = newTargetFrequency;
        saver.save(INSTANCE);
    }

    public static int getAudioBufferSize() {
        return INSTANCE.audioBufferSize;
    }

    public static void setAudioBufferSize(int newAudioBufferSize) {
        INSTANCE.audioBufferSize = newAudioBufferSize;
        saver.save(INSTANCE);
    }

    public static int getSampleRate() {
        return INSTANCE.sampleRate;
    }

    public static void setSampleRate(int newSampleRate) {
        INSTANCE.sampleRate = newSampleRate;
        saver.save(INSTANCE);
    }
}
