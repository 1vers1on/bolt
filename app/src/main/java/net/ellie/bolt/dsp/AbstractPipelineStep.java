package net.ellie.bolt.dsp;

import org.apache.commons.math3.util.Pair;

public abstract class AbstractPipelineStep {
    public abstract int process(double[] buffer, int length, DspPipeline pipeline);

    public abstract void reset();
    
    // Real to complex and Complex to real change the output length
    public abstract Pair<NumberType, NumberType> getInputOutputType();

    public abstract PipelineStepType getType();
}
