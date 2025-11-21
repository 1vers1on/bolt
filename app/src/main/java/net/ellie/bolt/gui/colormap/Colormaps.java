package net.ellie.bolt.gui.colormap;

import net.ellie.bolt.gui.colormap.colormaps.*;
import java.awt.Color;

public class Colormaps {
    public static Color mapValue(Colormap colormap, float value) {
        IColormap cmap = colormap.getInstance();
        return cmap.getColor(value);
    }

    public static enum Colormap {
        GRAYSCALE("Grayscale", new Grayscale()),
        VIRIDIS("Viridis", new Virdis()),
        PLASMA("Plasma", new Plasma()),
        INFERNO("Inferno", new Inferno()),
        MAGMA("Magma", new Magma()),
        CIVIDIS("Cividis", new Civdis()),
        TURBO("Turbo", new Turbo());

        public static final Colormap[] VALUES = values();
        private final String displayName;
        private final IColormap instance;

        Colormap(String displayName, IColormap instance) {
            this.displayName = displayName;
            this.instance = instance;
        }

        public String getDisplayName() {
            return displayName;
        }

        public IColormap getInstance() {
            return instance;
        }
    }
}
