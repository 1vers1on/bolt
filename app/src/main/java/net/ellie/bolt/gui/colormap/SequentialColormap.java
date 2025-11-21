package net.ellie.bolt.gui.colormap;

import java.awt.Color;

public class SequentialColormap implements IColormap {
    private final Color[] colors;

    protected SequentialColormap(Color[] colors) {
        this.colors = colors;
    }

    @Override
    public Color getColor(float value) {
        if (value <= 0f) {
            return colors[0];
        } else if (value >= 1f) {
            return colors[colors.length - 1];
        } else {
            if (colors.length <= 1) {
                return colors[0];
            }

            float scaledValue = value * (colors.length - 1);
            int index = (int) Math.floor(scaledValue);
            float fraction = scaledValue - index;

            Color color1 = colors[index];
            Color color2 = colors[Math.min(index + 1, colors.length - 1)];
            int r = (int) (color1.getRed() + fraction * (color2.getRed() - color1.getRed()));
            int g = (int) (color1.getGreen() + fraction * (color2.getGreen() - color1.getGreen()));
            int b = (int) (color1.getBlue() + fraction * (color2.getBlue() - color1.getBlue()));
            return new Color(r, g, b);
        }
    }
}
