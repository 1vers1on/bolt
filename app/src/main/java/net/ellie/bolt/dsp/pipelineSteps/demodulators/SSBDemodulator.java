package net.ellie.bolt.dsp.pipelineSteps.demodulators;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

import org.apache.commons.math3.util.Pair;

public class SSBDemodulator extends AbstractPipelineStep {
    private PipelineAttribute<Boolean> isUSB;
    private PipelineAttribute<Double> sampleRate;
    private PipelineAttribute<Double> carrierFrequency;

    private double phase;
    private double phaseIncrement;
    
    public SSBDemodulator(PipelineAttribute<Boolean> isUSB, PipelineAttribute<Double> sampleRate, PipelineAttribute<Double> carrierFrequency, DspPipeline pipeline) {
        this.isUSB = isUSB;
        this.sampleRate = sampleRate;
        this.carrierFrequency = carrierFrequency;

        this.phaseIncrement = 2.0 * Math.PI * this.carrierFrequency.resolve(pipeline) / this.sampleRate.resolve(pipeline);;
        this.phase = 0.0;
        this.carrierFrequency.addListener(newValue -> {
            this.phaseIncrement = 2.0 * Math.PI * newValue / this.sampleRate.resolve(pipeline);
        });

        this.sampleRate.addListener(newValue -> {
            this.phaseIncrement = 2.0 * Math.PI * this.carrierFrequency.resolve(pipeline) / newValue;
        });
    }
    
    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
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
}