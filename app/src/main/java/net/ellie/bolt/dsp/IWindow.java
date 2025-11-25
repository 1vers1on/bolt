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
}
