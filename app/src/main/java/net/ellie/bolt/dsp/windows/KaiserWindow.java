package net.ellie.bolt.dsp.windows;

import java.util.concurrent.ConcurrentHashMap;

import net.ellie.bolt.dsp.IWindow;

import org.apache.commons.math3.special.BesselJ;

public class KaiserWindow implements IWindow {
    private final ConcurrentHashMap<Integer, double[]> CACHE = new ConcurrentHashMap<>();
    private final double beta;

    public KaiserWindow(double beta) {
        this.beta = beta;
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
            double denom = BesselJ.value(0, beta);

            for (int n = 0; n < len; n++) {
                double ratio = (2.0 * n) / (len - 1) - 1.0;
                double val = beta * Math.sqrt(1 - ratio * ratio);
                coeffs[n] = BesselJ.value(0, val) / denom;
            }
            return coeffs;
        });
    }

    @Override
    public String getName() {
        return "Kaiser (β=" + beta + ")";
    }
}
