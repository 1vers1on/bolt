package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class Decimator extends AbstractPipelineStep {
    private final int decimationFactor;
    private final double[] taps;
    private final double[] delay;
    private int delayIdx;
    private int sampleCounter;

    public Decimator(int decimationFactor, double[] taps) {
        if (decimationFactor < 1) {
            throw new IllegalArgumentException("Decimation factor must be >= 1");
        }
        if (taps == null || taps.length == 0) {
            throw new IllegalArgumentException("Filter taps must be non-empty");
        }
        this.decimationFactor = decimationFactor;
        this.taps = taps.clone();
        this.delay = new double[taps.length];
        this.delayIdx = 0;
        this.sampleCounter = 0;
    }

    public Decimator(int decimationFactor, double inputSampleRate) {
        this(decimationFactor, designAntiAliasingFilter(decimationFactor, inputSampleRate));
    }

    public static double[] designAntiAliasingFilter(int decimationFactor, double inputSampleRate) {
        double outputSampleRate = inputSampleRate / decimationFactor;
        
        double cutoffHz = 0.4 * outputSampleRate;
        
        int filterLength = Math.min(127, Math.max(31, 4 * decimationFactor));
        if (filterLength % 2 == 0) filterLength++;
        
        return designHammingLowPass(filterLength, inputSampleRate, cutoffHz);
    }

    private static double[] designHammingLowPass(int M, double fs, double fc) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        
        double[] h = new double[M];
        int mid = M / 2;
        double normCut = fc / fs;
        
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 2.0 * normCut
                    : Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] = sinc * w;
        }
        
        double sum = 0.0;
        for (double v : h) sum += v;
        for (int n = 0; n < M; n++) h[n] /= sum;
        
        return h;
    }

    @Override
    public int process(double[] buffer, int length) {
        if (buffer == null) return 0;
        
        int L = taps.length;
        int outIdx = 0;

        for (int n = 0; n < length; n++) {
            delay[delayIdx] = buffer[n];

            if (sampleCounter == 0) {
                double acc = 0.0;
                int di = delayIdx;

                for (int k = 0; k < L; k++) {
                    acc += delay[di] * taps[k];
                    di--;
                    if (di < 0) di = L - 1;
                }

                buffer[outIdx++] = acc;
            }

            delayIdx++;
            if (delayIdx >= L) delayIdx = 0;

            sampleCounter++;
            if (sampleCounter >= decimationFactor) {
                sampleCounter = 0;
            }
        }

        return outIdx;
    }

    @Override
    public void reset() {
        for (int i = 0; i < delay.length; i++) delay[i] = 0.0;
        delayIdx = 0;
        sampleCounter = 0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.DECIMATOR;
    }

    public int getDecimationFactor() {
        return decimationFactor;
    }
}
