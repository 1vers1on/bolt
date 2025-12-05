package net.ellie.bolt.dsp;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;
import net.ellie.bolt.dsp.pipelineSteps.FrequencyShifter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DspThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DspThread.class);

    private final CircularFloatBuffer inputBuffer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private DoubleFFT_1D fft;
    private IWindow windowFunction;
    private CircularFloatBuffer waterfallOutputBuffer;
    private CircularFloatBuffer audioOutputBuffer;

    private List<IPipelineStep> pipelineSteps = new ArrayList<>();

    private final NumberType inputType = NumberType.COMPLEX; // TODO: make configurable

    private boolean pipelineValid = false;

    public DspThread(CircularFloatBuffer inputBuffer, IWindow windowFunction) {
        this.inputBuffer = inputBuffer;
        this.fft = new DoubleFFT_1D(Configuration.getFftSize());
        this.waterfallOutputBuffer = new CircularFloatBuffer(Configuration.getFftSize());
        this.audioOutputBuffer = new CircularFloatBuffer(Configuration.getAudioBufferSize());
        this.windowFunction = windowFunction;
    }

    public void start() {
        running.set(true);
        new Thread(this, "DspThread").start();
    }

    @Override
    public void run() {
        logger.info("DspThread started");
        running.set(true);

        int n = Configuration.getFftSize();
        double[] fftBuf = new double[n * 2];

        while (running.get()) {
            try {
                inputBuffer.read(fftBuf, 0, n * 2);
            } catch (InterruptedException e) {
                running.set(false);
                logger.error("DspThread interrupted during read", e);
                break;
            }

            double[] pipelineBuf = fftBuf;

            windowFunction.apply(fftBuf, n);

            fft.complexForward(fftBuf);

            double[] magnitude = new double[n];
            double gain = windowFunction.getGainCompensation(n);
            double dbRange = Configuration.getWaterfallMaxDb() - Configuration.getWaterfallMinDb();
            for (int i = 0; i < n; i++) {
                double real = fftBuf[2 * i];
                double imag = fftBuf[2 * i + 1];
                magnitude[i] = 10.0 * Math.log10(real * real + imag * imag) + 20.0 * Math.log10(gain);

                double normalizedDb = (magnitude[i] - Configuration.getWaterfallMinDb()) / dbRange;
                normalizedDb = Math.max(0.0, Math.min(1.0, normalizedDb));
                magnitude[i] = (float) normalizedDb;
            }

            double[] shifted = new double[n];
            int half = n / 2;
            System.arraycopy(magnitude, half, shifted, 0, n - half);
            System.arraycopy(magnitude, 0, shifted, n - half, half);

            waterfallOutputBuffer.writeNonBlocking(shifted, 0, n);

            if (pipelineValid) {
                int pipelineBufLength = pipelineBuf.length;
                for (IPipelineStep step : pipelineSteps) {
                    pipelineBufLength = step.process(pipelineBuf, pipelineBufLength);
                }

                if (pipelineSteps.get(pipelineSteps.size() - 1).getInputOutputType().getSecond() == NumberType.REAL) {
                    float[] audioBuf = new float[pipelineBufLength];
                    for (int i = 0; i < pipelineBufLength; i++) {
                        audioBuf[i] = (float) pipelineBuf[i];
                    }
                    audioOutputBuffer.writeNonBlocking(audioBuf, 0, pipelineBufLength);
                }
            }
        }
    }

    public void stop() {
        running.set(false);
    }

    public CircularFloatBuffer getWaterfallOutputBuffer() {
        return waterfallOutputBuffer;
    }

    public CircularFloatBuffer getAudioOutputBuffer() {
        return audioOutputBuffer;
    }

    private void validatePipeline() {
        NumberType previousType = inputType;
        boolean hasFrequencyShift = false;

        for (IPipelineStep step : pipelineSteps) {
            var types = step.getInputOutputType();
            NumberType stepInputType = types.getFirst();
            NumberType stepOutputType = types.getSecond();

            if (stepInputType != previousType) {
                throw new IllegalStateException("Pipeline step input type " + stepInputType +
                        " does not match previous output type " + previousType);
            }

            if (step instanceof FrequencyShifter) {
                hasFrequencyShift = true;
            }

            previousType = stepOutputType;
        }

        if (!hasFrequencyShift) {
            throw new IllegalStateException("Pipeline must contain at least one FrequencyShifter step");
        }
    }

    public void clearPipeline() {
        pipelineValid = false;
        pipelineSteps = new ArrayList<>();
    }

    public void addPipelineStep(IPipelineStep step) {
        pipelineSteps.add(step);
        pipelineValid = false;
    }

    public void buildPipeline() {
        try {
            validatePipeline();
            pipelineValid = true;
        } catch (IllegalStateException e) {
            logger.error("Invalid DSP pipeline configuration", e);
            clearPipeline();
        }
    }

    public List<IPipelineStep> getPipelineSteps() {
        return pipelineSteps;
    }
}
