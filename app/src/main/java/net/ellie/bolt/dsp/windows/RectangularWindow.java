package net.ellie.bolt.dsp.windows;

import net.ellie.bolt.dsp.IWindow;

public class RectangularWindow implements IWindow {
    public RectangularWindow() {
    }

    @Override
    public void apply(double[] buffer, int length) {
        // rectangular window does not modify the buffer
    }

    @Override
    public void applyComplex(double[] buffer, int complexLength) {
        // rectangular window does not modify the buffer
    }

    @Override
    public double[] getCoefficients(int length) {
        double[] coeffs = new double[length];
        for (int i = 0; i < length; i++) {
            coeffs[i] = 1.0;
        }
        return coeffs;
    }

    @Override
    public String getName() {
        return "Rectangular";
    }
}
