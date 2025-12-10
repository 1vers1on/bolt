package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.IWindow;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class RealWaterfall implements IPipelineStep {
    private final CircularFloatBuffer outputBuffer;
    private final DoubleFFT_1D fft;
    private final IWindow windowFunction;
    private final int fftSize;
    // Reuse buffers to avoid frequent allocations and GC stalls
    private final double[] work;
    private final double[] magnitude;

    public RealWaterfall(CircularFloatBuffer outputBuffer, IWindow windowFunction, int fftSize) {
        this.outputBuffer = outputBuffer;
        this.fft = new DoubleFFT_1D(fftSize);
        this.windowFunction = windowFunction;
        this.fftSize = fftSize;
        this.work = new double[fftSize];
        // Allocate up to fftSize bins to allow returning n bins per process call
        this.magnitude = new double[fftSize];
    }

    @Override
    public int process(double[] buffer, int length) {
        int n = Math.min(length, fftSize);
        System.arraycopy(buffer, 0, work, 0, n);

        windowFunction.apply(work, n);

        if (n < fftSize) {
            for (int i = n; i < fftSize; i++) {
                work[i] = 0.0;
            }
        }

        fft.realForward(work);

        int bins = n;

        double gainCompensation = windowFunction.getGainCompensation(fftSize);
        double dbRange = Configuration.getWaterfallMaxDb() - Configuration.getWaterfallMinDb();
        double scaleFactor = gainCompensation / fftSize;
        double scaleFactorDb = 20.0 * Math.log10(scaleFactor);

        double dc = Math.abs(work[0]);
        double dcPower = dc * dc;
        if (dcPower < 1e-20)
            dcPower = 1e-20;
        double dcDb = 10.0 * Math.log10(dcPower) + scaleFactorDb;
        double dcNorm = (dcDb - Configuration.getWaterfallMinDb()) / dbRange;
        magnitude[0] = Math.max(0.0, Math.min(1.0, dcNorm));

        for (int k = 1; k < bins; k++) {
            int reIndex = 2 * k;
            int imIndex = reIndex + 1;
            if (imIndex < work.length) {
                double re = work[reIndex];
                double im = work[imIndex];
                double powerSquared = re * re + im * im;
                if (powerSquared < 1e-20)
                    powerSquared = 1e-20;
                double db = 10.0 * Math.log10(powerSquared) + scaleFactorDb;
                double norm = (db - Configuration.getWaterfallMinDb()) / dbRange;
                magnitude[k] = Math.max(0.0, Math.min(1.0, norm));
            } else {
                magnitude[k] = 0.0;
            }
        }

        outputBuffer.writeNonBlocking(magnitude, 0, bins);

        return n;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public void reset() {
        // No internal state to reset for the real waterfall
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.VISUALIZATION;
    }
}
