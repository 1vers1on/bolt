package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class DummySineStep extends AbstractPipelineStep {
    private double frequency;
    private double sampleRate;

    public DummySineStep(double frequency, double sampleRate) {
        this.frequency = frequency;
        this.sampleRate = sampleRate;
    }

    public int process(double[] buffer, int length) {
        for (int i = 0; i < length; i++) {
            buffer[i] = Math.sin(2.0 * Math.PI * frequency * i / sampleRate);
        }

        return length;
    }

    @Override
    public void reset() {
        // Reset any state if necessary
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.OTHER;
    }
}
