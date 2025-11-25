package net.ellie.bolt.dsp.windows;

import net.ellie.bolt.dsp.IWindow;

import java.util.concurrent.ConcurrentHashMap;

public class HannWindow implements IWindow {
    private static final ConcurrentHashMap<Integer, double[]> CACHE = new ConcurrentHashMap<>();

    public HannWindow() {
    }

    @Override
    public void apply(double[] buffer, int length) {
        double[] coeffs = getCoefficients(length);
        for (int i = 0; i < length; i++) {
            buffer[i] *= coeffs[i];
        }
    }

    @Override
    public double[] getCoefficients(int length) {
        return CACHE.computeIfAbsent(length, len -> {
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                coeffs[i] = 0.5 * (1 - Math.cos((2 * Math.PI * i) / (len - 1)));
            }
            return coeffs;
        });
    }

    @Override
    public String getName() {
        return "Hann";
    }
}
