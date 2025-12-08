package net.ellie.bolt.dsp;

public interface IWindow {
    void apply(double[] buffer, int length);

    double[] getCoefficients(int length);

    String getName();

    default double getGainCompensation(int length) {
        double[] coeffs = getCoefficients(length);
        double sum = 0.0;
        for (double c : coeffs) {
            sum += c;
        }
        return length / sum;
    }

    /**
     * Apply the window to a complex interleaved buffer.
     * The buffer layout is [re0, im0, re1, im1, ...].
     * complexLength is the number of complex samples (i.e., re/im pairs).
     */
    default void applyComplex(double[] buffer, int complexLength) {
        double[] coeffs = getCoefficients(complexLength);
        int n = complexLength;
        for (int i = 0, bi = 0; i < n; i++, bi += 2) {
            double w = coeffs[i];
            buffer[bi] *= w;      // real
            buffer[bi + 1] *= w;  // imag
        }
    }
}
