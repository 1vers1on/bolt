package net.ellie.bolt.dsp.pipelineSteps.demodulators;

import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

import org.apache.commons.math3.util.Pair;

public class FmDemodulator implements IPipelineStep {

    private double prevRe = 0.0;
    private double prevIm = 0.0;
    private boolean hasPrev = false;

    @Override
    public int process(double[] buffer, int length) {
        int outIndex = 0;

        for (int i = 0; i + 1 < length; i += 2) {
            double re = buffer[i];
            double im = buffer[i + 1];

            double demod;
            if (hasPrev) {
                demod = (prevRe * im - prevIm * re);
                double mag2 = re * re + im * im;
                if (mag2 > 0.0) {
                    demod /= mag2;
                }
            } else {
                demod = 0.0;
                hasPrev = true;
            }

            prevRe = re;
            prevIm = im;

            buffer[outIndex++] = demod;
        }

        return outIndex;
    }

    @Override
    public void reset() {
        hasPrev = false;
        prevRe = 0.0;
        prevIm = 0.0;
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
