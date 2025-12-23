
package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class FrequencyShifter extends AbstractPipelineStep {
    private PipelineAttribute<Double> sampleRate;
    private PipelineAttribute<Double> offsetHz;

    private double phase;
    private double phaseInc;
    private double cosPhase;
    private double sinPhase;
    private double cosInc;
    private double sinInc;

    public FrequencyShifter(PipelineAttribute<Double> sampleRate, PipelineAttribute<Double> offsetHz, DspPipeline pipeline) {
        this.sampleRate = sampleRate;
        this.offsetHz = offsetHz;
        this.phase = 0.0;
        updatePhaseInc(pipeline);
        this.offsetHz.addListener(newValue -> {
            updatePhaseInc(pipeline);
        });
        this.sampleRate.addListener(newValue -> {
            updatePhaseInc(pipeline);
        });
        this.cosPhase = 1.0;
        this.sinPhase = 0.0;
    }

    private void updatePhaseInc(DspPipeline pipeline) {
        this.phaseInc = 2.0 * Math.PI * this.offsetHz.resolve(pipeline) / this.sampleRate.resolve(pipeline);
        this.cosInc = Math.cos(phaseInc);
        this.sinInc = Math.sin(phaseInc);
        this.cosPhase = Math.cos(phase);
        this.sinPhase = Math.sin(phase);
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        int safeLength = (length & ~1);
        int renormEvery = 16;
        int count = 0;

        for (int i = 0; i < safeLength; i += 2) {
            double real = buffer[i];
            double imag = buffer[i + 1];

            double shiftedReal = real * cosPhase - imag * sinPhase;
            double shiftedImag = real * sinPhase + imag * cosPhase;

            buffer[i] = shiftedReal;
            buffer[i + 1] = shiftedImag;

            double nextCos = cosPhase * cosInc - sinPhase * sinInc;
            double nextSin = cosPhase * sinInc + sinPhase * cosInc;
            cosPhase = nextCos;
            sinPhase = nextSin;

            if (++count == renormEvery) {
                double mag = Math.hypot(cosPhase, sinPhase);
                if (mag != 0.0) {
                    cosPhase /= mag;
                    sinPhase /= mag;
                }
                count = 0;
            }
        }

        phase += phaseInc * (safeLength / 2);
        if (phase > Math.PI) {
            phase -= 2.0 * Math.PI;
        } else if (phase < -Math.PI) {
            phase += 2.0 * Math.PI;
        }

        return safeLength;
    }

    @Override
    public void reset() {
        phase = 0.0;
        cosPhase = 1.0;
        sinPhase = 0.0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.OTHER;
    }
}
