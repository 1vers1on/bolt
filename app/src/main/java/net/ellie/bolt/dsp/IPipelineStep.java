package net.ellie.bolt.dsp;

import org.apache.commons.math3.util.Pair;

public interface IPipelineStep {
    int process(double[] buffer, int length);

    void reset();
    
    // Real to complex and Complex to real change the output length
    Pair<NumberType, NumberType> getInputOutputType();

    PipelineStepType getType();
}
