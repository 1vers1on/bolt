package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class DummySineStep extends AbstractPipelineStep {
    private final PipelineAttribute<Double> frequency;
    private final PipelineAttribute<Double> sampleRate;

    public DummySineStep(PipelineAttribute<Double> frequency, PipelineAttribute<Double> sampleRate) {
        this.frequency = frequency;
        this.sampleRate = sampleRate;
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        double freq = frequency.resolve(pipeline);
        double rate = sampleRate.resolve(pipeline);
        for (int i = 0; i < length; i++) {
            buffer[i] = Math.sin(2.0 * Math.PI * freq * i / rate);
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
