package net.ellie.bolt.dsp;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

        while (running.get()) {
            try {
                inputBuffer.read(pipelineBuf, 0, inputBufferSize);
            } catch (InterruptedException e) {
                running.set(false);
                logger.error("DspThread interrupted during read", e);
                break;
            }

            // Process through pipeline
            int pipelineBufLength = pipeline.process(pipelineBuf, pipelineBuf.length);
            
            // Output to audio buffer if the final output is real
            if (pipeline.isValid() && pipeline.getFinalOutputType() == NumberType.REAL) {
                float[] audioBuf = new float[pipelineBufLength];
                for (int i = 0; i < pipelineBufLength; i++) {
                    audioBuf[i] = (float) pipelineBuf[i];
                }
                audioOutputBuffer.writeNonBlocking(audioBuf, 0, pipelineBufLength);
            }
        }
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

    // Delegate pipeline methods to the DspPipeline class
    public IDemodulator getDemodulator() {
        return pipeline.getDemodulator();
    }
    
    public WAVRecorder getWAVRecorder() {
        return pipeline.getWAVRecorder();
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
}