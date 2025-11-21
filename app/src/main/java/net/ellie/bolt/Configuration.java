package net.ellie.bolt;

import net.ellie.bolt.gui.colormap.Colormaps.Colormap;

public class Configuration {
    static Colormap colormap = Colormap.VIRIDIS;
    static String inputDevice = "Audio";
    static int fftSize = 4096;

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
}
