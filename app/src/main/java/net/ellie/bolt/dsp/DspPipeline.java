package net.ellie.bolt.dsp;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.ellie.bolt.dsp.pipelineSteps.WAVRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DspPipeline {
    private static final Logger logger = LoggerFactory.getLogger(DspPipeline.class);
    
    private final List<AbstractPipelineStep> pipelineSteps = new ArrayList<>();
    private final NumberType inputType;
    private boolean pipelineValid = false;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public DspPipeline(NumberType inputType) {
        this.inputType = inputType;
    }

    public int process(double[] buffer, int length) {
        lock.readLock().lock();
        try {
            if (!pipelineValid) {
                return 0;
            }
            
            int pipelineBufLength = length;
            for (AbstractPipelineStep step : pipelineSteps) {
                pipelineBufLength = step.process(buffer, pipelineBufLength);
            }
            return pipelineBufLength;
        } finally {
            lock.readLock().unlock();
        }
    }

    public IDemodulator getDemodulator() {
        lock.readLock().lock();
        try {
            for (AbstractPipelineStep step : pipelineSteps) {
                if (step instanceof IDemodulator) {
                    return (IDemodulator) step;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public WAVRecorder getWAVRecorder() {
        lock.readLock().lock();
        try {
            for (AbstractPipelineStep step : pipelineSteps) {
                if (step instanceof WAVRecorder) {
                    return (WAVRecorder) step;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clearPipeline() {
        lock.writeLock().lock();
        try {
            pipelineValid = false;
            pipelineSteps.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addPipelineStep(AbstractPipelineStep step) {
        lock.writeLock().lock();
        try {
            pipelineSteps.add(step);
            pipelineValid = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void buildPipeline() {
        lock.writeLock().lock();
        try {
            try {
                validatePipeline();
                pipelineValid = true;
            } catch (IllegalStateException e) {
                logger.error("Invalid DSP pipeline configuration", e);
                clearPipeline();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<AbstractPipelineStep> getPipelineSteps() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(pipelineSteps);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isValid() {
        lock.readLock().lock();
        try {
            return pipelineValid;
        } finally {
            lock.readLock().unlock();
        }
    }

    public NumberType getFinalOutputType() {
        lock.readLock().lock();
        try {
            if (pipelineSteps.isEmpty()) {
                return inputType;
            }
            var types = pipelineSteps.get(pipelineSteps.size() - 1).getInputOutputType();
            return types.getSecond();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void validatePipeline() {
        NumberType previousType = inputType;
        
        for (AbstractPipelineStep step : pipelineSteps) {
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
}