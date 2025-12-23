package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class Hysteresis extends AbstractPipelineStep {
    private int state;
    private final PipelineAttribute<Double> upperThreshold;
    private final PipelineAttribute<Double> lowerThreshold;

    public Hysteresis(PipelineAttribute<Double> upperThreshold, PipelineAttribute<Double> lowerThreshold) {
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.state = 0;
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        double upper = upperThreshold.resolve(pipeline);
        double lower = lowerThreshold.resolve(pipeline);
        for (int i = 0; i < length; i++) {
            double value = buffer[i];
            if (state == 0) {
                if (value >= upper) {
                    state = 1;
                }
            } else {
                if (value <= lower) {
                    state = 0;
                }
            }
            buffer[i] = state;
        }
        return length;
    }

    @Override
    public void reset() {
        state = 0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }
}
