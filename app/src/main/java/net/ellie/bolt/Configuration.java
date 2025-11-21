package net.ellie.bolt;

import net.ellie.bolt.gui.colormap.Colormaps.Colormap;

public class Configuration {
    static Colormap colormap = Colormap.VIRIDIS;
    static String inputDevice = "Audio";

    public static Colormap getColormap() {
        return colormap;
    }

    public static void setColormap(Colormap newColormap) {
        colormap = newColormap;
    }
}
