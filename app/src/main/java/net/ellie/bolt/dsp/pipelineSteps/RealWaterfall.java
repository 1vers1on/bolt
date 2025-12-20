package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.IWindow;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;


public class RealWaterfall extends AbstractPipelineStep {
    private final CircularFloatBuffer outputBuffer;
    private final DoubleFFT_1D fft;
    private final IWindow windowFunction;
    private final int fftSize;
    private final double[] work;
    private final double[] magnitude;
    private final double[] inputBuffer;
    private int inputBufferFill = 0;

    public RealWaterfall(CircularFloatBuffer outputBuffer, IWindow windowFunction, int fftSize) {
        this.outputBuffer = outputBuffer;
        this.fft = new DoubleFFT_1D(fftSize);
        this.windowFunction = windowFunction;
        this.fftSize = fftSize;
        this.work = new double[fftSize];
        this.magnitude = new double[fftSize];
        this.inputBuffer = new double[fftSize];
        this.inputBufferFill = 0;
    }

    @Override
    public int process(double[] buffer, int length) {
        int processed = 0;
        while (processed < length) {
            int toCopy = Math.min(fftSize - inputBufferFill, length - processed);
            System.arraycopy(buffer, processed, inputBuffer, inputBufferFill, toCopy);
            inputBufferFill += toCopy;
            processed += toCopy;

            if (inputBufferFill == fftSize) {
                System.arraycopy(inputBuffer, 0, work, 0, fftSize);
                windowFunction.apply(work, fftSize);
                fft.realForward(work);

                int bins = fftSize;

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
                inputBufferFill = 0;
            }
        }
        return length;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public void reset() {
        inputBufferFill = 0;
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.VISUALIZATION;
    }
}
