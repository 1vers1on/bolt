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
    private double[] inputBuffer;
    private int inputBufferFill = 0;

    public Waterfall(CircularFloatBuffer outputBuffer, net.ellie.bolt.dsp.attributes.PipelineAttribute<IWindow> windowFunction, net.ellie.bolt.dsp.attributes.PipelineAttribute<Integer> fftSizeAttr) {
    this.outputBuffer = outputBuffer;
    this.windowFunction = windowFunction;
    this.fftSizeAttr = fftSizeAttr;
    this.inputBufferFill = 0;
    }

    @Override
    public int process(double[] buffer, int length, net.ellie.bolt.dsp.DspPipeline pipeline) {
        IWindow win = windowFunction.resolve(pipeline);
        int fftSize = fftSizeAttr.resolve(pipeline);
        if (this.fft == null || this.fftSize != fftSize) {
            this.fftSize = fftSize;
            this.fft = new DoubleFFT_1D(fftSize);
            this.inputBuffer = new double[fftSize * 2];
            this.inputBufferFill = 0;
        }
        int processed = 0;
        while (processed < length) {
            int toCopy = Math.min(fftSize * 2 - inputBufferFill, length - processed);
            System.arraycopy(buffer, processed, inputBuffer, inputBufferFill, toCopy);
            inputBufferFill += toCopy;
            processed += toCopy;

            if (inputBufferFill == fftSize * 2) {
                double[] waterfallData = new double[fftSize * 2];
                System.arraycopy(inputBuffer, 0, waterfallData, 0, fftSize * 2);
                win.applyComplex(waterfallData, fftSize);
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
                inputBufferFill = 0;
            }
        }
        return length;
    }

    @Override
    public void reset() {
    this.fft = null;
    this.inputBufferFill = 0;
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
