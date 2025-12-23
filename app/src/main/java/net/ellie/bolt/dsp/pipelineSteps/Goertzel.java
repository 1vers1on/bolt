package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class Goertzel extends AbstractPipelineStep {
    private final PipelineAttribute<Double> targetFrequency;
    private final PipelineAttribute<Double> sampleRate;
    private final PipelineAttribute<Integer> blockSize;

    public Goertzel(PipelineAttribute<Double> targetFrequency, PipelineAttribute<Double> sampleRate, PipelineAttribute<Integer> blockSize) {
        this.targetFrequency = targetFrequency;
        this.sampleRate = sampleRate;
        this.blockSize = blockSize;
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        double freq = targetFrequency.resolve(pipeline);
        double rate = sampleRate.resolve(pipeline);
        int blkSize = blockSize.resolve(pipeline);
        int blocks = length / blkSize;

        double normalizedFreq = freq / rate;
        double k = normalizedFreq * blkSize;
        double omega = (2.0 * Math.PI * k) / blkSize;
        double coeff = 2.0 * Math.cos(omega);

        for (int block = 0; block < blocks; block++) {
            int offset = block * blkSize;

            double s0 = 0.0;
            double s1 = 0.0;
            double s2 = 0.0;

            for (int i = 0; i < blkSize; i++) {
                s0 = buffer[offset + i] + coeff * s1 - s2;
                s2 = s1;
                s1 = s0;
            }

            double magnitudeSquared = s1 * s1 + s2 * s2 - s1 * s2 * coeff;

            buffer[block] = Math.sqrt(magnitudeSquared);
        }

        return blocks;
    }

    @Override
    public void reset() {
        // No internal state to reset
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
