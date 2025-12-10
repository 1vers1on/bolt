package net.ellie.bolt.dsp;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;
import net.ellie.bolt.dsp.pipelineSteps.WAVRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DspThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DspThread.class);

    private final CircularFloatBuffer inputBuffer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private CircularFloatBuffer audioOutputBuffer;

    private List<IPipelineStep> pipelineSteps = new ArrayList<>();

    private final NumberType inputType;

    private boolean pipelineValid = false;

    private final int inputBufferSize;

    private Thread localDspThread = null;

    public DspThread(CircularFloatBuffer inputBuffer, int inputBufferSize, NumberType inputType) {
        this.inputBuffer = inputBuffer;
        this.audioOutputBuffer = new CircularFloatBuffer(Configuration.getAudioBufferSize());
        this.inputBufferSize = inputBufferSize;
        this.inputType = inputType;
    }

    public void start() {
        running.set(true);
        localDspThread = new Thread(this, "DspThread");
        localDspThread.start();
    }

    @Override
    public void run() {
        logger.info("DspThread started");
        running.set(true);

        double[] pipelineBuf = new double[inputBufferSize];

        while (running.get()) {
            try {
                inputBuffer.read(pipelineBuf, 0, inputBufferSize);
            } catch (InterruptedException e) {
                running.set(false);
                logger.error("DspThread interrupted during read", e);
                break;
            }

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

    public IDemodulator getDemodulator() {
        for (IPipelineStep step : pipelineSteps) {
            if (step instanceof IDemodulator) {
                return (IDemodulator) step;
            }
        }
        return null;
    }
    
    public WAVRecorder getWAVRecorder() {
        for (IPipelineStep step : pipelineSteps) {
            if (step instanceof WAVRecorder) {
                return (WAVRecorder) step;
            }
        }
        return null;
    }

    public void stop() {
        running.set(false);
        if (localDspThread != null) {
            try {
                localDspThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("DspThread stopped");
    }

    public CircularFloatBuffer getAudioOutputBuffer() {
        return audioOutputBuffer;
    }

    private void validatePipeline() {
        NumberType previousType = inputType;
        
        for (IPipelineStep step : pipelineSteps) {
            var types = step.getInputOutputType();
            NumberType stepInputType = types.getFirst();
            NumberType stepOutputType = types.getSecond();

            if (stepInputType != previousType) {
                throw new IllegalStateException("Pipeline step input type " + stepInputType +
                        " does not match previous output type " + previousType);
            }

            previousType = stepOutputType;
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
