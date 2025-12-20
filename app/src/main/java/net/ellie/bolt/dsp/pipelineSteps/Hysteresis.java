package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class Hysteresis extends AbstractPipelineStep {
    private int state;
    private double upperThreshold;
    private double lowerThreshold;

    public Hysteresis(double upperThreshold, double lowerThreshold) {
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.state = 0;
    }

    @Override
    public int process(double[] buffer, int length) {
        for (int i = 0; i < length; i++) {
            double value = buffer[i];
            if (state == 0) {
                if (value >= upperThreshold) {
                    state = 1;
                }
            } else {
                if (value <= lowerThreshold) {
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

    public void setUpperThreshold(double value) {
        this.upperThreshold = value;
    }

    public void setLowerThreshold(double value) {
        this.lowerThreshold = value;
    }
}
