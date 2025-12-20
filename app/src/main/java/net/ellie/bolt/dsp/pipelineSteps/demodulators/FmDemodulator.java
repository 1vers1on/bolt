package net.ellie.bolt.dsp.pipelineSteps.demodulators;

import net.ellie.bolt.dsp.IDemodulator;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

import org.apache.commons.math3.util.Pair;

public class FmDemodulator extends AbstractPipelineStep implements IDemodulator {

    private final int sampleRate;
    private double frequencyOffsetHz = 0.0;
    private double phase = 0.0;
    private double twoPi = 2.0 * Math.PI;
    private double prevRe = 0.0;
    private double prevIm = 0.0;
    private boolean hasPrev = false;

    public FmDemodulator(int sampleRate, double frequencyOffsetHz) {
        this.sampleRate = sampleRate;
        this.frequencyOffsetHz = frequencyOffsetHz;
        this.phase = 0.0;
    }

    @Override
    public int process(double[] buffer, int length) {
        int outIndex = 0;

        for (int i = 0; i + 1 < length; i += 2) {
            double re = buffer[i];
            double im = buffer[i + 1];

            double phaseInc = twoPi * frequencyOffsetHz / sampleRate;
            phase += phaseInc;
            if (phase > Math.PI) {
                phase -= twoPi;
            } else if (phase < -Math.PI) {
                phase += twoPi;
            }
            double cosP = Math.cos(phase);
            double sinP = Math.sin(phase);
            double rotRe = re * cosP - im * sinP;
            double rotIm = re * sinP + im * cosP;

            double demod;
            if (hasPrev) {
                demod = (prevRe * rotIm - prevIm * rotRe);
                double mag2 = rotRe * rotRe + rotIm * rotIm;
                if (mag2 > 0.0) {
                    demod /= mag2;
                }
            } else {
                demod = 0.0;
                hasPrev = true;
            }

            prevRe = rotRe;
            prevIm = rotIm;

            buffer[outIndex++] = demod;
        }

        return outIndex;
    }

    @Override
    public void reset() {
        hasPrev = false;
        prevRe = 0.0;
        prevIm = 0.0;
        phase = 0.0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.DEMODULATOR;
    }

    @Override
    public void setFrequencyOffsetHz(double offsetHz) {
        this.frequencyOffsetHz = offsetHz;
    }
}
