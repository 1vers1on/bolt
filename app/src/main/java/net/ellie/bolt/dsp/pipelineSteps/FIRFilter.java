package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class FIRFilter implements IPipelineStep {
    private double[] taps;
    private double[] delay;
    private int delayIdx;

    public FIRFilter(double[] taps) {
        if (taps == null || taps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.taps = taps.clone();
        this.delay = new double[taps.length * 2];
        this.delayIdx = 0;
    }

    public FIRFilter() {
        this(new double[] { 1.0 });
    }

    @Override
    public int process(double[] buffer, int length) {
        if (buffer == null) return 0;
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Complex buffer length must be even (interleaved re, im)");
        }
        int complexSamples = length / 2;
        int L = taps.length;

        for (int n = 0; n < complexSamples; n++) {
            int writePos = delayIdx * 2;
            delay[writePos] = buffer[n * 2];
            delay[writePos + 1] = buffer[n * 2 + 1];

            double accRe = 0.0;
            double accIm = 0.0;

            int di = delayIdx;
            for (int k = 0; k < L; k++) {
                int pos = di * 2;
                double tk = taps[k];
                accRe += delay[pos] * tk;
                accIm += delay[pos + 1] * tk;

                di--;
                if (di < 0) di = L - 1;
            }

            buffer[n * 2] = accRe;
            buffer[n * 2 + 1] = accIm;

            delayIdx++;
            if (delayIdx >= L) delayIdx = 0;
        }

        return length;
    }

    @Override
    public void reset() {
        for (int i = 0; i < delay.length; i++) delay[i] = 0.0;
        delayIdx = 0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }

    public void setTaps(double[] taps) {
        if (taps == null || taps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.taps = taps.clone();
        this.delay = new double[taps.length * 2];
        this.delayIdx = 0;
    }
}