package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class FrequencyShifter extends AbstractPipelineStep {
    private final double sampleRate;
    private double phase = 0.0;
    private double phaseInc = 0.0;

    private double cosPhase = 1.0;
    private double sinPhase = 0.0;
    private double cosInc = 1.0;
    private double sinInc = 0.0;

    public FrequencyShifter(double sampleRate) {
        this.sampleRate = sampleRate;
        setOffsetHz(0.0);
    }

    public void setOffsetHz(double offsetHz) {
        this.phaseInc = 2.0 * Math.PI * offsetHz / sampleRate;
        cosInc = Math.cos(phaseInc);
        sinInc = Math.sin(phaseInc);
        cosPhase = Math.cos(phase);
        sinPhase = Math.sin(phase);
    }

    @Override
    public int process(double[] buffer, int length) {
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
