package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.IWindow;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class Waterfall extends AbstractPipelineStep {
    private final CircularFloatBuffer outputBuffer;
    private final net.ellie.bolt.dsp.attributes.PipelineAttribute<IWindow> windowFunction;
    private final net.ellie.bolt.dsp.attributes.PipelineAttribute<Integer> fftSizeAttr;
    private DoubleFFT_1D fft;
    private int fftSize;

    public Waterfall(CircularFloatBuffer outputBuffer, net.ellie.bolt.dsp.attributes.PipelineAttribute<IWindow> windowFunction, net.ellie.bolt.dsp.attributes.PipelineAttribute<Integer> fftSizeAttr) {
        this.outputBuffer = outputBuffer;
        this.windowFunction = windowFunction;
        this.fftSizeAttr = fftSizeAttr;
        // fft, buffers will be initialized in process/reset
    }

    @Override
    public int process(double[] buffer, int length, net.ellie.bolt.dsp.DspPipeline pipeline) {
        IWindow win = windowFunction.resolve(pipeline);
        int fftSize = fftSizeAttr.resolve(pipeline);
        if (this.fft == null || this.fftSize != fftSize) {
            this.fftSize = fftSize;
            this.fft = new DoubleFFT_1D(fftSize);
        }
        double[] waterfallData = new double[fftSize * 2];
        System.arraycopy(buffer, 0, waterfallData, 0, length);
        win.applyComplex(waterfallData, length / 2);
        fft.complexForward(waterfallData);
        double[] magnitude = new double[fftSize];

        double gainCompensation = win.getGainCompensation(fftSize);
        double dbRange = Configuration.getWaterfallMaxDb() - Configuration.getWaterfallMinDb();
        double scaleFactor = gainCompensation / fftSize;
        double scaleFactorDb = 20.0 * Math.log10(scaleFactor);

        for (int i = 0; i < fftSize; i++) {
            double real = waterfallData[2 * i];
            double imag = waterfallData[2 * i + 1];
            double powerSquared = real * real + imag * imag;
            if (powerSquared < 1e-20) {
                powerSquared = 1e-20;
            }
            magnitude[i] = 10.0 * Math.log10(powerSquared) + scaleFactorDb;
            double normalizedDb = (magnitude[i] - Configuration.getWaterfallMinDb()) / dbRange;
            normalizedDb = Math.max(0.0, Math.min(1.0, normalizedDb));
            magnitude[i] = normalizedDb;
        }

        double[] shifted = new double[fftSize];
        int half = fftSize / 2;
        System.arraycopy(magnitude, half, shifted, 0, fftSize - half);
        System.arraycopy(magnitude, 0, shifted, fftSize - half, half);

        outputBuffer.writeNonBlocking(shifted, 0, fftSize);

        return length;
    }

    @Override
    public void reset() {
        this.fft = null;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.VISUALIZATION;
    }
}
