package net.ellie.bolt.gui.colormap.colormaps;

import java.awt.Color;

import net.ellie.bolt.gui.colormap.IColormap;

public class Grayscale implements IColormap {
    @Override
    public Color getColor(float value) {
        int gray = Math.min(255, Math.max(0, (int) (value * 255)));
        return new Color(gray, gray, gray);
    }
}
