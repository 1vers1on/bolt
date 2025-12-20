package net.ellie.bolt.dsp.pipelineSteps.demodulators;

import net.ellie.bolt.dsp.IDemodulator;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

import org.apache.commons.math3.util.Pair;

public class SSBDemodulator extends AbstractPipelineStep implements IDemodulator {
    private final boolean isUSB;
    private final double sampleRate;
    private double carrierFrequency;
    
    private double phase;
    private double phaseIncrement;
    
    public SSBDemodulator(boolean isUSB, double sampleRate, double carrierFrequency) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }
        this.isUSB = isUSB;
        this.sampleRate = sampleRate;
        this.carrierFrequency = carrierFrequency;
        
        this.phaseIncrement = 2.0 * Math.PI * this.carrierFrequency / this.sampleRate;
        this.phase = 0.0;
    }

    @Override
    public void setFrequencyOffsetHz(double offsetHz) {
        this.carrierFrequency = offsetHz;
        this.phaseIncrement = 2.0 * Math.PI * this.carrierFrequency / this.sampleRate;
    }
    
    @Override
    public int process(double[] buffer, int length) {
        int outIndex = 0;
        
        for (int i = 0; i + 1 < length; i += 2) {
            double I = buffer[i];
            double Q = buffer[i + 1];

            double cosPhase = Math.cos(this.phase);
            double sinPhase = Math.sin(this.phase);

            buffer[outIndex++] = I * cosPhase + Q * sinPhase;
            
            this.phase += this.phaseIncrement;
            
            if (this.phase >= 2.0 * Math.PI) {
                this.phase -= 2.0 * Math.PI;
            }
        }
        return outIndex;
    }
    
    @Override
    public void reset() {
        this.phase = 0.0;
    }
    
    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.REAL);
    }
    
    @Override
    public PipelineStepType getType() {
        return PipelineStepType.DEMODULATOR;
    }
    
    public boolean isUSB() {
        return isUSB;
    }
}