package net.ellie.bolt;

import net.ellie.bolt.gui.colormap.Colormaps.Colormap;

public class Configuration {
    private static Colormap colormap = Colormap.VIRIDIS;

    public static Colormap getColormap() {
        return colormap;
    }

    public static void setColormap(Colormap newColormap) {
        colormap = newColormap;
    }
}
