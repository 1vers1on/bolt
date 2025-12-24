package net.ellie.bolt.dsp;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DspThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DspThread.class);

    private final CircularFloatBuffer inputBuffer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private CircularFloatBuffer audioOutputBuffer;
    private final DspPipeline pipeline;
    private final int inputBufferSize;
    private Thread localDspThread = null;

    public DspThread(CircularFloatBuffer inputBuffer, int inputBufferSize, NumberType inputType) {
        this.inputBuffer = inputBuffer;
        this.audioOutputBuffer = new CircularFloatBuffer(Configuration.getAudioBufferSize());
        this.inputBufferSize = inputBufferSize;
        this.pipeline = new DspPipeline(inputType);
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
        int sampleRate = Configuration.getInputSampleRate();

        while (running.get()) {
            int readCount = inputBuffer.readNonBlocking(pipelineBuf, 0, inputBufferSize);
            if (readCount > 0) {
                int pipelineBufLength = pipeline.process(pipelineBuf, readCount);
                
                if (pipeline.isValid() && pipeline.getFinalOutputType() == NumberType.REAL) {
                    float[] audioBuf = new float[pipelineBufLength];
                    for (int i = 0; i < pipelineBufLength; i++) {
                        audioBuf[i] = (float) pipelineBuf[i];
                    }
                    audioOutputBuffer.writeNonBlocking(audioBuf, 0, pipelineBufLength);
                }

                long sleepTimeMs = (long) ((readCount / (double) sampleRate) * 1000);
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException e) {
                    running.set(false);
                    logger.error("DspThread interrupted during sleep", e);
                    break;
                }
            } else {
                // No data available, sleep a short time to avoid busy waiting
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    running.set(false);
                    logger.error("DspThread interrupted during sleep", e);
                    break;
                }
            }
        }
    }    public void stop() {
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

    public void clearPipeline() {
        pipeline.clearPipeline();
    }

    public void addPipelineStep(AbstractPipelineStep step) {
        pipeline.addPipelineStep(step);
    }

    public void buildPipeline() {
        pipeline.buildPipeline();
    }

    public List<AbstractPipelineStep> getPipelineSteps() {
        return pipeline.getPipelineSteps();
    }

    public DspPipeline getPipeline() {
        return pipeline;
    }
}