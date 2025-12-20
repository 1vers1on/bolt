package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class Goertzel extends AbstractPipelineStep {
    private double targetFrequency;
    private double sampleRate;
    private int blockSize;

    public Goertzel(double targetFrequency, double sampleRate, int blockSize) {
        this.targetFrequency = targetFrequency;
        this.sampleRate = sampleRate;
        this.blockSize = blockSize;
    }

    @Override
    public int process(double[] buffer, int length) {
        int blocks = length / blockSize;

        double normalizedFreq = targetFrequency / sampleRate;
        double k = normalizedFreq * blockSize;
        double omega = (2.0 * Math.PI * k) / blockSize;
        double coeff = 2.0 * Math.cos(omega);
        
        for (int block = 0; block < blocks; block++) {
            int offset = block * blockSize;
            
            double s0 = 0.0;
            double s1 = 0.0;
            double s2 = 0.0;
            
            for (int i = 0; i < blockSize; i++) {
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

    public void setTargetFrequency(double targetFrequency) {
        this.targetFrequency = targetFrequency;
    }
}
