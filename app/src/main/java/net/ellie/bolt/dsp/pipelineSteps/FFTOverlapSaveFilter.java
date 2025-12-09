package net.ellie.bolt.dsp.pipelineSteps;

import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

import org.apache.commons.math3.util.Pair;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class FFTOverlapSaveFilter implements IPipelineStep {
    private final int fftSize;
    private final int hopSize;
    private final int filterLen;

    private final double[] H;

    private final double[] overlap;

    private final double[] X;
    private final double[] Y;

    private final DoubleFFT_1D fft;

    public FFTOverlapSaveFilter(double[] taps, int fftSize) {
        if (taps == null || taps.length == 0) throw new IllegalArgumentException("taps must be non-empty");
        if (fftSize <= 0 || (fftSize & (fftSize - 1)) != 0) throw new IllegalArgumentException("fftSize must be power of two > 0");
        this.filterLen = taps.length;
        this.fftSize = fftSize;
        this.hopSize = Math.max(1, fftSize - (filterLen - 1));

        if (filterLen > fftSize) {
            throw new IllegalArgumentException("filterLen must be <= fftSize for overlap-save");
        }

        this.H = new double[2 * fftSize];
        this.overlap = new double[2 * (filterLen - 1)];
        this.X = new double[2 * fftSize];
        this.Y = new double[2 * fftSize];
        this.fft = new DoubleFFT_1D(fftSize);

        double[] hTime = new double[2 * fftSize];
        for (int n = 0; n < filterLen; n++) {
            hTime[2 * n] = taps[n];
            hTime[2 * n + 1] = 0.0;
        }

        System.arraycopy(hTime, 0, H, 0, hTime.length);
        fft.complexForward(H);
    }

    @Override
    public int process(double[] buffer, int length) {
        if ((length & 1) != 0) throw new IllegalArgumentException("length must be even (complex interleaved)");
        int N = length / 2;
        if (N < fftSize) {
            return 0;
        }

        int producedComplex = 0;
        int inIndex = 0;
        while (inIndex + fftSize <= N) {
            int Lm1 = filterLen - 1;
            int copyPrefix = Math.min(Lm1, fftSize);
            for (int i = 0; i < copyPrefix; i++) {
                X[2 * i] = overlap[2 * i];
                X[2 * i + 1] = overlap[2 * i + 1];
            }

            int rem = fftSize - copyPrefix;
            for (int i = 0; i < rem; i++) {
                int srcIdx = inIndex + i;
                X[2 * (copyPrefix + i)] = buffer[2 * srcIdx];
                X[2 * (copyPrefix + i) + 1] = buffer[2 * srcIdx + 1];
            }

            System.arraycopy(X, 0, Y, 0, 2 * fftSize);
            fft.complexForward(Y);

            for (int k = 0; k < fftSize; k++) {
                double xr = Y[2 * k], xi = Y[2 * k + 1];
                double hr = H[2 * k], hi = H[2 * k + 1];
                double yr = xr * hr - xi * hi;
                double yi = xr * hi + xi * hr;
                Y[2 * k] = yr;
                Y[2 * k + 1] = yi;
            }

            fft.complexInverse(Y, true);

            int start = Lm1;
            int emit = Math.min(hopSize, fftSize - start);
            for (int i = 0; i < emit; i++) {
                int dst = inIndex + i;
                int src = start + i;
                buffer[2 * dst] = Y[2 * src];
                buffer[2 * dst + 1] = Y[2 * src + 1];
            }

            producedComplex += emit;

            for (int i = 0; i < Lm1; i++) {
                int src = fftSize - Lm1 + i;
                overlap[2 * i] = Y[2 * src];
                overlap[2 * i + 1] = Y[2 * src + 1];
            }

            inIndex += hopSize;
        }

        return producedComplex * 2;
    }

    @Override
    public void reset() {
        for (int i = 0; i < overlap.length; i++) overlap[i] = 0.0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }
}
