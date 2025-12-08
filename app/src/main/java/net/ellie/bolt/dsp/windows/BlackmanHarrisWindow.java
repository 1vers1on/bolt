package net.ellie.bolt.dsp.windows;

import java.util.concurrent.ConcurrentHashMap;

import net.ellie.bolt.dsp.IWindow;

public class BlackmanHarrisWindow implements IWindow {
    private static final ConcurrentHashMap<Integer, double[]> CACHE = new ConcurrentHashMap<>();
    private static final double A0 = 0.35875;
    private static final double A1 = 0.48829;
    private static final double A2 = 0.14128;
    private static final double A3 = 0.01168;

    public BlackmanHarrisWindow() {
    }

    @Override
    public void apply(double[] buffer, int length) {
        double[] coeffs = getCoefficients(length);
        for (int i = 0; i < length; i++) {
            buffer[i] *= coeffs[i];
        }
    }

    @Override
    public void applyComplex(double[] buffer, int complexLength) {
        double[] coeffs = getCoefficients(complexLength);
        for (int i = 0, bi = 0; i < complexLength; i++, bi += 2) {
            double w = coeffs[i];
            buffer[bi] *= w;      // real
            buffer[bi + 1] *= w;  // imag
        }
    }

    @Override
    public double[] getCoefficients(int length) {
        return CACHE.computeIfAbsent(length, len -> {
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                coeffs[i] = A0
                        - A1 * Math.cos((2 * Math.PI * i) / (len - 1))
                        + A2 * Math.cos((4 * Math.PI * i) / (len - 1))
                        - A3 * Math.cos((6 * Math.PI * i) / (len - 1));
            }
            return coeffs;
        });
    }

    @Override
    public String getName() {
        return "Blackman-Harris";
    }
}
